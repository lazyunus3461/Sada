package tr.kasabatr.esya;

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

/** Hub-managed three-map Prop Hunt, with explicit queue admission and isolated matches. */
public final class KasabaEsyaPlugin extends JavaPlugin implements HubGame,Listener,TabExecutor {
    static final String ID="esyasaklambaci";
    record Session(UUID party,List<UUID> members){}
    final Set<UUID> visitors=new LinkedHashSet<>(),queue=new LinkedHashSet<>();
    final Map<UUID,Session> sessions=new HashMap<>();
    final Map<UUID,PropRoom> membership=new HashMap<>();
    final Map<Integer,PropRoom> rooms=new LinkedHashMap<>();
    final Random random=new Random();
    final List<ToyMap> maps=new ArrayList<>(),mapBag=new ArrayList<>();
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
        queueBar=Bukkit.createBossBar("§dEşya Saklambacı",BarColor.PURPLE,BarStyle.SOLID);saveDefaultConfig();screens=new GameScreens(this);toolKey=new NamespacedKey(this,"esya-tool");
        try{
            List<String> names=getConfig().getStringList("template-worlds");if(names.size()!=3||new HashSet<>(names).size()!=3)throw new IOException("Üç farklı harita gerekli.");
            for(String name:names){MapInstaller.ensure(this,name);ToyMap map=new ToyMap(Bukkit.getWorldContainer().toPath().resolve(name).resolve("prop-map.json"));if(!map.worldName.equals(name))throw new IOException("Harita adı config ile uyuşmuyor: "+name);maps.add(map);}
            instances=new WorldInstances(this);instances.clearStaleCopies();
            for(ToyMap map:maps){World world=Bukkit.getWorld(map.worldName);if(world==null)world=Bukkit.createWorld(new WorldCreator(map.worldName).generator(new EmptyWorldGenerator()).generateStructures(false));if(world==null)throw new IOException("Harita açılamadı: "+map.worldName);configure(world);validateWorld(map,world);new MultiverseBridge(this).register(world,map.lobby.at(world));}
            lobbyWorld=LobbyWorld.open();configure(lobbyWorld);new MultiverseBridge(this).register(lobbyWorld,lobbyPoint());
            hub=Bukkit.getServicesManager().load(HubApi.class);if(hub==null)throw new IOException("KasabaHub bulunamadı.");hub.register(this,this);presentLobby();
        }catch(Exception e){getLogger().log(Level.SEVERE,"Eşya Saklambacı açılamadı; mevcut dünya/veri dosyaları korundu.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        Bukkit.getPluginManager().registerEvents(this,this);Objects.requireNonNull(getCommand("esya")).setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this,this::tick,5,5);for(Player p:Bukkit.getOnlinePlayers())recover(p);
        getLogger().info("Eşya Saklambacı hazır: ayrı lobi dünyası ve üç harita, /oyunlar, /esya; normal maç 4–12, yönetici denemesi 2 kişi.");
    }
    @Override public void onDisable(){
        stopping=true;if(queueBar!=null)queueBar.removeAll();
        if(hub!=null)try{hub.unregister(this,ID);}catch(RuntimeException e){getLogger().warning(e.getMessage());}
        for(UUID id:List.copyOf(visitors)){Player p=Bukkit.getPlayer(id);if(p!=null)detach(p,true);else leave(id);}
        for(PropRoom room:List.copyOf(rooms.values()))room.close();
    }
    private void configure(World world){world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.KEEP_INVENTORY,true);world.setGameRule(GameRule.LOCATOR_BAR,false);world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);world.setTime(1000);world.setStorm(false);world.setThundering(false);}
    @Override public ChunkGenerator getDefaultWorldGenerator(String name,String id){return new EmptyWorldGenerator();}
    @Override public GameInfo info(){return new GameInfo(ID,"Eşya Saklambacı","Oyuncakçı / Müze / Akvaryum • kılık değiştir • 4–12 kişi",1,12);}
    @Override public boolean available(){return !stopping&&hub!=null&&lobbyWorld!=null&&Bukkit.getWorld(lobbyWorld.getUID())==lobbyWorld&&maps.size()==3&&maps.stream().allMatch(map->Bukkit.getWorld(map.worldName)!=null&&instances.templateReady(map));}
    @Override public boolean lobby(UUID id){return visitors.contains(id)&&!membership.containsKey(id);}
    @Override public boolean penaltyEligible(UUID id){PropRoom room=membership.get(id);Player p=Bukkit.getPlayer(id);return !stopping&&room!=null&&room.active()&&!room.practice&&p!=null&&!p.isDead();}
    @Override public boolean containsMatch(UUID id,Location at){PropRoom room=membership.get(id);return room==null||room.contains(at);}
    boolean auth(Player p){AuthState state=Bukkit.getServicesManager().load(AuthState.class);return p.isOnline()&&state!=null&&state.authenticated(p.getUniqueId());}
    void ready(Player p){if(!auth(p)||!visitors.contains(p.getUniqueId()))throw new IllegalArgumentException("Önce /oyunlar üzerinden Eşya Saklambacı lobisine gir.");GameAccessGuard guard=Bukkit.getServicesManager().load(GameAccessGuard.class);if(guard!=null&&guard.blocked(p.getUniqueId()))throw new IllegalArgumentException(guard.message(p.getUniqueId()));}
    void tell(CommandSender p,String text){p.sendMessage("§d[Eşya Saklambacı] §f"+text);}
    void abandon(UUID id){if(hub!=null)hub.abandon(id);}
    @Override public boolean join(List<Player> team){
        if(!available()||team==null||team.isEmpty()||team.size()>12)return false;
        for(Player p:team)if(p==null||!auth(p)||!p.hasPermission("kasaba.esya.play")||visitors.contains(p.getUniqueId())||PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return false;
        List<Player> prepared=new ArrayList<>();Map<Player,Location> before=new LinkedHashMap<>();UUID party=UUID.randomUUID();List<UUID> ids=team.stream().map(Player::getUniqueId).toList();
        try{
            for(Player p:team){p.closeInventory();PlayerBackup.save(getDataFolder(),p);before.put(p,p.getLocation());prepared.add(p);visitors.add(p.getUniqueId());sessions.put(p.getUniqueId(),new Session(party,ids));prepareLobby(p);}
            for(Player p:team){tell(p,"Lobiye geldin. Pusuladan Oyna seç; burada bekleyebilir veya başka oyun lobisine geçebilirsin.");Bukkit.getScheduler().runTaskLater(this,()->{if(auth(p)&&lobby(p.getUniqueId()))menu(p);},2);}
            return true;
        }catch(Exception e){getLogger().log(Level.WARNING,"Eşya grup girişi geri alınıyor.",e);for(Player p:prepared){detach(p,false);try{teleport(p,before.get(p));}catch(RuntimeException ignored){}}return false;}
    }
    @Override public void leave(UUID id){abandon(id);Player p=Bukkit.getPlayer(id);if(p!=null)detach(p,stopping);else{visitors.remove(id);sessions.remove(id);queue.remove(id);PropRoom room=membership.remove(id);if(room!=null)room.remove(id);}}
    private void detach(Player p,boolean restoreLocation){
        UUID id=p.getUniqueId();abandon(id);boolean owned=visitors.remove(id);sessions.remove(id);queue.remove(id);queueBar.removePlayer(p);editors.remove(id);screens.clear(id);p.closeDialog();
        PropRoom room=membership.remove(id);if(room!=null)room.remove(id);reveal(p);if(!owned)return;
        if(!quitting.contains(id))try{internalTeleport.add(id);PlayerBackup.restore(getDataFolder(),p,restoreLocation&&!hub.transferring(id));}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Eşya yedeği korundu: "+p.getName(),e);p.kick(net.kyori.adventure.text.Component.text("Eşya Saklambacı envanter yedeğin korunuyor. Yöneticiye haber ver."));}finally{internalTeleport.remove(id);}
        if(room!=null&&p.getWorld()==room.world&&!stopping)try{teleport(p,lobbyPoint());}catch(RuntimeException e){getLogger().warning("Geçici dünyadan çıkarma yeniden denenecek: "+p.getName());}
    }
    void prepare(Player p,Location at){
        if(NetworkFeatures.get()!=null)NetworkFeatures.get().beforeGame(p);screens.clear(p.getUniqueId());p.closeDialog();p.closeInventory();p.setItemOnCursor(null);p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);
        for(var effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());p.setGameMode(GameMode.ADVENTURE);p.setFlySpeed(.1f);p.setFlying(false);p.setAllowFlight(false);p.setGravity(true);p.setInvulnerable(false);p.setAbsorptionAmount(0);p.setCollidable(false);p.setFireTicks(0);p.setFallDistance(0);p.setNoDamageTicks(0);p.setFoodLevel(20);p.setSaturation(20);p.setExhaustion(0);if(!p.isDead())p.setHealth(Math.min(20,p.getMaxHealth()));p.setTotalExperience(0);p.setLevel(0);p.setExp(0);teleport(p,at);
    }
    private void prepareLobby(Player p){prepare(p,lobbyPoint());tools(p);}
    Location lobbyPoint(){return LobbyWorld.spawn(lobbyWorld);}
    void teleport(Player p,Location to){internalTeleport.add(p.getUniqueId());try{if(p.isInsideVehicle())p.leaveVehicle();if(!p.teleport(to,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Işınlanma engellendi.");p.setFallDistance(0);}finally{internalTeleport.remove(p.getUniqueId());}}
    void reveal(Player p){for(Player viewer:Bukkit.getOnlinePlayers())if(viewer!=p)viewer.showPlayer(this,p);}
    private boolean enter(Player p){if(!auth(p))throw new IllegalArgumentException("Önce hesabına giriş yap.");if(visitors.contains(p.getUniqueId()))return true;if(hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Mevcut oyunun lobisindeki pusuladan geçiş yap veya /lobi kullan.");Bukkit.dispatchCommand(p,"oyunlar git "+ID);return visitors.contains(p.getUniqueId());}
    private List<Player> party(Player p){ready(p);Session s=sessions.get(p.getUniqueId());List<Player> members=s.members.stream().filter(visitors::contains).map(Bukkit::getPlayer).filter(Objects::nonNull).toList();if(members.isEmpty()||members.getFirst()!=p)throw new IllegalArgumentException("Oynama seçimini grup lideri yapmalı.");for(Player member:members){ready(member);hub.requireGameEntry(member.getUniqueId(),ID);if(membership.containsKey(member.getUniqueId()))throw new IllegalArgumentException("Grubundaki herkes oyun lobisinde olmalı.");}return members;}
    private void play(Player p){for(Player member:party(p)){queue.add(member.getUniqueId());queueBar.addPlayer(member);member.closeDialog();screens.clear(member.getUniqueId());tell(member,"Sıradasın: "+queue.size()+"/4 • En az 4 kişiyle 15 saniyelik sayım başlar.");}}
    private void cancelQueue(Player p){for(Player member:party(p)){queue.remove(member.getUniqueId());queueBar.removePlayer(member);screens.clear(member.getUniqueId());member.closeDialog();tell(member,"Sıradan çıktın; bu lobide kalabilirsin.");}if(queue.size()<4)queueDeadline=0;}
    private List<UUID> selectBatch(){Map<UUID,List<UUID>> groups=new LinkedHashMap<>();for(UUID id:queue){Session s=sessions.get(id);if(s!=null)groups.computeIfAbsent(s.party,k->new ArrayList<>()).add(id);}return EsyaRules.batch(new ArrayList<>(groups.values()),12);}
    private int maximumRooms(){return Math.clamp(getConfig().getInt("max-concurrent-rooms",3),1,8);}
    private void start(boolean practice){
        List<UUID> selected=selectBatch();if(selected.size()<(practice?2:4)||practice&&selected.size()!=2)throw new IllegalArgumentException(practice?"Deneme için sırada tam 2 oyuncu olmalı.":"Normal maç için en az 4 kişi sırada olmalı.");if(rooms.size()>=maximumRooms())throw new IllegalArgumentException("Odalar dolu; bir maçın bitmesini bekle.");
        for(UUID id:selected){Player p=Bukkit.getPlayer(id);if(p==null)throw new IllegalArgumentException("Oyuncu ayrıldı.");ready(p);hub.requireGameEntry(id,ID);}
        ToyMap map=nextMap();PropRoom room=new PropRoom(this,nextRoom++,map,practice);queueDeadline=0;
        try{room.world=instances.create(room.id,map);rooms.put(room.id,room);room.players.addAll(selected);for(UUID id:selected){queue.remove(id);membership.put(id,room);Player p=Bukkit.getPlayer(id);queueBar.removePlayer(p);p.closeDialog();screens.clear(id);}room.start();}
        catch(Exception e){getLogger().log(Level.SEVERE,"Eşya odası güvenli biçimde kapatılıyor.",e);room.close();for(UUID id:selected){queue.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null){queueBar.removePlayer(p);tell(p,"Maç açılamadı; lobiye döndün. /esya ile yeniden deneyebilirsin.");}}}
    }
    private ToyMap nextMap(){if(mapBag.isEmpty()){mapBag.addAll(maps);Collections.shuffle(mapBag,random);}return mapBag.removeLast();}
    private void tick(){
        if(!available())return;long now=System.currentTimeMillis();queue.removeIf(id->!visitors.contains(id)||Bukkit.getPlayer(id)==null||membership.containsKey(id));
        for(UUID id:List.copyOf(visitors)){Player p=Bukkit.getPlayer(id);if(p!=null&&lobby(id)&&!insideLobby(p.getLocation()))returnGameLobby(p,0);}
        if(queue.size()>=4&&rooms.size()<maximumRooms()){
            int duration=Math.clamp(getConfig().getInt("countdown-seconds",15),5,60);if(queueDeadline==0)queueDeadline=now+duration*1000L;long left=Math.max(0,(queueDeadline-now+999)/1000);
            queueBar.setColor(left<=5?BarColor.RED:BarColor.PURPLE);queueBar.setTitle("§dEşya Saklambacı §8• §eBaşlamasına "+left+" sn §8• §b"+queue.size()+" oyuncu");queueBar.setProgress(Math.clamp((queueDeadline-now)/(duration*1000.0),0,1));
            if(now>=queueDeadline)try{start(false);}catch(RuntimeException e){queueDeadline=0;getLogger().log(Level.WARNING,"Eşya başlangıcı ertelendi.",e);}
        }else{queueDeadline=0;queueBar.setColor(BarColor.BLUE);queueBar.setTitle("§dEşya Saklambacı §8• §b"+queue.size()+"/4 §7• "+(rooms.size()>=maximumRooms()?"Odalar dolu":"Oyuncu bekleniyor"));queueBar.setProgress(Math.clamp(queue.size()/4.0,0,1));}
        for(PropRoom room:List.copyOf(rooms.values()))try{room.tick(now);}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Eşya odası kapatılıyor: "+room.id,e);room.close();}
    }
    void toLobby(Player p){ready(p);abandon(p.getUniqueId());queue.remove(p.getUniqueId());queueBar.removePlayer(p);PropRoom room=membership.remove(p.getUniqueId());if(room!=null)room.remove(p.getUniqueId());returnGameLobby(p,0);}
    void returnGameLobby(Player p,int attempt){
        if(!p.isOnline()||!visitors.contains(p.getUniqueId())||membership.containsKey(p.getUniqueId()))return;
        try{if(p.isDead())p.spigot().respawn();prepareLobby(p);}catch(RuntimeException e){if(!stopping&&attempt<3)Bukkit.getScheduler().runTaskLater(this,()->returnGameLobby(p,attempt+1),10);else if(hub!=null)hub.returnToLobbyForced(p.getUniqueId());}
    }
    void closeWhenEmpty(World world,int attempt){if(world.getPlayers().isEmpty()){instances.close(world);return;}if(!stopping&&attempt<20)Bukkit.getScheduler().runTaskLater(this,()->closeWhenEmpty(world,attempt+1),10);else getLogger().warning("İçinde oyuncu olan geçici dünya korundu: "+world.getName());}
    Set<ToyMap.Pos> placeDecoys(ToyMap map,World world){for(ToyMap.Pos p:map.decoyCandidates)world.getBlockAt(p.x(),p.y(),p.z()).setType(Material.AIR,false);List<ToyMap.Pos> sites=new ArrayList<>(map.decoyCandidates);Collections.shuffle(sites,random);Set<ToyMap.Pos> active=new LinkedHashSet<>();List<Material> kinds=new ArrayList<>();for(int i=0;i<map.decoys.size();i++)kinds.add(map.materials.get(i%map.materials.size()));Collections.shuffle(kinds,random);for(int i=0;i<kinds.size();i++){ToyMap.Pos p=sites.get(i);world.getBlockAt(p.x(),p.y(),p.z()).setType(kinds.get(i),false);active.add(p);}return active;}
    private ItemStack tool(Material material,String text,String action){ItemStack item=new ItemStack(material);item.editMeta(meta->{meta.setDisplayName(text);meta.getPersistentDataContainer().set(toolKey,PersistentDataType.STRING,action);});return item;}
    private String action(ItemStack item){return item!=null&&item.hasItemMeta()?item.getItemMeta().getPersistentDataContainer().getOrDefault(toolKey,PersistentDataType.STRING,""):"";}
    void tools(Player p){PropRoom room=membership.get(p.getUniqueId());p.getInventory().setItem(0,room!=null&&room.seekers.contains(p.getUniqueId())?tool(Material.SPYGLASS,"§c§lEbe Merceği §7• Tahmin et","guess"):null);p.getInventory().setItem(8,tool(Material.COMPASS,"§d§lEşya Saklambacı §7• Menü","menu"));}
    private void presentLobby(){hub.presentLobby(this,ID,lobbyPoint(),"EŞYA SAKLAMBACI",List.of("§ePusuladan Oyna seç §8• §b4–12 kişi","§7Eşyaya dönüş, saklan §8• §d3 rastgele harita"),"GALİBİYET");}
    String mapName(ToyMap map){return map.worldName.contains("Oyuncakci")?"Oyuncakçı":map.worldName.contains("Muze")?"Müze":map.worldName.contains("Akvaryum")?"Akvaryum":map.worldName;}
    void menu(Player p){
        ready(p);PropRoom room=membership.get(p.getUniqueId());List<String> lines=new ArrayList<>();List<GameScreens.Button> buttons=new ArrayList<>();
        if(room==null){lines.add("§eÜç harita: §fOyuncakçı • Müze • Akvaryum");lines.add("§7Kendiliğinden sıraya girmezsin. Oyna seç veya bu lobide kal.");lines.add("§bSırada "+queue.size()+"/4 §8• §dAktif oda "+rooms.size()+"/"+maximumRooms());buttons.add(new GameScreens.Button(queue.contains(p.getUniqueId())?"Sıradan çık":"Oyna",()->{if(queue.contains(p.getUniqueId()))cancelQueue(p);else play(p);}));buttons.add(new GameScreens.Button("Odaların durumu",()->roomInfo(p)));buttons.add(new GameScreens.Button("Oyun lobileri",()->hub.openGames(p.getUniqueId())));}
        else{lines.add("§dOda "+room.id+" §8• §e"+mapName(room.map));lines.add(room.hiders.contains(p.getUniqueId())?"§aSaklanıyorsun; iki kez zıpla, uç ve bloğun yanında hizalan.":"§cEbesin; gerçek oyuncu kılığını bulmak için tıkla.");if(room.hiders.contains(p.getUniqueId())&&room.active())buttons.add(new GameScreens.Button("Kılık seç",()->disguiseMenu(p)));buttons.add(new GameScreens.Button("Eşya lobisine dön",()->toLobby(p)));}
        buttons.add(new GameScreens.Button("Nasıl oynanır?",()->help(p)));buttons.add(new GameScreens.Button(room==null?"Bu lobide kal":"Oyuna dön",()->{if(room==null&&queue.contains(p.getUniqueId()))cancelQueue(p);}));buttons.add(new GameScreens.Button("Ana spawna dön",()->hub.returnToLobby(p.getUniqueId())));screens.show(p,"Eşya Saklambacı",lines,buttons,room==null);
    }
    private void roomInfo(Player p){List<String> lines=new ArrayList<>();for(PropRoom room:rooms.values())lines.add("§dOda "+room.id+" §8• §e"+mapName(room.map)+" §8• §b"+room.players.size()+" kişi §8• §7"+(room.practice?"Deneme":room.phase==PropRoom.Phase.HIDING?"Saklanma":room.phase==PropRoom.Phase.HUNTING?"Arama":"Sonuçlar"));if(lines.isEmpty())lines.add("§7Henüz başlayan oda yok.");screens.show(p,"Eşya Saklambacı • Odalar",lines,List.of(new GameScreens.Button("Geri",()->menu(p))),true);}
    private void help(Player p){screens.show(p,"Eşya Saklambacı • Nasıl oynanır?",List.of("§e4 kişi sıradayken 15 saniyelik sayım başlar; maç en fazla 12 kişi.","§d4–7 kişide 1 ebe, 8–12 kişide 2 ebe rastgele seçilir.","§aSaklananlar 20 saniyede kılık ve yer seçer. Ebeler sonra serbest kalır.","§7Kılık seçimi pusulada; değiştirdikten sonra 20 saniye beklenir.","§bSaklanan: iki kez zıpla, yavaşça uç; zıpla yüksel, eğil alçal.","§aBir bloğun yanında durunca aynı seviyeye otomatik hizalanırsın.","§cYanlış tahmin 3 saniye yavaşlatır. Duvar arkasından yakalama yok.","§eBulunan oyuncu ebe olarak yardım eder. Ebeler herkesi bulursa kazanır.","§a4 dakika bitince hâlâ saklanan oyuncular kazanır.","§7Galibiyet: başlangıçtaki ebeler veya süreyi bitiren saklananlar.","§cBaşlayan normal maçı terk etmek bu oyuna 15 dakika giriş cezası verir.","§b/esya • /esya oyna • /esya kılık • /esya lobi • /lobi"),List.of(new GameScreens.Button("Geri",()->menu(p))),false);}
    private void disguiseMenu(Player p){ready(p);PropRoom room=membership.get(p.getUniqueId());if(room==null||!room.active()||!room.hiders.contains(p.getUniqueId()))throw new IllegalArgumentException("Yalnız saklananlar kılık seçebilir.");List<GameScreens.Button> buttons=new ArrayList<>();for(Material kind:room.map.materials)buttons.add(new GameScreens.Button(label(kind),()->room.change(p,kind)));buttons.add(new GameScreens.Button("Oyuna dön",()->{}));screens.show(p,"Eşya Saklambacı • Kılık seç",List.of("§dMevcut kılığın: §f"+label(room.disguises.getOrDefault(p.getUniqueId(),room.map.materials.getFirst())),"§7Haritaya uygun bir eşya seç; seçimler arasında 20 saniye beklenir."),buttons,false);}
    static String label(Material material){return switch(material){case RED_WOOL->"Kırmızı Yün";case BLUE_WOOL->"Mavi Yün";case YELLOW_WOOL->"Sarı Yün";case CYAN_WOOL->"Turkuaz Yün";case BARREL->"Varil";case BOOKSHELF->"Kitaplık";case OAK_PLANKS->"Meşe Tahtası";case PRISMARINE->"Prizmarin";case DARK_PRISMARINE->"Koyu Prizmarin";case BLUE_TERRACOTTA->"Mavi Terakota";case QUARTZ_BLOCK->"Kuvars Bloğu";case CHISELED_QUARTZ_BLOCK->"Yontulmuş Kuvars";case RED_TERRACOTTA->"Kırmızı Terakota";case GOLD_BLOCK->"Altın Bloğu";default->material.name().replace('_',' ');};}
    private String normalize(String value){return value.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ö','o').replace('ü','u');}
    private void admin(CommandSender p){if(!p.hasPermission("kasaba.esya.admin"))throw new IllegalArgumentException("Bu komut yöneticiye aittir.");}
    @Override public boolean onCommand(CommandSender sender,Command command,String alias,String[] args){
        try{String sub=args.length==0?"menu":normalize(args[0]);
            if(sub.equals("testharita")){admin(sender);if(!rooms.isEmpty())throw new IllegalArgumentException("Harita testi için aktif odaların bitmesini bekle.");testMaps(sender);return true;}
            if(!(sender instanceof Player p))throw new IllegalArgumentException("Bu komutu oyunda kullan.");
            if(sub.equals("duzenleme")){admin(p);if(visitors.contains(p.getUniqueId())||hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile oyundan çık.");if(args.length!=2)throw new IllegalArgumentException("/esya düzenleme aç|kapat");if(normalize(args[1]).equals("ac"))editors.add(p.getUniqueId());else if(normalize(args[1]).equals("kapat"))editors.remove(p.getUniqueId());else throw new IllegalArgumentException("/esya düzenleme aç|kapat");tell(p,"Harita düzenleme: "+(editors.contains(p.getUniqueId())?"açık":"kapalı"));return true;}
            if(!enter(p))return true;
            switch(sub){case "menu","katil"->menu(p);case "oyna"->play(p);case "kilik"->disguiseMenu(p);case "durum"->{tell(p,"Sırada: "+queue.size()+"/4 • Aktif oda: "+rooms.size()+"/"+maximumRooms());roomInfo(p);}case "lobi"->toLobby(p);case "cik"->hub.returnToLobby(p.getUniqueId());case "baslat","deneme"->{admin(p);if(!lobby(p.getUniqueId())||!queue.contains(p.getUniqueId()))throw new IllegalArgumentException("Önce /esya oyna ile sıraya gir.");start(sub.equals("deneme"));}case "yardim"->help(p);default->throw new IllegalArgumentException("/esya • oyna • kılık • durum • lobi • çık • yardım");}
        }catch(Exception e){tell(sender,e.getMessage()==null?"İşlem tamamlanamadı.":e.getMessage());}return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){return args.length==1?List.of("oyna","kılık","durum","lobi","çık","yardım","başlat","deneme","testharita","düzenleme").stream().filter(s->normalize(s).startsWith(normalize(args[0]))).toList():args.length==2&&normalize(args[0]).equals("duzenleme")?List.of("aç","kapat"):List.of();}
    private boolean insideLobby(Location at){return LobbyWorld.contains(at,lobbyWorld);}
    private boolean managed(World world){return world==lobbyWorld||world.getName().startsWith("esya_room_")||maps.stream().anyMatch(map->map.worldName.equals(world.getName()));}
    private boolean editing(Player p){return editors.contains(p.getUniqueId())&&p.hasPermission("kasaba.esya.admin")&&!visitors.contains(p.getUniqueId())&&!p.getWorld().getName().startsWith("esya_room_");}
    private void recover(Player p){if(!auth(p)||visitors.contains(p.getUniqueId())||!PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return;try{PlayerBackup.restore(getDataFolder(),p,true);tell(p,"Önceki oyun oturumundaki eşyaların ve durumun geri yüklendi.");}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Eşya kurtarma dosyası korundu.",e);p.kick(net.kyori.adventure.text.Component.text("Eşya Saklambacı envanter kaydın okunamadı. Yöneticiye haber ver."));}}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(PlayerAuthenticatedEvent e){recover(e.getPlayer());}
    @EventHandler public void joined(PlayerJoinEvent e){quitting.remove(e.getPlayer().getUniqueId());for(PropRoom room:rooms.values())for(UUID id:room.hiders){Player hidden=Bukkit.getPlayer(id);if(hidden!=null&&hidden!=e.getPlayer())e.getPlayer().hidePlayer(this,hidden);}}
    @EventHandler(priority=EventPriority.LOWEST) public void quit(PlayerQuitEvent e){quitting.add(e.getPlayer().getUniqueId());leave(e.getPlayer().getUniqueId());messageAt.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void move(PlayerMoveEvent e){UUID id=e.getPlayer().getUniqueId();if(!visitors.contains(id)||internalTeleport.contains(id))return;PropRoom room=membership.get(id);if(room==null){if(!insideLobby(e.getTo()))e.setTo(insideLobby(e.getFrom())?e.getFrom():lobbyPoint());return;}if(!room.contains(e.getTo())){e.setTo(e.getFrom());return;}if(room.phase==PropRoom.Phase.HIDING&&room.seekers.contains(id)&&e.getTo().distanceSquared(room.map.seekerWait.at(room.world))>25){e.setTo(e.getFrom());return;}if(room.hiders.contains(id)){if(e.getPlayer().isFlying()&&!room.canFloat(e.getTo())){e.setTo(e.getFrom());return;}if(e.hasChangedPosition())room.follow(e.getPlayer(),e.getTo());}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void travel(PlayerTeleportEvent e){UUID id=e.getPlayer().getUniqueId();if(visitors.contains(id)&&!internalTeleport.contains(id))e.setCancelled(true);else if(!visitors.contains(id)&&e.getTo().getWorld().getName().startsWith("esya_room_")&&!stopping)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){Player p=e.getPlayer();if(!visitors.contains(p.getUniqueId())){if(e.getClickedBlock()!=null&&managed(e.getClickedBlock().getWorld())&&!editing(p))e.setCancelled(true);return;}e.setCancelled(true);if(e.getHand()!=EquipmentSlot.HAND)return;String action=action(e.getItem());if(action.equals("menu")&&e.getAction().name().startsWith("RIGHT_CLICK")){menu(p);return;}PropRoom room=membership.get(p.getUniqueId());if(room==null||room.phase!=PropRoom.Phase.HUNTING||!room.seekers.contains(p.getUniqueId()))return;
        var ray=p.getWorld().rayTraceEntities(p.getEyeLocation(),p.getLocation().getDirection(),4.5,.05,entity->room.hitboxOwner.containsKey(entity.getUniqueId()));
        if(ray!=null&&ray.getHitEntity()!=null){room.guess(p,room.hitboxOwner.get(ray.getHitEntity().getUniqueId()));return;}
        if(e.getClickedBlock()!=null&&room.map.materials.contains(e.getClickedBlock().getType())&&p.getEyeLocation().distanceSquared(e.getClickedBlock().getLocation().add(.5,.5,.5))<=25)room.guess(p,null);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void interactEntity(PlayerInteractEntityEvent e){PropRoom room=membership.get(e.getPlayer().getUniqueId());if(room!=null){e.setCancelled(true);if(e.getHand()==EquipmentSlot.HAND&&room.hitboxOwner.containsKey(e.getRightClicked().getUniqueId()))room.guess(e.getPlayer(),room.hitboxOwner.get(e.getRightClicked().getUniqueId()));}else if(managed(e.getRightClicked().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void attack(EntityDamageByEntityEvent e){if(managed(e.getEntity().getWorld()))e.setCancelled(true);if(e.getDamager() instanceof Player p){PropRoom room=membership.get(p.getUniqueId());if(room!=null&&room.hitboxOwner.containsKey(e.getEntity().getUniqueId()))room.guess(p,room.hitboxOwner.get(e.getEntity().getUniqueId()));}}
    @EventHandler(priority=EventPriority.HIGHEST) public void damage(EntityDamageEvent e){if(e.getEntity() instanceof Player&&(visitors.contains(e.getEntity().getUniqueId())||managed(e.getEntity().getWorld())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventory(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p&&visitors.contains(p.getUniqueId())){MenuClicks.open(this,e,item->action(item).equals("menu"),()->{if(auth(p)&&visitors.contains(p.getUniqueId()))menu(p);});e.setCancelled(true);}}
    @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(visitors.contains(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent e){if(visitors.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e){if(visitors.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent e){if(visitors.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void breakBlock(BlockBreakEvent e){if(managed(e.getBlock().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void placeBlock(BlockPlaceEvent e){if(managed(e.getBlock().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void hunger(FoodLevelChangeEvent e){if(visitors.contains(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void flight(PlayerToggleFlightEvent e){PropRoom room=membership.get(e.getPlayer().getUniqueId());if(room!=null&&(!room.active()||!room.hiders.contains(e.getPlayer().getUniqueId())||e.isFlying()&&!room.canFloat(e.getPlayer().getLocation())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void gameMode(PlayerGameModeChangeEvent e){if(visitors.contains(e.getPlayer().getUniqueId())&&e.getNewGameMode()!=GameMode.ADVENTURE)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void commands(PlayerCommandPreprocessEvent e){if(!membership.containsKey(e.getPlayer().getUniqueId()))return;String command=normalize(e.getMessage().substring(1).split(" ",2)[0]);if(command.contains(":"))command=command.substring(command.indexOf(':')+1);if(!Set.of("esya","esyasaklambaci","lobi","oyunlar","oyunceza","tablo","kasababilgi").contains(command)){e.setCancelled(true);tell(e.getPlayer(),"Maçta /esya veya /lobi kullanabilirsin.");}}
    @EventHandler public void spawn(EntitySpawnEvent e){if(managed(e.getEntity().getWorld())&&(e.getEntity() instanceof Mob||e.getEntity() instanceof TNTPrimed||e.getEntity() instanceof Projectile))e.setCancelled(true);}
    @EventHandler public void explode(EntityExplodeEvent e){if(managed(e.getLocation().getWorld()))e.setCancelled(true);}
    @EventHandler public void multiverse(org.bukkit.event.server.PluginEnableEvent e){if(e.getPlugin().getName().equals("Multiverse-Core"))Bukkit.getScheduler().runTask(this,()->{for(ToyMap map:maps)new MultiverseBridge(this).register(Bukkit.getWorld(map.worldName),map.lobby.at(Bukkit.getWorld(map.worldName)));new MultiverseBridge(this).register(lobbyWorld,lobbyPoint());});}
    private void validateWorld(ToyMap map,World world)throws IOException{for(ToyMap.Point point:map.hiderSpawns)if(!safe(point.at(world)))throw new IOException("Saklanan doğması güvenli değil: "+map.worldName);if(!safe(map.lobby.at(world))||!safe(map.seekerWait.at(world))||!safe(map.seekerRelease.at(world)))throw new IOException("Lobi/ebe doğması güvenli değil: "+map.worldName);}
    private boolean safe(Location at){return at.getBlock().isPassable()&&at.clone().add(0,1,0).getBlock().isPassable()&&at.clone().subtract(0,1,0).getBlock().getType().isSolid();}
    private void testMaps(CommandSender sender)throws IOException{for(ToyMap map:maps){World copy=null;try{copy=instances.create(nextRoom++,map);validateWorld(map,copy);for(ToyMap.Decoy decoy:map.decoys)if(copy.getBlockAt(decoy.pos().x(),decoy.pos().y(),decoy.pos().z()).getType()!=decoy.material())throw new IOException("Başlangıç yemi farklı: "+decoy.pos());Set<ToyMap.Pos> active=placeDecoys(map,copy);if(active.size()!=map.decoys.size())throw new IOException("Yem sayısı hatalı.");for(ToyMap.Pos pos:map.decoyCandidates){Material kind=copy.getBlockAt(pos.x(),pos.y(),pos.z()).getType();if(active.contains(pos)!=map.materials.contains(kind))throw new IOException("Yem konumu hatalı: "+pos);}tell(sender,"§a"+mapName(map)+": "+active.size()+" yem / "+map.decoyCandidates.size()+" aday ve doğmalar doğrulandı.");}finally{if(copy!=null)instances.close(copy);}}}
}
