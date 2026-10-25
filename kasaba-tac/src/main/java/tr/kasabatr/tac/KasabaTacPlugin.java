package tr.kasabatr.tac;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.boss.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.auth.api.*;
import tr.kasaba.hub.api.*;

/** Hub-managed crown chase, with explicit queue admission and isolated matches. */
public final class KasabaTacPlugin extends JavaPlugin implements HubGame,Listener,TabExecutor {
    static final String ID="lanetlitac";
    record Session(UUID party,List<UUID> members){}
    final Set<UUID> visitors=new LinkedHashSet<>(),queue=new LinkedHashSet<>();
    final Map<UUID,Session> sessions=new HashMap<>();
    final Map<UUID,CrownRoom> membership=new HashMap<>();
    final Map<Integer,CrownRoom> rooms=new LinkedHashMap<>();
    final Random random=new Random();
    CrownMap map;
    private final Set<UUID> internalTeleport=new HashSet<>(),quitting=new HashSet<>(),editors=new HashSet<>();
    private BossBar queueBar;
    private final Map<UUID,Long> messageAt=new HashMap<>();
    World lobbyWorld;
    WorldInstances instances;
    HubApi hub;
    GameScreens screens;
    NamespacedKey toolKey;
    boolean stopping;
    int nextRoom=101;
    long queueDeadline;

