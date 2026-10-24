package tr.kasabatr.ispanya;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import org.bukkit.*;
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

public final class KasabaIspanyaPlugin extends JavaPlugin implements HubGame,Listener,TabExecutor {
    private record Session(UUID party,List<UUID> members){}
    final Map<Integer,GameRoom> rooms=new TreeMap<>();final Map<UUID,GameRoom> membership=new HashMap<>();
    private final Map<UUID,Session> sessions=new HashMap<>();private final Set<UUID> teleporting=new HashSet<>(),editors=new HashSet<>(),quitting=new HashSet<>();
    private int nextRoom=101;private NamespacedKey diceKey,compassKey,shortcutKey;private WorldInstances instances;private ArenaTrack track;private HubApi hub;private GameScreens screens;private boolean disabling;
    @Override public void onEnable(){
        saveDefaultConfig();migrate();if(!new File(getDataFolder(),"track.yml").isFile())saveResource("track.yml",false);
        diceKey=new NamespacedKey(this,"dice");compassKey=new NamespacedKey(this,"menu");shortcutKey=new NamespacedKey(this,"shortcut");screens=new GameScreens(this);
        try{track=new ArenaTrack(new File(getDataFolder(),"track.yml"));instances=new WorldInstances(this);instances.startup();hub=Bukkit.getServicesManager().load(HubApi.class);if(hub==null)throw new IOException("KasabaHub API bulunamadı.");hub.register(this,this);}
        catch(Exception e){getLogger().log(Level.SEVERE,"Şans Yolu oyunu açılamadı; mevcut dünya ve veriler korunuyor.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        Objects.requireNonNull(getCommand("ispanya")).setTabCompleter(this);Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getScheduler().runTaskTimer(this,()->{for(GameRoom room:List.copyOf(rooms.values()))try{room.tick();}catch(RuntimeException e){getLogger().log(Level.SEVERE,"Şans Yolu odası durduruldu.",e);finish(room,"Oda hatası nedeniyle lobiye dönüldü.",false);}},20,20);
        Bukkit.getScheduler().runTaskTimer(this,this::rescueLobby,2,2);Bukkit.getOnlinePlayers().forEach(this::recover);getLogger().info("Şans Yolu 0.4.0 hazır: /oyunlar → Şans Yolu; ortak lobi ve 3 eşzamanlı 2–4 kişilik oda.");
    }
    private void migrate(){
        if(getConfig().getInt("color-effects-version",0)<3){for(String kind:List.of("red","green","yellow","purple"))if(getConfig().getString("tile-effects."+kind,"none").equals("none"))getConfig().set("tile-effects."+kind,switch(kind){case "red"->"bad_luck";case "green"->"advance:3";case "yellow"->"shortcut";default->"random";});if(getConfig().getString("tile-effects.red","").equals("back:3"))getConfig().set("tile-effects.red","bad_luck");getConfig().set("color-effects-version",3);saveConfig();}
    }
    @Override public void onDisable(){disabling=true;var worlds=rooms.values().stream().map(r->r.world).filter(Objects::nonNull).toList();for(GameRoom room:List.copyOf(rooms.values()))finish(room,"Eklenti kapanıyor.",false);if(hub!=null)try{hub.unregister(this,"ispanya");}catch(RuntimeException e){getLogger().warning(e.getMessage());}for(UUID id:List.copyOf(sessions.keySet()))leave(id);if(instances!=null)worlds.forEach(instances::close);}
    @Override public ChunkGenerator getDefaultWorldGenerator(String name,String id){return new EmptyWorldGenerator();}
    @Override public GameInfo info(){return new GameInfo("ispanya","Şans Yolu","Zar yarışı • 2–4 kişilik odalar • 3 eşzamanlı maç",1,4);}
    @Override public boolean available(){return instances!=null&&instances.templateReady()&&!disabling;}
    @Override public boolean lobby(UUID id){GameRoom room=membership.get(id);return sessions.containsKey(id)&&(room==null||!room.active&&!room.closing);}
    @Override public boolean penaltyEligible(UUID id){GameRoom r=membership.get(id);return !disabling&&r!=null&&r.active&&!r.closing&&!r.ending&&!r.finished.contains(id);}
    @Override public boolean containsMatch(UUID id,Location at){GameRoom r=membership.get(id);return r==null||at.getWorld()==r.world;}
    boolean auth(Player p){AuthState state=Bukkit.getServicesManager().load(AuthState.class);return state!=null&&state.authenticated(p.getUniqueId());}
    void ready(Player p){if(!auth(p))throw new IllegalArgumentException("Önce hesabına giriş yap.");if(!sessions.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Önce /oyunlar üzerinden Şans Yolu'na gir.");}
    int minimum(){return Math.clamp(getConfig().getInt("min-players",2),2,4);}int maximum(){return Math.clamp(getConfig().getInt("max-players",4),minimum(),4);}
    WorldInstances instances(){return instances;}ArenaTrack track(){return track;}
    @Override public boolean join(List<Player> team){
        if(!available()||team.isEmpty()||team.size()>maximum()||new HashSet<>(team).size()!=team.size())return false;
        for(Player p:team)if(!p.isOnline()||!auth(p)||sessions.containsKey(p.getUniqueId())||PlayerBackup.exists(getDataFolder(),p.getUniqueId()))return false;
        UUID party=UUID.randomUUID();List<UUID> ids=team.stream().map(Player::getUniqueId).toList();var prepared=new ArrayList<Player>();
        try{
            for(Player p:team){p.closeInventory();PlayerBackup.save(getDataFolder(),p);prepared.add(p);sessions.put(p.getUniqueId(),new Session(party,ids));prepare(p,instances.lobby(),false);}
            for(Player p:team)Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&lobby(p.getUniqueId()))menu(p);},2);return true;
        }catch(Exception e){for(Player p:prepared)leave(p.getUniqueId());getLogger().log(Level.WARNING,"İspanya grup girişi geri alındı.",e);return false;}
    }
    @Override public void leave(UUID id){hub.abandon(id);
        Session session=sessions.remove(id);if(screens!=null)screens.clear(id);editors.remove(id);GameRoom room=membership.remove(id);if(room!=null&&!room.closing)room.remove(id);
        Player p=Bukkit.getPlayer(id);if(p!=null&&room!=null)room.restoreBoard(p);
        if(session!=null&&p!=null&&!quitting.contains(id))try{PlayerBackup.restore(getDataFolder(),p,!hub.transferring(id));}catch(RuntimeException e){getLogger().log(Level.SEVERE,"İspanya eşya yedeği korunuyor: "+id,e);p.kick(net.kyori.adventure.text.Component.text("İspanya eşya yedeğin kurtarılamadı. Yöneticiye haber ver."));}
    }
    private boolean enter(Player p){if(!auth(p))throw new IllegalArgumentException("Önce hesabına giriş yap.");if(sessions.containsKey(p.getUniqueId()))return true;if(hub.playing(p.getUniqueId()))throw new IllegalArgumentException("Önce /lobi ile mevcut oyundan çık.");Bukkit.dispatchCommand(p,"oyunlar git ispanya");return sessions.containsKey(p.getUniqueId());}
    void teleport(Player p,Location point){if(p==null)throw new IllegalArgumentException("Oyuncu çevrimdışı.");teleporting.add(p.getUniqueId());try{if(!p.teleport(point,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Işınlanma başka bir eklenti tarafından engellendi.");p.setFallDistance(0);}finally{teleporting.remove(p.getUniqueId());}}
    void prepare(Player p,Location point,boolean match){
        if(NetworkFeatures.get()!=null)NetworkFeatures.get().beforeGame(p);screens.clear(p.getUniqueId());p.closeDialog();p.closeInventory();p.setItemOnCursor(null);p.getInventory().clear();
        for(var effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());p.setAbsorptionAmount(0);p.setFireTicks(0);p.setInvulnerable(false);p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(match);p.setGravity(!match);p.setFoodLevel(20);p.setSaturation(20);p.setHealth(p.getMaxHealth());p.setTotalExperience(0);p.setLevel(0);p.setExp(0);teleport(p,point);
        // Multiverse may apply its world mode during the teleport; the game owns its final mode.
        p.setGameMode(GameMode.ADVENTURE);p.setFlying(false);p.setAllowFlight(match);
        if(match){p.getInventory().setItem(4,item(Material.NAUTILUS_SHELL,"§e§lİki Zar At",diceKey));p.getInventory().setHeldItemSlot(4);}else p.getInventory().setItem(8,item(Material.COMPASS,"§6Şans Yolu §7• Odalar",compassKey));
    }
    private ItemStack item(Material type,String name,NamespacedKey key){var item=new ItemStack(type);var meta=item.getItemMeta();meta.setDisplayName(name);meta.setLore(List.of("§7Sağ tıkla • /sansyolu"));meta.getPersistentDataContainer().set(key,PersistentDataType.BYTE,(byte)1);item.setItemMeta(meta);return item;}
    private List<Player> party(Player leader){
        ready(leader);Session own=sessions.get(leader.getUniqueId());List<Player> players=own.members.stream().filter(id->{Session s=sessions.get(id);return s!=null&&s.party.equals(own.party);}).map(Bukkit::getPlayer).toList();
        if(players.stream().anyMatch(Objects::isNull))throw new IllegalArgumentException("Gruptaki biri çevrimdışı.");for(Player p:players){ready(p);if(membership.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Gruptaki herkes önce /sansyolu lobi ile odasından çıksın.");}
        if(players.size()>1&&!players.getFirst().getUniqueId().equals(leader.getUniqueId()))throw new IllegalArgumentException("Odayı grup lideri seçmeli.");return players;
    }
    private void queue(Player p,Integer roomId,boolean create){
        List<Player> team=party(p);for(Player member:team)hub.requireGameEntry(member.getUniqueId(),"ispanya");GameRoom room=null;
        if(roomId!=null){room=rooms.get(roomId);if(room==null||room.active||room.closing||room.members.size()+team.size()>maximum())throw new IllegalArgumentException("Oda dolu, başlamış veya kapanmış.");}
        else if(!create&&getConfig().getBoolean("auto-join-open-room",true))room=rooms.values().stream().filter(r->!r.active&&!r.closing&&r.members.size()+team.size()<=maximum()).findFirst().orElse(null);
        if(room==null){if(rooms.size()>=Math.clamp(getConfig().getInt("max-rooms",3),1,10))throw new IllegalArgumentException("Bütün odalar dolu; biraz sonra tekrar dene.");room=new GameRoom(this,nextRoom++,p.getUniqueId());rooms.put(room.id,room);}
        for(Player member:team){room.members.add(member.getUniqueId());membership.put(member.getUniqueId(),room);screens.clear(member.getUniqueId());member.closeDialog();room.bar.addPlayer(member);tell(member,"Oda "+room.id+" • "+room.members.size()+"/"+maximum()+" kişi. En az "+minimum()+" oyuncuda geri sayım başlar.");}for(Player member:team)menu(member);
    }
    private void gameLobby(Player p){ready(p);hub.abandon(p.getUniqueId());GameRoom room=membership.remove(p.getUniqueId());if(room!=null){room.remove(p.getUniqueId());room.restoreBoard(p);}prepare(p,instances.lobby(),false);}
    void inactiveLobby(Player p){gameLobby(p);tell(p,"Maçta 3 kez zar atmadığın için Şans Yolu lobisine döndün.");}
    void giveShortcut(Player p){p.getInventory().setItem(0,item(Material.BLAZE_ROD,"§eKestirme çubuğu • Sağ tıkla",shortcutKey));p.getInventory().setHeldItemSlot(0);}
    void removeShortcut(Player p){for(int slot=0;slot<p.getInventory().getSize();slot++){ItemStack i=p.getInventory().getItem(slot);if(i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(shortcutKey))p.getInventory().setItem(slot,null);}p.getInventory().setHeldItemSlot(4);}
    private void stay(Player p){ready(p);GameRoom room=membership.get(p.getUniqueId());if(room!=null&&!room.active)gameLobby(p);screens.clear(p.getUniqueId());p.closeDialog();}
    void finish(GameRoom room,String reason,boolean reward){
        if(room.closing)return;room.closing=true;room.stopAnimation();rooms.remove(room.id);String ranking=room.ranking();room.announce(reason+(room.active?" Sıralama: "+ranking:""));
        if(reward&&room.active&&!disabling){List<UUID> order=room.orderedFinish();for(int i=0;i<order.size();i++){Player p=Bukkit.getPlayer(order.get(i));if(p==null)continue;for(String command:getConfig().getStringList("reward-commands"))if(!command.isBlank())try{Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command.replace("{player}",p.getName()).replace("{rank}",String.valueOf(i+1)).replace("{points}",String.valueOf(room.points.getOrDefault(p.getUniqueId(),0))));}catch(RuntimeException e){getLogger().log(Level.WARNING,"Ödül komutu çalışmadı.",e);}}}
        room.bar.removeAll();for(UUID id:List.copyOf(room.members)){membership.remove(id);Player p=Bukkit.getPlayer(id);if(p==null)continue;room.restoreBoard(p);if(!disabling&&sessions.containsKey(id))try{prepare(p,instances.lobby(),false);tell(p,"Tekrar oynamak için pusulayı kullan. Ana spawn: /lobi");}catch(RuntimeException e){getLogger().log(Level.WARNING,"Şans Yolu lobisine dönüş engellendi.",e);hub.returnToLobby(id);}}
        // A departing final player is restored by leave() after room.remove() returns.
        if(room.world!=null&&!disabling)Bukkit.getScheduler().runTask(this,()->instances.close(room.world));
    }
    private void menu(Player p){
        ready(p);GameRoom own=membership.get(p.getUniqueId());var lines=new ArrayList<String>();var buttons=new ArrayList<GameScreens.Button>();boolean safe=own==null||!own.active;
        lines.add("İki zar at, kare kare ilerle ve bitişe ulaş. Odalar 2–4 kişiliktir.");
        if(own!=null){lines.add("Odan: "+own.id+" • "+own.members.size()+"/"+maximum()+" • "+(own.active?"Oynanıyor":"Oyuncu bekleniyor"));if(own.active){buttons.add(new GameScreens.Button("Zar at",()->{if(!own.roll(p.getUniqueId(),false))tell(p,"Zar sırası sende değil.");}));}else if(own.owner.equals(p.getUniqueId())||p.hasPermission("kasabatr.ispanya.admin"))buttons.add(new GameScreens.Button("Maçı başlat",()->{try{own.start();}catch(IOException e){tell(p,e.getMessage());}}));buttons.add(new GameScreens.Button("Şans Yolu lobisi",()->gameLobby(p)));}
        else{buttons.add(new GameScreens.Button("Oyna",()->queue(p,null,false)));buttons.add(new GameScreens.Button("Oda oluştur",()->queue(p,null,true)));for(GameRoom room:rooms.values()){lines.add("Oda "+room.id+" • "+room.members.size()+"/"+maximum()+" • "+(room.active?"Oynanıyor":"Bekliyor"));if(!room.active)buttons.add(new GameScreens.Button("Oda "+room.id,()->queue(p,room.id,false)));}}
        if(safe)buttons.add(new GameScreens.Button("Oyun lobileri",()->hub.openGames(p.getUniqueId())));buttons.add(new GameScreens.Button("Nasıl oynanır?",()->help(p)));buttons.add(new GameScreens.Button(safe?"Bu lobide kal":"Oyuna dön",()->stay(p)));buttons.add(new GameScreens.Button("Ana spawna dön",()->hub.returnToLobby(p.getUniqueId())));screens.show(p,"Şans Yolu • Odalar",lines,buttons,safe);
    }
    private void help(Player p){screens.show(p,"Şans Yolu • Nasıl oynanır?",List.of("Sıran geldiğinde iki zar atarsın; toplamı kadar otomatik ilerlersin. Zar atmazsan sıra geçer; maçta 3 kez atlamada oyun lobisine dönersin.","Kırmızı: bir tur bekle veya 3 kare geri. Yeşil: 3 kare ileri.","Sarı: kestirme çubuğunu sağ tıklayarak köprüden geç. Mor: rastgele 1–6 kare ileri veya geri. Siyah: etkisiz.","Renkler ve üç kestirme her maç yeniden yerleştirilir. Kötü şans etkisi açıklanır, sonra hareket edilir.","Son kareye ulaşanlar sırayla bitirir; diğer oyuncular devam eder. Süre dolunca bitirenler, ardından ilerleme ve puana göre sıralama açıklanır.","/sansyolu zar • /sansyolu lobi: oda bırak • /lobi: ana spawn"),List.of(new GameScreens.Button("Odalar",()->menu(p))),false);}
    void tell(CommandSender sender,String text){sender.sendMessage(tr.kasaba.hub.api.MenuStyle.title("[Şans Yolu] ").append(tr.kasaba.hub.api.MenuStyle.line(text)));}
    static String normalize(String text){return text.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p)){tell(sender,"Bu komut oyun içindir.");return true;}
        try{
            if(!auth(p))throw new IllegalArgumentException("Önce hesabına giriş yap.");String action=args.length==0?"odalar":normalize(args[0]);
            if(action.equals("bilgi")){tell(p,"Dünya: "+instances.publicWorld.getName()+" • "+track.size()+" kare • Odalar: "+rooms.size()+"/"+getConfig().getInt("max-rooms",3));return true;}
            if(action.equals("duzenleme")){admin(p);if(p.getWorld()!=instances.publicWorld)throw new IllegalArgumentException("Düzenleme yalnız ortak İspanya dünyasında açılır.");if(args.length>1&&normalize(args[1]).equals("ac"))editors.add(p.getUniqueId());else editors.remove(p.getUniqueId());tell(p,"İspanya düzenleme: "+(editors.contains(p.getUniqueId())?"açık":"kapalı"));return true;}
            if(action.equals("lobi")&&args.length>1&&normalize(args[1]).equals("ayarla")){admin(p);if(p.getWorld()!=instances.publicWorld||!WorldInstances.safe(p.getLocation()))throw new IllegalArgumentException("İspanya dünyasında güvenli zeminde dur.");Location l=p.getLocation();getConfig().set("lobby.x",l.getX());getConfig().set("lobby.y",l.getY());getConfig().set("lobby.z",l.getZ());getConfig().set("lobby.yaw",l.getYaw());getConfig().set("lobby.pitch",l.getPitch());saveConfig();instances.publicWorld.setSpawnLocation(l);new MultiverseBridge(this).register(instances.publicWorld,l);tell(p,"İspanya lobi noktası ayarlandı.");return true;}
            if(!enter(p))return true;GameRoom own=membership.get(p.getUniqueId());
            switch(action){case "odalar","menu"->menu(p);case "yardim"->help(p);case "oyna"->queue(p,null,false);case "olustur"->queue(p,null,true);case "katil"->{if(args.length!=2)throw new IllegalArgumentException("/sansyolu katıl <oda>");queue(p,Integer.parseInt(args[1]),false);}case "baslat"->{if(own==null||!own.owner.equals(p.getUniqueId())&&!p.hasPermission("kasabatr.ispanya.admin"))throw new IllegalArgumentException("Odayı sahibi başlatabilir.");own.start();}case "kestirme"->{if(own==null||!own.useShortcut(p.getUniqueId()))tell(p,"Henüz kullanılacak kestirmen yok.");}case "zar"->{if(own==null||!own.roll(p.getUniqueId(),false))tell(p,"Şu anda zar sırası sende değil.");}case "lobi"->gameLobby(p);case "cik"->hub.returnToLobby(p.getUniqueId());case "bitir"->{admin(p);if(args.length!=2)throw new IllegalArgumentException("/sansyolu bitir <oda>");GameRoom room=rooms.get(Integer.parseInt(args[1]));if(room==null)throw new IllegalArgumentException("Oda bulunamadı.");finish(room,"Yönetici maçı durdurdu.",false);}default->tell(p,"/sansyolu • oluştur • katıl <oda> • başlat • zar • lobi • çık • yardım");}
        }catch(NumberFormatException e){tell(p,"Oda numarası sayı olmalı.");}catch(IOException|IllegalArgumentException e){tell(p,e.getMessage());}return true;
    }
    private void admin(Player p){if(!p.hasPermission("kasabatr.ispanya.admin"))throw new IllegalArgumentException("Yönetim yetkin yok.");}
    @Override public List<String> onTabComplete(CommandSender sender,Command c,String label,String[] args){if(args.length==1)return List.of("odalar","oyna","oluştur","katıl","başlat","zar","lobi","çık","yardım","bilgi").stream().filter(s->normalize(s).startsWith(normalize(args[0]))).toList();if(args.length==2&&normalize(args[0]).equals("katil"))return rooms.values().stream().filter(r->!r.active).map(r->String.valueOf(r.id)).toList();return List.of();}
    void recover(Player p){if(!p.isOnline()||!auth(p)||sessions.containsKey(p.getUniqueId()))return;try{if(PlayerBackup.restore(getDataFolder(),p,true))tell(p,"Önceki İspanya oturumundaki eşyaların ve modun geri yüklendi.");}catch(RuntimeException e){getLogger().log(Level.SEVERE,"İspanya kurtarma yedeği korundu.",e);p.kick(net.kyori.adventure.text.Component.text("İspanya eşya yedeğin okunamadı. Yöneticiye haber ver."));}}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(PlayerAuthenticatedEvent e){recover(e.getPlayer());}
    @EventHandler public void multiverse(org.bukkit.event.server.PluginEnableEvent e){if(e.getPlugin().getName().equals("Multiverse-Core"))Bukkit.getScheduler().runTask(this,()->new MultiverseBridge(this).register(instances.publicWorld,instances.lobby()));}
    @EventHandler public void joined(PlayerJoinEvent e){quitting.remove(e.getPlayer().getUniqueId());Bukkit.getScheduler().runTask(this,()->recover(e.getPlayer()));}
    @EventHandler(priority=EventPriority.LOWEST) public void quit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();quitting.add(id);leave(id);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){Player p=e.getPlayer();ItemStack item=e.getItem();if(e.getHand()==EquipmentSlot.HAND&&(e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK)&&item!=null&&item.hasItemMeta()){
        if(item.getItemMeta().getPersistentDataContainer().has(compassKey)){e.setCancelled(true);if(sessions.containsKey(p.getUniqueId())&&auth(p))menu(p);return;}
        if(item.getItemMeta().getPersistentDataContainer().has(shortcutKey)){e.setCancelled(true);GameRoom room=membership.get(p.getUniqueId());if(auth(p)&&room!=null)room.useShortcut(p.getUniqueId());return;}
        if(item.getItemMeta().getPersistentDataContainer().has(diceKey)){e.setCancelled(true);GameRoom room=membership.get(p.getUniqueId());if(auth(p)&&(room==null||!room.roll(p.getUniqueId(),false)))tell(p,"Zar sırası sende değil.");return;}}
        if(instances.managed(p.getWorld())&&!editing(p))e.setCancelled(true);
    }
    private boolean editing(Player p){return p.getWorld()==instances.publicWorld&&editors.contains(p.getUniqueId())&&p.hasPermission("kasabatr.ispanya.admin")&&(!membership.containsKey(p.getUniqueId())||!membership.get(p.getUniqueId()).active);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId())&&!teleporting.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void move(PlayerMoveEvent e){GameRoom room=membership.get(e.getPlayer().getUniqueId());if(room!=null&&room.active&&!teleporting.contains(e.getPlayer().getUniqueId())&&!(e instanceof PlayerTeleportEvent)&&e.hasChangedPosition())e.setCancelled(true);}
    private void rescueLobby(){if(instances==null||instances.publicWorld==null)return;Location safe=instances.lobby();for(Player p:instances.publicWorld.getPlayers())if(auth(p)&&!editors.contains(p.getUniqueId())&&p.getY()<safe.getY()-2){GameRoom r=membership.get(p.getUniqueId());if(r==null||!r.active){teleport(p,safe);p.setVelocity(new org.bukkit.util.Vector());p.setFallDistance(0);}}}
    @EventHandler(ignoreCancelled=true) public void breaking(BlockBreakEvent e){if(instances.managed(e.getBlock().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void placing(BlockPlaceEvent e){if(instances.managed(e.getBlock().getWorld())&&!editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void dropping(PlayerDropItemEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p&&sessions.containsKey(p.getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void click(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p)tr.kasaba.hub.api.MenuClicks.open(this,e,i->i!=null&&i.hasItemMeta()&&i.getItemMeta().getPersistentDataContainer().has(compassKey),()->{if(sessions.containsKey(p.getUniqueId())&&auth(p))menu(p);});if(sessions.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void drag(InventoryDragEvent e){if(sessions.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){if(sessions.containsKey(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void hunger(FoodLevelChangeEvent e){if(sessions.containsKey(e.getEntity().getUniqueId()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(instances.managed(e.getEntity().getWorld())&&e.getEntity() instanceof Player)e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void explosion(EntityExplodeEvent e){if(instances.managed(e.getLocation().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void explosion(BlockExplodeEvent e){if(instances.managed(e.getBlock().getWorld()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void spawn(EntitySpawnEvent e){if(instances.managed(e.getEntity().getWorld())&&(e.getEntity() instanceof Mob||e.getEntity() instanceof Projectile||e.getEntity() instanceof TNTPrimed))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void flight(PlayerToggleFlightEvent e){GameRoom room=membership.get(e.getPlayer().getUniqueId());if(room!=null&&room.active){e.setCancelled(true);e.getPlayer().setFlying(false);}}
}
