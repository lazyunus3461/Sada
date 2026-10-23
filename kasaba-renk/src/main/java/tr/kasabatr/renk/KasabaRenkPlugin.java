package tr.kasabatr.renk;

import java.io.*;
import java.nio.file.*;
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

/** Original KasabaTR game: two chances, shrinking color deadlines and three separate boards. */
public final class KasabaRenkPlugin extends JavaPlugin implements HubGame,Listener,TabExecutor {
    record Session(UUID party,List<UUID> members){}
    enum Phase {WAITING,WARNING,DROP}
    static final Material[] BLOCKS={Material.RED_CONCRETE,Material.BLUE_CONCRETE,Material.LIME_CONCRETE,Material.YELLOW_CONCRETE,Material.PURPLE_CONCRETE,Material.ORANGE_CONCRETE,Material.WHITE_CONCRETE,Material.PINK_CONCRETE};
    static final String[] NAMES={"§cKIRMIZI","§9MAVİ","§aYEŞİL","§eSARI","§5MOR","§6TURUNCU","§fBEYAZ","§dPEMBE"};
    final Map<Integer,Room> rooms=new TreeMap<>();final Map<UUID,Room> membership=new HashMap<>();final Map<UUID,Session> sessions=new HashMap<>();
    private final Set<UUID> moving=new HashSet<>(),quitting=new HashSet<>();private HubApi hub;private World world;private GameScreens screens;private NamespacedKey menuKey;private boolean stopping;
    @Override public void onEnable(){
        saveDefaultConfig();screens=new GameScreens(this);menuKey=new NamespacedKey(this,"menu");
        try{
            String name=getConfig().getString("world","renkkacisi");if(!name.matches("[A-Za-z0-9_-]{1,60}"))throw new IOException("Geçersiz dünya adı.");Path folder=Bukkit.getWorldContainer().toPath().resolve(name);Path marker=folder.resolve(".kasaba-renk-dunya");if(Files.exists(folder)&&!Files.isRegularFile(marker))throw new IOException("Mevcut işaretsiz dünya korunuyor: "+name);
            world=Bukkit.getWorld(name);if(world==null)world=Bukkit.createWorld(new WorldCreator(name).type(WorldType.FLAT).generatorSettings("{\"layers\":[],\"biome\":\"minecraft:the_void\",\"structure_overrides\":[]}").generator(new EmptyWorldGenerator()).generateStructures(false));if(world==null)throw new IOException("Oyun dünyası açılamadı.");Files.writeString(marker,"KasabaRenk 1.0.0\n");
            world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);world.setGameRule(GameRule.KEEP_INVENTORY,true);world.setGameRule(GameRule.LOCATOR_BAR,false);world.setTime(1000);world.setStorm(false);world.setThundering(false);world.getWorldBorder().setCenter(128,0);world.getWorldBorder().setSize(768);
            buildLobby();for(int id=1;id<=3;id++){Room r=new Room(id);r.build();}new MultiverseBridge(this).register(world,lobbyPoint());
            hub=Bukkit.getServicesManager().load(HubApi.class);if(hub==null)throw new IOException("KasabaHub bulunamadı.");hub.register(this,this);
        }catch(Exception e){getLogger().log(Level.SEVERE,"Renk Kaçışı açılamadı; mevcut dünyalar korunuyor.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        Objects.requireNonNull(getCommand("renk")).setTabCompleter(this);Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getScheduler().runTaskTimer(this,this::tick,2,2);Bukkit.getOnlinePlayers().forEach(this::recover);
        getLogger().info("Renk Kaçışı hazır: /oyunlar → Renk Kaçışı • 3 oda • 2–8 kişi • iki hata hakkı.");
    }
    @Override public void onDisable(){stopping=true;for(Room room:List.copyOf(rooms.values()))finish(room,"Oyun kapanıyor.");if(hub!=null)try{hub.unregister(this,"renk");}catch(RuntimeException e){getLogger().warning(e.getMessage());}for(UUID id:List.copyOf(sessions.keySet()))leave(id);}
    @Override public ChunkGenerator getDefaultWorldGenerator(String name,String id){return new EmptyWorldGenerator();}
    @Override public GameInfo info(){return new GameInfo("renk","Renk Kaçışı","Hedef renge yetiş • iki hata hakkı • 2–8 kişilik 3 oda",1,8);}
    @Override public boolean available(){return world!=null&&Bukkit.getWorld(world.getUID())==world&&!stopping;}
    @Override public boolean lobby(UUID id){Room r=membership.get(id);return sessions.containsKey(id)&&(r==null||r.phase==Phase.WAITING);}
    private Location lobbyPoint(){return new Location(world,.5,81,-48.5,0,0);}
    private boolean auth(Player p){AuthState state=Bukkit.getServicesManager().load(AuthState.class);return state!=null&&state.authenticated(p.getUniqueId());}
    void ready(Player p){if(!auth(p)||!sessions.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Önce /oyunlar üzerinden Renk Kaçışı'na gir.");}
    void tell(CommandSender p,String text){p.sendMessage("§d[Renk Kaçışı] §f"+text);}
    @Override public boolean join(List<Player> team){
        if(!available()||team.isEmpty()||team.size()>8)return false;for(Player p:team)if(!p.isOnline()||!auth(p)||sessions.containsKey(p.getUniqueId())||PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return false;
        UUID party=UUID.randomUUID();List<UUID> ids=team.stream().map(Player::getUniqueId).toList();var prepared=new ArrayList<Player>();
        try{for(Player p:team){p.closeInventory();PlayerBackup.save(getDataFolder(),p);prepared.add(p);sessions.put(p.getUniqueId(),new Session(party,ids));prepareLobby(p);}for(Player p:team)Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&lobby(p.getUniqueId()))menu(p);},2);return true;}
        catch(Exception e){prepared.forEach(p->leave(p.getUniqueId()));getLogger().log(Level.WARNING,"Renk grup girişi geri alındı.",e);return false;}
    }
    @Override public void leave(UUID id){Session old=sessions.remove(id);screens.clear(id);Room room=membership.remove(id);Player p=Bukkit.getPlayer(id);if(room!=null)room.remove(id);if(old!=null&&p!=null&&!quitting.contains(id))try{PlayerBackup.restore(getDataFolder(),p,true);}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Renk eşya yedeği korundu.",e);p.kick(net.kyori.adventure.text.Component.text("Renk Kaçışı eşya yedeğin korunuyor. Yöneticiye haber ver."));}}
    private boolean enter(Player p){if(!auth(p))throw new IllegalArgumentException("Önce giriş yap.");if(sessions.containsKey(p.getUniqueId()))return true;if(hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile mevcut oyundan çık.");Bukkit.dispatchCommand(p,"oyunlar git renk");return sessions.containsKey(p.getUniqueId());}
    void teleport(Player p,Location target){moving.add(p.getUniqueId());try{if(!p.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Işınlanma engellendi.");p.setFallDistance(0);}finally{moving.remove(p.getUniqueId());}}
    private void reset(Player p){if(NetworkFeatures.get()!=null)NetworkFeatures.get().beforeGame(p);screens.clear(p.getUniqueId());p.closeDialog();p.closeInventory();p.setItemOnCursor(null);p.getInventory().clear();for(var e:p.getActivePotionEffects())p.removePotionEffect(e.getType());p.setAbsorptionAmount(0);p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);p.setGravity(true);p.setInvulnerable(false);p.setFireTicks(0);p.setFoodLevel(20);p.setSaturation(20);p.setHealth(p.getMaxHealth());p.setTotalExperience(0);p.setLevel(0);p.setExp(0);}
    private void prepareLobby(Player p){reset(p);teleport(p,lobbyPoint());p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(false);var compass=new ItemStack(Material.COMPASS);var meta=compass.getItemMeta();meta.setDisplayName("§dRenk Kaçışı §7• Odalar");meta.getPersistentDataContainer().set(menuKey,PersistentDataType.BYTE,(byte)1);compass.setItemMeta(meta);p.getInventory().setItem(8,compass);}
    private List<Player> party(Player leader){ready(leader);Session own=sessions.get(leader.getUniqueId());List<Player> team=own.members.stream().filter(id->{Session s=sessions.get(id);return s!=null&&s.party.equals(own.party);}).map(Bukkit::getPlayer).toList();if(team.isEmpty()||team.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Gruptaki biri çevrimdışı.");if(team.size()>1&&!team.getFirst().getUniqueId().equals(leader.getUniqueId()))throw new IllegalArgumentException("Odayı grup lideri seçmeli.");for(Player p:team)if(membership.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Gruptaki herkes önce /renk lobi ile odasından çıksın.");return team;}
    private void queue(Player p,Integer selected){List<Player> team=party(p);Room room=selected==null?rooms.values().stream().filter(r->r.phase==Phase.WAITING&&r.members.size()+team.size()<=8).findFirst().orElse(null):rooms.get(selected);
        if(room==null){int id=selected==null?java.util.stream.IntStream.rangeClosed(1,3).filter(i->!rooms.containsKey(i)).findFirst().orElse(0):selected;if(id<1||id>3)throw new IllegalArgumentException("Üç oda da dolu.");room=new Room(id);rooms.put(id,room);}
        if(room.phase!=Phase.WAITING||room.members.size()+team.size()>8)throw new IllegalArgumentException("Oda dolu veya oynanıyor.");for(Player member:team){room.members.add(member.getUniqueId());membership.put(member.getUniqueId(),room);screens.clear(member.getUniqueId());member.closeDialog();tell(member,"Oda "+room.id+" • "+room.members.size()+"/8 oyuncu");}room.announce("En az 2 kişide geri sayım başlar.");}
    private void gameLobby(Player p){ready(p);Room room=membership.remove(p.getUniqueId());if(room!=null)room.remove(p.getUniqueId());prepareLobby(p);menu(p);}
    private void menu(Player p){ready(p);Room own=membership.get(p.getUniqueId());var buttons=new ArrayList<GameScreens.Button>();var lines=new ArrayList<>(List.of("Hedef renge yetiş! Diğer zeminler düşer. İki hata hakkın var.","Hedef renk yazıyla ve eldeki blokla gösterilir. Turlar hızlanır."));boolean safe=own==null||own.phase==Phase.WAITING;
        if(own==null){buttons.add(new GameScreens.Button("Oyna",()->queue(p,null)));for(int id=1;id<=3;id++){final int n=id;Room room=rooms.get(id);lines.add("Oda "+id+" • "+(room==null?"Boş":room.members.size()+"/8 • "+room.phase));buttons.add(new GameScreens.Button("Oda "+id,()->queue(p,n)));}}
        else{lines.add("Odan: "+own.id+" • "+own.phase);if(own.phase==Phase.WAITING&&own.members.iterator().next().equals(p.getUniqueId()))buttons.add(new GameScreens.Button("Maçı başlat",()->start(p,false)));buttons.add(new GameScreens.Button("Renk lobisi",()->gameLobby(p)));}
        buttons.add(new GameScreens.Button("Nasıl oynanır?",()->screens.show(p,"Renk Kaçışı • Kurallar",List.of("Ortaya çıkan renk adına bak, aynı renk zemine koş.","Süre bitince diğer renkler kaybolur. Düşersen bir hakkın gider; sonraki tur geri dönersin.","İkinci hatada seyirci olursun. Son kalan kazanır; 15 tur sonunda kalanlar hak ve başarılı tur sayısıyla sıralanır.","PvP, blok koyma/kırma ve uçuş kapalıdır. /renk lobi oda bırakır; /lobi ana spawna götürür."),List.of(new GameScreens.Button("Odalar",()->menu(p))),false)));
        buttons.add(new GameScreens.Button("Lobi",()->hub.returnToLobby(p.getUniqueId())));screens.show(p,"Renk Kaçışı • Odalar",lines,buttons,safe);
    }
    private void start(Player p,boolean practice){Room room=membership.get(p.getUniqueId());if(room==null||room.phase!=Phase.WAITING)throw new IllegalArgumentException("Önce bir bekleme odası seç.");if(!room.members.iterator().next().equals(p.getUniqueId())&&!p.hasPermission("kasaba.renk.admin"))throw new IllegalArgumentException("Odayı sahibi başlatabilir.");if(practice&&!p.hasPermission("kasaba.renk.admin"))throw new IllegalArgumentException("Tek kişilik deneme yöneticiye aittir.");if(room.members.size()<2&&!practice)throw new IllegalArgumentException("En az iki oyuncu gerekli.");room.start(practice);}
    private void tick(){long now=System.currentTimeMillis();for(Room room:List.copyOf(rooms.values()))try{room.tick(now);}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Renk odası durduruluyor.",e);finish(room,"Oda hatası; lobiye dönüldü.");}}
    private void finish(Room room,String result){if(rooms.remove(room.id)!=room)return;room.announce(result);room.bar.removeAll();for(UUID id:List.copyOf(room.members)){membership.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null&&sessions.containsKey(id)&&!stopping)try{prepareLobby(p);}catch(RuntimeException e){hub.returnToLobby(id);}}room.build();}
    private void buildLobby(){for(int x=-9;x<=9;x++)for(int z=-58;z<=-39;z++){world.getBlockAt(x,80,z).setType(Math.abs(x)==9||z==-58||z==-39?BLOCKS[Math.floorMod(x+z,8)]:Material.SMOOTH_QUARTZ,false);for(int y=81;y<=85;y++)world.getBlockAt(x,y,z).setType(Material.AIR,false);}for(int x:new int[]{-8,8})for(int z:new int[]{-57,-40}){for(int y=81;y<=83;y++)world.getBlockAt(x,y,z).setType(Material.QUARTZ_PILLAR,false);world.getBlockAt(x,84,z).setType(Material.SEA_LANTERN,false);}world.setSpawnLocation(lobbyPoint());}
    final class Room {
        final int id;final LinkedHashSet<UUID> members=new LinkedHashSet<>();final Map<UUID,Integer> lives=new LinkedHashMap<>(),passes=new LinkedHashMap<>();final Set<UUID> failed=new HashSet<>();final Random random=new Random();final BossBar bar=Bukkit.createBossBar("§dRenk Kaçışı",BarColor.PINK,BarStyle.SOLID);
        Phase phase=Phase.WAITING;int round,target;long deadline;boolean practice;int[] cells=RoundRules.layout(random);
        Room(int id){this.id=id;}int origin(){return (id-1)*128-16;}Location observer(){return new Location(world,origin()+39.5,86,.5,90,25);}
        void build(){for(int x=0;x<32;x++)for(int z=0;z<32;z++){world.getBlockAt(origin()+x,80,z-16).setType(BLOCKS[RoundRules.colorAt(cells,x,z)],false);for(int y=81;y<=84;y++)world.getBlockAt(origin()+x,y,z-16).setType(Material.AIR,false);}for(int x=origin()+37;x<=origin()+41;x++)for(int z=-2;z<=2;z++)world.getBlockAt(x,85,z).setType(Material.GLASS,false);}
        void start(boolean practice){this.practice=practice;for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p==null||!auth(p))throw new IllegalArgumentException("Oyuncu ayrıldı.");lives.put(id,2);passes.put(id,0);reset(p);bar.addPlayer(p);}nextRound();}
        void nextRound(){round++;phase=Phase.WARNING;failed.clear();cells=RoundRules.layout(random);target=random.nextInt(8);build();deadline=System.currentTimeMillis()+RoundRules.warningMillis(round);int seat=0;
            for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p==null)continue;if(lives.getOrDefault(id,0)>0){p.setGameMode(GameMode.ADVENTURE);p.setGravity(true);p.setFlying(false);p.setAllowFlight(false);teleport(p,new Location(world,origin()+4.5+(seat%4)*7,81,-11.5+(seat/4)*20));p.getInventory().clear();var item=new ItemStack(BLOCKS[target]);var meta=item.getItemMeta();meta.setDisplayName(NAMES[target]+" §7• Bu renge koş!");item.setItemMeta(meta);p.getInventory().setItem(4,item);p.getInventory().setHeldItemSlot(4);p.sendTitle(NAMES[target],"§fTur "+round+" • §c"+lives.get(id)+" hak",2,35,5);p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_PLING,.8f,1.2f);seat++;}}
        }
        boolean safe(Player p){return p.getWorld()==world&&p.getY()>=79.8&&p.getY()<84&&RoundRules.colorAt(cells,p.getLocation().getBlockX()-origin(),p.getLocation().getBlockZ()+16)==target;}
        void miss(Player p){UUID id=p.getUniqueId();if(!failed.add(id))return;int left=lives.compute(id,(k,v)->Math.max(0,v-1));p.setGameMode(left==0?GameMode.SPECTATOR:GameMode.ADVENTURE);p.setGravity(false);teleport(p,observer());p.sendTitle(left==0?"§cELENDİN":"§eSON HAKKIN",left==0?"§7Diğerlerini izleyebilirsin":"§fSonraki tur geri dönüyorsun",2,35,5);p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BASS,1,.7f);}
        void tick(long now){
            if(phase==Phase.WAITING){if(members.size()<2){deadline=0;return;}if(deadline==0){deadline=now+Math.clamp(getConfig().getInt("countdown-seconds",10),3,60)*1000L;announce("Maç birazdan başlıyor.");}if(now>=deadline)start(false);return;}
            for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p==null)continue;if(lives.getOrDefault(id,0)>0&&!failed.contains(id)){p.setFlying(false);p.setAllowFlight(false);if(p.getY()<76||p.getWorld()!=world||Math.abs(p.getX()-(origin()+16))>20||Math.abs(p.getZ())>20)miss(p);}else if(p.getLocation().distanceSquared(observer())>2500)teleport(p,observer());}
            if(phase==Phase.WARNING){bar.setTitle("§fTur "+round+" §7• "+NAMES[target]+" §7• "+String.format(Locale.ROOT,"%.1f sn",Math.max(0,deadline-now)/1000.0));bar.setProgress(Math.clamp((deadline-now)/(double)RoundRules.warningMillis(round),0,1));if(now>=deadline){phase=Phase.DROP;deadline=now+3000;for(int x=0;x<32;x++)for(int z=0;z<32;z++)if(RoundRules.colorAt(cells,x,z)!=target)world.getBlockAt(origin()+x,80,z-16).setType(Material.AIR,false);for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null)p.playSound(p.getLocation(),Sound.BLOCK_GLASS_BREAK,.6f,.8f);}}}
            else if(now>=deadline){for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null&&lives.getOrDefault(id,0)>0&&!failed.contains(id)){if(!safe(p))miss(p);else passes.merge(id,1,Integer::sum);}}List<UUID> alive=lives.entrySet().stream().filter(e->e.getValue()>0).map(Map.Entry::getKey).toList();if(alive.isEmpty())finish(this,"§eHerkes elendi. Berabere!");else if(!practice&&alive.size()==1)finish(this,"§6Kazanan: "+name(alive.getFirst())+" • "+round+" tur");else if(round>=Math.clamp(getConfig().getInt("max-rounds",15),3,30)){int best=alive.stream().mapToInt(id->lives.get(id)*100+passes.getOrDefault(id,0)).max().orElse(0);List<String> winners=alive.stream().filter(id->lives.get(id)*100+passes.getOrDefault(id,0)==best).map(this::name).toList();finish(this,"§6Sonuç: "+String.join(", ",winners)+(winners.size()>1?" • Berabere":" kazandı!"));}else nextRound();}
            for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null)p.sendActionBar(net.kyori.adventure.text.Component.text("Tur "+round+" • Hak "+lives.getOrDefault(id,0)+"/2 • Başarılı tur "+passes.getOrDefault(id,0)));}
        }
        void remove(UUID id){members.remove(id);lives.remove(id);passes.remove(id);failed.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null)bar.removePlayer(p);if(members.isEmpty())finish(this,"Oda boşaldı.");}
        void announce(String text){for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null)tell(p,"Oda "+this.id+" • "+text);}}String name(UUID id){Player p=Bukkit.getPlayer(id);return p==null?id.toString().substring(0,8):p.getName();}
    }
    private static String norm(String s){return s.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){if(!(sender instanceof Player p)){tell(sender,"Bu komut oyun içindir.");return true;}try{if(!enter(p))return true;String action=args.length==0?"odalar":norm(args[0]);switch(action){case "odalar","yardim"->menu(p);case "oyna"->queue(p,null);case "oda"->{if(args.length!=2)throw new IllegalArgumentException("/renk oda 1|2|3");queue(p,Integer.parseInt(args[1]));}case "baslat"->start(p,false);case "deneme"->start(p,true);case "lobi"->gameLobby(p);case "cik"->hub.returnToLobby(p.getUniqueId());default->tell(p,"/renk • oyna • oda 1|2|3 • başlat • lobi • çık");}}catch(RuntimeException e){tell(p,e.getMessage()==null?"İşlem yapılamadı.":e.getMessage());}return true;}
    @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){return a.length==1?List.of("odalar","oyna","oda","başlat","lobi","çık","yardım").stream().filter(x->norm(x).startsWith(norm(a[0]))).toList():a.length==2&&a[0].equals("oda")?List.of("1","2","3"):List.of();}
    private void recover(Player p){if(!p.isOnline()||!auth(p)||sessions.containsKey(p.getUniqueId()))return;try{if(PlayerBackup.restore(getDataFolder(),p,true))tell(p,"Önceki oyun oturumundaki eşyaların geri verildi.");}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Renk kurtarma kaydı korundu.",e);p.kick(net.kyori.adventure.text.Component.text("Renk Kaçışı kurtarma kaydın okunamadı. Yöneticiye haber ver."));}}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(PlayerAuthenticatedEvent e){recover(e.getPlayer());}
    @EventHandler public void multiverse(org.bukkit.event.server.PluginEnableEvent e){if(e.getPlugin().getName().equals("Multiverse-Core"))Bukkit.getScheduler().runTask(this,()->new MultiverseBridge(this).register(world,lobbyPoint()));}
    @EventHandler public void joined(PlayerJoinEvent e){quitting.remove(e.getPlayer().getUniqueId());Bukkit.getScheduler().runTask(this,()->recover(e.getPlayer()));}
    @EventHandler(priority=EventPriority.LOWEST) public void quit(PlayerQuitEvent e){quitting.add(e.getPlayer().getUniqueId());leave(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId())&&!moving.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void use(PlayerInteractEvent e){if(e.getHand()==EquipmentSlot.HAND&&e.getItem()!=null&&e.getItem().hasItemMeta()&&e.getItem().getItemMeta().getPersistentDataContainer().has(menuKey)){e.setCancelled(true);if(e.getAction().name().startsWith("RIGHT_CLICK")&&sessions.containsKey(e.getPlayer().getUniqueId())&&auth(e.getPlayer()))menu(e.getPlayer());return;}if(e.getPlayer().getWorld()==world)e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void breaking(BlockBreakEvent e){if(e.getBlock().getWorld()==world)e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void placing(BlockPlaceEvent e){if(e.getBlock().getWorld()==world)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(e.getEntity().getWorld()==world&&e.getEntity() instanceof Player)e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void drop(PlayerDropItemEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(sessions.containsKey(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void click(InventoryClickEvent e){if(sessions.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void drag(InventoryDragEvent e){if(sessions.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void hunger(FoodLevelChangeEvent e){if(sessions.containsKey(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void flight(PlayerToggleFlightEvent e){Room room=membership.get(e.getPlayer().getUniqueId());if(room!=null&&room.phase!=Phase.WAITING&&room.lives.getOrDefault(e.getPlayer().getUniqueId(),0)>0)e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void spawn(EntitySpawnEvent e){if(e.getEntity().getWorld()==world&&(e.getEntity() instanceof Mob||e.getEntity() instanceof Projectile||e.getEntity() instanceof TNTPrimed))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void explode(EntityExplodeEvent e){if(e.getLocation().getWorld()==world)e.setCancelled(true);}
}