    @Override public void onEnable(){
        queueBar=Bukkit.createBossBar("§dLanetli Taç",BarColor.PURPLE,BarStyle.SOLID);saveDefaultConfig();screens=new GameScreens(this);toolKey=new NamespacedKey(this,"tac-tool");
        try{
            String arenaName=getConfig().getString("world",MapInstaller.DEFAULTS.getFirst());
            MapInstaller.ensure(this,arenaName);map=new CrownMap(arenaName,Bukkit.getWorldContainer().toPath().resolve(arenaName).resolve("crown-map.json"));instances=new WorldInstances(this);instances.clearStaleCopies();
            lobbyWorld=LobbyWorld.open();configure(lobbyWorld);World template=load(arenaName);validateWorld(map,template);
            new MultiverseBridge(this).register(lobbyWorld,lobbyPoint());new MultiverseBridge(this).register(template,map.lobby.at(template));
            hub=Bukkit.getServicesManager().load(HubApi.class);if(hub==null)throw new IOException("KasabaHub bulunamadı.");hub.register(this,this);presentLobby();
        }catch(Exception e){getLogger().log(Level.SEVERE,"Lanetli Taç açılamadı; mevcut dünya/veri dosyaları korundu.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        Bukkit.getPluginManager().registerEvents(this,this);Objects.requireNonNull(getCommand("tac")).setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this,this::tick,5,5);for(Player p:Bukkit.getOnlinePlayers())recover(p);
        getLogger().info("Lanetli Taç hazır: ayrı lobi, üç eşzamanlı oda, taç takibi ve dönen kapılar, /oyunlar, /tac; normal maç 6–16, yönetici denemesi 2 kişi.");
    }
    @Override public void onDisable(){
        stopping=true;if(queueBar!=null)queueBar.removeAll();
        if(hub!=null)try{hub.unregister(this,ID);}catch(RuntimeException e){getLogger().warning(e.getMessage());}
        for(UUID id:List.copyOf(visitors)){Player p=Bukkit.getPlayer(id);if(p!=null)detach(p,true);else leave(id);}
        for(CrownRoom room:List.copyOf(rooms.values()))room.close();
    }
    private void configure(World world){world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.KEEP_INVENTORY,true);world.setGameRule(GameRule.LOCATOR_BAR,false);world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);world.setTime(1000);world.setStorm(false);world.setThundering(false);}
    private World load(String name)throws IOException{World w=Bukkit.getWorld(name);if(w==null)w=Bukkit.createWorld(new WorldCreator(name).generator(new EmptyWorldGenerator()).generateStructures(false));if(w==null)throw new IOException("Dünya açılamadı: "+name);configure(w);return w;}
    @Override public ChunkGenerator getDefaultWorldGenerator(String name,String id){return new EmptyWorldGenerator();}
    @Override public GameInfo info(){return new GameInfo(ID,"Lanetli Taç","Tacı kap ve taşı • şato kovalamacası • 6–16 kişi",1,maximumPlayers());}
    @Override public boolean available(){return !stopping&&hub!=null&&lobbyWorld!=null&&Bukkit.getWorld(lobbyWorld.getUID())==lobbyWorld&&map!=null&&Bukkit.getWorld(map.worldName)!=null&&instances.templateReady(map);}
    @Override public boolean lobby(UUID id){return visitors.contains(id)&&!membership.containsKey(id);}
    @Override public boolean penaltyEligible(UUID id){CrownRoom room=membership.get(id);Player p=Bukkit.getPlayer(id);return !stopping&&room!=null&&room.active()&&!room.practice&&p!=null&&!p.isDead();}
    @Override public boolean containsMatch(UUID id,Location at){CrownRoom room=membership.get(id);return room==null||room.contains(at);}
    boolean auth(Player p){AuthState state=Bukkit.getServicesManager().load(AuthState.class);return p.isOnline()&&state!=null&&state.authenticated(p.getUniqueId());}
    void ready(Player p){if(!auth(p)||!visitors.contains(p.getUniqueId()))throw new IllegalArgumentException("Önce /oyunlar üzerinden Lanetli Taç lobisine gir.");GameAccessGuard guard=Bukkit.getServicesManager().load(GameAccessGuard.class);if(guard!=null&&guard.blocked(p.getUniqueId()))throw new IllegalArgumentException(guard.message(p.getUniqueId()));}
    void tell(CommandSender p,String text){p.sendMessage("§d[Lanetli Taç] §f"+text);}
    void abandon(UUID id){if(hub!=null)hub.abandon(id);}
    @Override public boolean join(List<Player> team){
        if(!available()||team==null||team.isEmpty()||team.size()>maximumPlayers())return false;
        for(Player p:team)if(p==null||!auth(p)||!p.hasPermission("kasaba.tac.play")||visitors.contains(p.getUniqueId())||PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return false;
        List<Player> prepared=new ArrayList<>();Map<Player,Location> before=new LinkedHashMap<>();UUID party=UUID.randomUUID();List<UUID> ids=team.stream().map(Player::getUniqueId).toList();
        try{
            for(Player p:team){p.closeInventory();PlayerBackup.save(getDataFolder(),p);before.put(p,p.getLocation());prepared.add(p);visitors.add(p.getUniqueId());sessions.put(p.getUniqueId(),new Session(party,ids));prepareLobby(p);}
            for(Player p:team){tell(p,"Lobiye geldin. Pusuladan Oyna seç; burada bekleyebilir veya başka oyun lobisine geçebilirsin.");Bukkit.getScheduler().runTaskLater(this,()->{if(auth(p)&&lobby(p.getUniqueId()))menu(p);},2);}
            return true;
        }catch(Exception e){getLogger().log(Level.WARNING,"Taç grup girişi geri alınıyor.",e);for(Player p:prepared){detach(p,false);try{teleport(p,before.get(p));}catch(RuntimeException ignored){}}return false;}
    }
    @Override public void leave(UUID id){abandon(id);Player p=Bukkit.getPlayer(id);if(p!=null)detach(p,stopping);else{visitors.remove(id);sessions.remove(id);queue.remove(id);CrownRoom room=membership.remove(id);if(room!=null)room.remove(id);}}
    private void detach(Player p,boolean restoreLocation){
        UUID id=p.getUniqueId();abandon(id);boolean owned=visitors.remove(id);sessions.remove(id);queue.remove(id);queueBar.removePlayer(p);editors.remove(id);screens.clear(id);p.closeDialog();
        CrownRoom room=membership.remove(id);if(room!=null)room.remove(id);reveal(p);if(!owned)return;
        if(!quitting.contains(id))try{internalTeleport.add(id);PlayerBackup.restore(getDataFolder(),p,restoreLocation&&!hub.transferring(id));}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Taç yedeği korundu: "+p.getName(),e);p.kick(net.kyori.adventure.text.Component.text("Lanetli Taç envanter yedeğin korunuyor. Yöneticiye haber ver."));}finally{internalTeleport.remove(id);}
        if(room!=null&&p.getWorld()==room.world&&!stopping)try{teleport(p,lobbyPoint());}catch(RuntimeException e){getLogger().warning("Geçici dünyadan çıkarma yeniden denenecek: "+p.getName());}
    }
    void prepare(Player p,Location at){
        if(NetworkFeatures.get()!=null)NetworkFeatures.get().beforeGame(p);screens.clear(p.getUniqueId());p.closeDialog();p.closeInventory();p.setItemOnCursor(null);p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);
        for(var effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);p.setGravity(true);p.setInvulnerable(false);p.setAbsorptionAmount(0);p.setCollidable(false);p.setFireTicks(0);p.setFallDistance(0);p.setNoDamageTicks(0);p.setFoodLevel(20);p.setSaturation(20);p.setExhaustion(0);if(!p.isDead())p.setHealth(Math.min(20,p.getMaxHealth()));p.setTotalExperience(0);p.setLevel(0);p.setExp(0);teleport(p,at);
    }
    private void prepareLobby(Player p){prepare(p,lobbyPoint());tools(p);}
    Location lobbyPoint(){return LobbyWorld.spawn(lobbyWorld);}
    void teleport(Player p,Location to){internalTeleport.add(p.getUniqueId());try{if(p.isInsideVehicle())p.leaveVehicle();if(!p.teleport(to,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Işınlanma engellendi.");p.setFallDistance(0);}finally{internalTeleport.remove(p.getUniqueId());}}
    void reveal(Player p){for(Player viewer:Bukkit.getOnlinePlayers())if(viewer!=p)viewer.showPlayer(this,p);}
    private boolean enter(Player p){if(!auth(p))throw new IllegalArgumentException("Önce hesabına giriş yap.");if(visitors.contains(p.getUniqueId()))return true;if(hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Mevcut oyunun lobisindeki pusuladan geçiş yap veya /lobi kullan.");Bukkit.dispatchCommand(p,"oyunlar git "+ID);return visitors.contains(p.getUniqueId());}
    private List<Player> party(Player p){ready(p);Session s=sessions.get(p.getUniqueId());List<Player> members=s.members.stream().filter(visitors::contains).map(Bukkit::getPlayer).filter(Objects::nonNull).toList();if(members.isEmpty()||members.getFirst()!=p)throw new IllegalArgumentException("Oynama seçimini grup lideri yapmalı.");for(Player member:members){ready(member);hub.requireGameEntry(member.getUniqueId(),ID);if(membership.containsKey(member.getUniqueId()))throw new IllegalArgumentException("Grubundaki herkes oyun lobisinde olmalı.");}return members;}
    private void play(Player p){for(Player member:party(p)){queue.add(member.getUniqueId());queueBar.addPlayer(member);member.closeDialog();screens.clear(member.getUniqueId());tell(member,"Sıradasın: "+queue.size()+"/"+minimumPlayers()+" • Başlama sayımı "+countdownSeconds()+" saniye.");}}
    private void cancelQueue(Player p){for(Player member:party(p)){queue.remove(member.getUniqueId());queueBar.removePlayer(member);screens.clear(member.getUniqueId());member.closeDialog();tell(member,"Sıradan çıktın; bu lobide kalabilirsin.");}if(queue.size()<minimumPlayers())queueDeadline=0;}
    private List<UUID> selectBatch(){Map<UUID,List<UUID>> groups=new LinkedHashMap<>();for(UUID id:queue){Session s=sessions.get(id);if(s!=null)groups.computeIfAbsent(s.party,k->new ArrayList<>()).add(id);}return CrownRules.batch(new ArrayList<>(groups.values()),maximumPlayers());}
    private int maximumPlayers(){return Math.clamp(getConfig().getInt("max-players",16),6,16);}
    private int minimumPlayers(){return Math.clamp(getConfig().getInt("min-players",6),6,maximumPlayers());}
    private int countdownSeconds(){return Math.clamp(getConfig().getInt("auto-start-seconds",20),5,120);}
    private int maximumRooms(){return Math.clamp(getConfig().getInt("max-concurrent-rooms",3),1,8);}
    private void start(boolean practice){
        List<UUID> selected=selectBatch();if(selected.size()<(practice?2:minimumPlayers())||practice&&(queue.size()!=2||selected.size()!=2))throw new IllegalArgumentException(practice?"Deneme için sırada tam 2 oyuncu olmalı.":"Normal maç için en az "+minimumPlayers()+" kişi sırada olmalı.");if(rooms.size()>=maximumRooms())throw new IllegalArgumentException("Odalar dolu; bir maçın bitmesini bekle.");
        for(UUID id:selected){Player p=Bukkit.getPlayer(id);if(p==null)throw new IllegalArgumentException("Oyuncu ayrıldı.");ready(p);hub.requireGameEntry(id,ID);}
        CrownRoom room=new CrownRoom(this,nextRoom++,map,practice);queueDeadline=0;
        try{room.world=instances.create(room.id,map);rooms.put(room.id,room);room.players.addAll(selected);for(UUID id:selected){queue.remove(id);membership.put(id,room);Player p=Bukkit.getPlayer(id);queueBar.removePlayer(p);p.closeDialog();screens.clear(id);}room.start();}
        catch(Exception e){getLogger().log(Level.SEVERE,"Taç odası güvenli biçimde kapatılıyor.",e);room.close();for(UUID id:selected){queue.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null){queueBar.removePlayer(p);tell(p,"Maç açılamadı; lobiye döndün. /tac ile yeniden deneyebilirsin.");}}}
    }
    private void tick(){
        if(!available())return;long now=System.currentTimeMillis();queue.removeIf(id->!visitors.contains(id)||Bukkit.getPlayer(id)==null||membership.containsKey(id));
        for(UUID id:List.copyOf(visitors)){Player p=Bukkit.getPlayer(id);if(p!=null&&lobby(id)&&!insideLobby(p.getLocation()))returnGameLobby(p,0);}
        if(selectBatch().size()>=minimumPlayers()&&rooms.size()<maximumRooms()){
            int duration=countdownSeconds();if(queueDeadline==0)queueDeadline=now+duration*1000L;long left=Math.max(0,(queueDeadline-now+999)/1000);
            queueBar.setColor(left<=5?BarColor.RED:BarColor.PURPLE);queueBar.setTitle("§dLanetli Taç §8• §eBaşlamasına "+left+" sn §8• §b"+queue.size()+" oyuncu");queueBar.setProgress(Math.clamp((queueDeadline-now)/(duration*1000.0),0,1));
            if(now>=queueDeadline)try{start(false);}catch(RuntimeException e){queueDeadline=0;getLogger().log(Level.WARNING,"Taç başlangıcı ertelendi.",e);}
        }else{queueDeadline=0;queueBar.setColor(BarColor.BLUE);queueBar.setTitle("§dLanetli Taç §8• §b"+queue.size()+"/"+minimumPlayers()+" §7• "+(rooms.size()>=maximumRooms()?"Odalar dolu":"Oyuncu bekleniyor"));queueBar.setProgress(Math.clamp(queue.size()/(double)minimumPlayers(),0,1));}
        for(CrownRoom room:List.copyOf(rooms.values()))try{room.tick(now);}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Taç odası kapatılıyor: "+room.id,e);room.close();}
    }
    void toLobby(Player p){ready(p);abandon(p.getUniqueId());queue.remove(p.getUniqueId());queueBar.removePlayer(p);CrownRoom room=membership.remove(p.getUniqueId());if(room!=null)room.remove(p.getUniqueId());returnGameLobby(p,0);}
    void returnGameLobby(Player p,int attempt){
        if(!p.isOnline()||!visitors.contains(p.getUniqueId())||membership.containsKey(p.getUniqueId()))return;
        try{if(p.isDead())p.spigot().respawn();prepareLobby(p);}catch(RuntimeException e){if(!stopping&&attempt<3)Bukkit.getScheduler().runTaskLater(this,()->returnGameLobby(p,attempt+1),10);else if(hub!=null)hub.returnToLobbyForced(p.getUniqueId());}
    }
    void closeWhenEmpty(World world,int attempt){if(world.getPlayers().isEmpty()){instances.close(world);return;}if(!stopping&&attempt<20)Bukkit.getScheduler().runTaskLater(this,()->closeWhenEmpty(world,attempt+1),10);else getLogger().warning("İçinde oyuncu olan geçici dünya korundu: "+world.getName());}
    private ItemStack tool(Material material,String text,String action){ItemStack item=new ItemStack(material);item.editMeta(meta->{meta.setDisplayName(text);meta.getPersistentDataContainer().set(toolKey,PersistentDataType.STRING,action);});return item;}
    private String action(ItemStack item){return item!=null&&item.hasItemMeta()?item.getItemMeta().getPersistentDataContainer().getOrDefault(toolKey,PersistentDataType.STRING,""):"";}
    void tools(Player p){p.getInventory().setItem(8,tool(Material.COMPASS,"§6§lLanetli Taç §7• Menü","menu"));}
    private void presentLobby(){hub.presentLobby(this,ID,lobbyPoint(),"LANETLİ TAÇ",List.of("§ePusuladan Oyna seç §8• §b6–16 kişi","§6Tacı kap ve taşı §8• §dŞato kapıları sürekli değişir"),"GALİBİYET");}
    void menu(Player p){ready(p);CrownRoom room=membership.get(p.getUniqueId());List<String> lines=new ArrayList<>();List<GameScreens.Button> buttons=new ArrayList<>();
        if(room==null){lines.add("§6Tacı kap; taşıdığın her saniye bir puan kazan.");lines.add("§7Oyna seç veya bu lobide kal; otomatik sıraya girmezsin.");lines.add("§bSırada "+queue.size()+"/"+minimumPlayers()+" §8• §dAktif oda "+rooms.size()+"/"+maximumRooms());buttons.add(new GameScreens.Button(queue.contains(p.getUniqueId())?"Sıradan çık":"Oyna",()->{if(queue.contains(p.getUniqueId()))cancelQueue(p);else play(p);}));buttons.add(new GameScreens.Button("Odaların durumu",()->roomInfo(p)));buttons.add(new GameScreens.Button("Oyun lobileri",()->hub.openGames(p.getUniqueId())));}
        else{lines.add("§6Oda "+room.id+" §8• §ePuanın: "+room.scores.getOrDefault(p.getUniqueId(),0));lines.add("§7Taç: §e"+room.name(room.crown)+" §8• §bKalan "+room.remaining()+" sn");buttons.add(new GameScreens.Button("Taç lobisine dön",()->toLobby(p)));}
        buttons.add(new GameScreens.Button("Nasıl oynanır?",()->help(p)));buttons.add(new GameScreens.Button(room==null?"Bu lobide kal":"Oyuna dön",()->{if(room==null&&queue.contains(p.getUniqueId()))cancelQueue(p);}));buttons.add(new GameScreens.Button("Ana spawna dön",()->hub.returnToLobby(p.getUniqueId())));screens.show(p,"Lanetli Taç",lines,buttons,room==null);}
    private void roomInfo(Player p){List<String> lines=new ArrayList<>();for(CrownRoom room:rooms.values())lines.add("§bOda "+room.id+" §8• §f"+room.players.size()+" kişi §8• §7"+(room.practice?"Deneme":room.active()?"Oynanıyor":"Sonuçlar"));if(lines.isEmpty())lines.add("§7Henüz başlayan oda yok.");screens.show(p,"Lanetli Taç • Odalar",lines,List.of(new GameScreens.Button("Geri",()->menu(p))),true);}
    private void help(Player p){screens.show(p,"Lanetli Taç • Nasıl oynanır?",List.of("§eNormal maç "+minimumPlayers()+"–"+maximumPlayers()+" kişi; "+countdownSeconds()+" saniye sayım.","§7Tacı taşıyan her saniye bir puan toplar; yakınlaşıp tacı kap.","§6Altın miğfer ve parıltı taç sahibini gösterir.","§bTaç devrinden sonra kısa koruma; anında geri kapma yok.","§dKuzey / doğu kapıları sırayla açılıp kapanır.","§aKapılar oyuncunun üstüne kapanmaz; yan yollar daima açıktır.","§e4 dakika sonunda en yüksek puan kazanır; eşitlikte ortak galibiyet.","§aSonuçtan 5 saniye sonra bu oyun lobisine dönersin.","§cNormal maçı yarım bırakmak bu oyuna 15 dakika giriş cezası verir.","§b/tac • /tac oyna • /tac lobi • /lobi"),List.of(new GameScreens.Button("Geri",()->menu(p))),false);}
    private String normalize(String value){return value.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ö','o').replace('ü','u');}
    private void admin(CommandSender p){if(!p.hasPermission("kasaba.tac.admin"))throw new IllegalArgumentException("Bu komut yöneticiye aittir.");}
    @Override public boolean onCommand(CommandSender sender,Command command,String alias,String[] args){
        try{String sub=args.length==0?"menu":normalize(args[0]);
            if(sub.equals("testharita")){admin(sender);if(!rooms.isEmpty())throw new IllegalArgumentException("Harita testi için aktif odaların bitmesini bekle.");testMaps(sender);return true;}
            if(!(sender instanceof Player p))throw new IllegalArgumentException("Bu komutu oyunda kullan.");
            if(sub.equals("duzenleme")){admin(p);if(visitors.contains(p.getUniqueId())||hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile oyundan çık.");if(args.length!=2)throw new IllegalArgumentException("/tac düzenleme aç|kapat");if(normalize(args[1]).equals("ac"))editors.add(p.getUniqueId());else if(normalize(args[1]).equals("kapat"))editors.remove(p.getUniqueId());else throw new IllegalArgumentException("/tac düzenleme aç|kapat");tell(p,"Harita düzenleme: "+(editors.contains(p.getUniqueId())?"açık":"kapalı"));return true;}
            if(!enter(p))return true;
            switch(sub){case "menu","katil"->menu(p);case "oyna"->play(p);case "durum"->{tell(p,"Sırada: "+queue.size()+"/"+minimumPlayers()+" • Aktif oda: "+rooms.size()+"/"+maximumRooms());roomInfo(p);}case "lobi"->toLobby(p);case "cik"->hub.returnToLobby(p.getUniqueId());case "baslat","deneme"->{admin(p);if(!lobby(p.getUniqueId())||!queue.contains(p.getUniqueId()))throw new IllegalArgumentException("Önce /tac oyna ile sıraya gir.");start(sub.equals("deneme"));}case "yardim"->help(p);default->throw new IllegalArgumentException("/tac • oyna • durum • lobi • çık • yardım");}
        }catch(Exception e){tell(sender,e.getMessage()==null?"İşlem tamamlanamadı.":e.getMessage());}return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return args.length==1?List.of("oyna","durum","lobi","çık","yardım","başlat","deneme","testharita","düzenleme").stream().filter(s->normalize(s).startsWith(normalize(args[0]))).toList():args.length==2&&normalize(args[0]).equals("duzenleme")?List.of("aç","kapat"):List.of();}
    private boolean insideLobby(Location at){return LobbyWorld.contains(at,lobbyWorld);}
    private boolean managed(World world){return world==lobbyWorld||world.getName().startsWith("tac_room_")||map!=null&&map.worldName.equals(world.getName());}
    private boolean editing(Player p){return editors.contains(p.getUniqueId())&&p.hasPermission("kasaba.tac.admin")&&!visitors.contains(p.getUniqueId())&&!p.getWorld().getName().startsWith("tac_room_");}
    private void recover(Player p){if(!auth(p)||visitors.contains(p.getUniqueId())||!PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return;try{PlayerBackup.restore(getDataFolder(),p,true);tell(p,"Önceki oyun oturumundaki eşyaların ve durumun geri yüklendi.");}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Taç kurtarma dosyası korundu.",e);p.kick(net.kyori.adventure.text.Component.text("Lanetli Taç envanter kaydın okunamadı. Yöneticiye haber ver."));}}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(PlayerAuthenticatedEvent e){recover(e.getPlayer());}
    @EventHandler public void joined(PlayerJoinEvent e){quitting.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.LOWEST) public void quit(PlayerQuitEvent e){quitting.add(e.getPlayer().getUniqueId());leave(e.getPlayer().getUniqueId());messageAt.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(PlayerMoveEvent e){UUID id=e.getPlayer().getUniqueId();if(!visitors.contains(id)||internalTeleport.contains(id))return;CrownRoom room=membership.get(id);if(room==null){if(!insideLobby(e.getTo()))e.setTo(insideLobby(e.getFrom())?e.getFrom():lobbyPoint());return;}if(!room.contains(e.getTo())){e.setTo(e.getFrom());return;}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void travel(PlayerTeleportEvent e){UUID id=e.getPlayer().getUniqueId();if(visitors.contains(id)&&!internalTeleport.contains(id))e.setCancelled(true);else if(!visitors.contains(id)&&e.getTo().getWorld().getName().startsWith("tac_room_")&&!stopping)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){Player p=e.getPlayer();if(!visitors.contains(p.getUniqueId())){if(e.getClickedBlock()!=null&&managed(e.getClickedBlock().getWorld())&&!editing(p))e.setCancelled(true);return;}e.setCancelled(true);if(e.getHand()!=EquipmentSlot.HAND||!e.getAction().isRightClick())return;String tag=action(e.getItem());if(tag.equals("menu")){menu(p);return;}}
    @EventHandler(priority=EventPriority.HIGHEST) public void interactEntity(PlayerInteractEntityEvent e){if(visitors.contains(e.getPlayer().getUniqueId())||managed(e.getRightClicked().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player&&(visitors.contains(e.getEntity().getUniqueId())||managed(e.getEntity().getWorld())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p&&visitors.contains(p.getUniqueId())){MenuClicks.open(this,e,item->action(item).equals("menu"),()->{if(auth(p)&&visitors.contains(p.getUniqueId()))menu(p);});e.setCancelled(true);}}
    @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(visitors.contains(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent e){if(visitors.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e){if(visitors.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent e){if(visitors.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void breakBlock(BlockBreakEvent e){if(managed(e.getBlock().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void placeBlock(BlockPlaceEvent e){if(managed(e.getBlock().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void hunger(FoodLevelChangeEvent e){if(visitors.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void flight(PlayerToggleFlightEvent e){if(membership.containsKey(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void gameMode(PlayerGameModeChangeEvent e){if(visitors.contains(e.getPlayer().getUniqueId())&&e.getNewGameMode()!=GameMode.ADVENTURE)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void commands(PlayerCommandPreprocessEvent e){if(!membership.containsKey(e.getPlayer().getUniqueId()))return;String command=normalize(e.getMessage().substring(1).split(" ",2)[0]);if(command.contains(":"))command=command.substring(command.indexOf(':')+1);if(!Set.of("tac","lanetlitac","lobi","oyunlar","oyunceza","tablo","kasababilgi").contains(command)){e.setCancelled(true);tell(e.getPlayer(),"Maçta /tac veya /lobi kullanabilirsin.");}}
    @EventHandler public void spawn(EntitySpawnEvent e){if(managed(e.getEntity().getWorld())&&(e.getEntity() instanceof Mob||e.getEntity() instanceof TNTPrimed))e.setCancelled(true);}
    @EventHandler public void explode(EntityExplodeEvent e){if(managed(e.getLocation().getWorld()))e.setCancelled(true);}
    @EventHandler public void multiverse(org.bukkit.event.server.PluginEnableEvent e){if(e.getPlugin().getName().equals("Multiverse-Core"))Bukkit.getScheduler().runTask(this,()->{new MultiverseBridge(this).register(lobbyWorld,lobbyPoint());World w=Bukkit.getWorld(map.worldName);new MultiverseBridge(this).register(w,map.lobby.at(w));});}
    private void validateWorld(CrownMap map,World world)throws IOException{for(CrownMap.Point spawn:map.spawns)if(!safe(spawn.at(world)))throw new IOException("Şato doğması kapalı: "+spawn);if(!safe(map.lobby.at(world)))throw new IOException("Şato yönetici doğması kapalı.");}
    private boolean safe(Location at){return at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid();}
    private void testMaps(CommandSender sender)throws IOException{World copy=null;try{copy=instances.create(nextRoom++,map);validateWorld(map,copy);tell(sender,"§aAyrı lobi, 16 şato doğması ve iki kapı doğrulandı.");}finally{if(copy!=null)instances.close(copy);}}
}
