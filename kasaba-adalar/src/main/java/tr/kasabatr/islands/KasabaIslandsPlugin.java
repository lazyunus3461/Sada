package tr.kasabatr.islands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import tr.kasabatr.islands.ClaimStore;
import tr.kasabatr.islands.IslandLayout;
import tr.kasabatr.islands.OceanBiomeProvider;
import tr.kasabatr.islands.OceanChunkGenerator;
import tr.kasabatr.islands.PlayerStateStore;

public final class KasabaIslandsPlugin extends JavaPlugin implements Listener,CommandExecutor,TabCompleter {
    private static final String PREFIX="§b[Adalar] §r";
    private final Map<UUID,Long> lastWarning=new HashMap<>();
    private final Set<UUID> relocating=new HashSet<>(),sessions=new HashSet<>();
    private final Map<UUID,Integer> requests=new HashMap<>();
    private ClaimStore claims;private PlayerStateStore playerStates;private String worldName;private IslandLayout layout;
    private tr.kasaba.hub.api.HubApi hub;
    private IslandCreativeMode creativeMode;
    private IslandResetter resetter;
    @Override public void onEnable(){
        saveDefaultConfig();worldName=getConfig().getString("world-name","ada");if("KasabaTR-Adalar-300".equals(worldName)){worldName="ada";getConfig().set("world-name",worldName);saveConfig();}if(worldName==null||!worldName.matches("[A-Za-z0-9_-]{1,64}")){getLogger().severe("world-name geçersiz.");Bukkit.getPluginManager().disablePlugin(this);return;}
        try{claims=new ClaimStore(this);claims.load();playerStates=new PlayerStateStore(this);java.util.Properties marker=new java.util.Properties();java.nio.file.Path file=Bukkit.getWorldContainer().toPath().resolve(worldName).resolve("kasaba-layout.properties");if(java.nio.file.Files.exists(file))try(var input=java.nio.file.Files.newInputStream(file)){marker.load(input);}layout=new IslandLayout(Integer.parseInt(marker.getProperty("spacing","200")));}
        catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada verileri okunamadı; dosyalar korundu.",ex);Bukkit.getPluginManager().disablePlugin(this);return;}
        Objects.requireNonNull(getCommand("ada")).setExecutor(this);getCommand("ada").setTabCompleter(this);creativeMode=new IslandCreativeMode(this,p->islandWorld(p.getWorld())&&authenticated(p));Bukkit.getPluginManager().registerEvents(this,this);if(Bukkit.getPluginManager().isPluginEnabled("KasabaAuth"))Bukkit.getPluginManager().registerEvents(new IslandAuthBridge(this),this);ensureWorld();
        if(Bukkit.getPluginManager().isPluginEnabled("KasabaHub"))try{hub=Bukkit.getServicesManager().load(tr.kasaba.hub.api.HubApi.class);if(hub!=null)hub.register(this,new IslandGame());}catch(RuntimeException ex){getLogger().log(java.util.logging.Level.SEVERE,"Adanı Yap kaydedilemedi. Aynı kimlikle komutla eklenmiş giriş varsa /oyunlar sil adaniyap kullanıp yeniden başlat.",ex);}
        resetter=new IslandResetter(this,claims,layout,()->Bukkit.getWorld(worldName));Bukkit.getPluginManager().registerEvents(new IslandSafety(this,this::islandWorld),this);Bukkit.getScheduler().runTaskTimer(this,resetter::tick,20,1);Bukkit.getScheduler().runTaskTimer(this,resetter::sweep,40,1200);Bukkit.getScheduler().runTaskTimer(this,this::recoverPurchases,40,100);
        getLogger().info(claims.size()+"/300 ada • normal 1 / VIP 2 ada • aralık "+layout.spacing()+" blok.");
    }
    @Override public void onDisable(){if(resetter!=null)resetter.close();if(hub!=null)try{hub.unregister(this,"adaniyap");}catch(RuntimeException ex){getLogger().warning(ex.getMessage());}if(playerStates==null)return;for(Player p:Bukkit.getOnlinePlayers())if(islandWorld(p.getWorld()))try{playerStates.saveIsland(p);}catch(IOException ex){getLogger().severe(ex.getMessage());}}
    private World ensureWorld(){World loaded=Bukkit.getWorld(worldName);if(loaded!=null){configureWorld(loaded);return loaded;}File folder=new File(Bukkit.getWorldContainer(),worldName);if(!new File(folder,"level.dat").isFile())return null;try{World world=Bukkit.createWorld(new WorldCreator(worldName).type(WorldType.FLAT).generator(new OceanChunkGenerator()).biomeProvider(new OceanBiomeProvider()));if(world!=null)configureWorld(world);return world;}catch(RuntimeException ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada dünyası açılamadı.",ex);return null;}}
    private void configureWorld(World world){world.setPVP(false);world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING,false);world.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK,false);world.setGameRule(org.bukkit.GameRule.RANDOM_TICK_SPEED,0);world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE,false);world.setTime(6000);world.setStorm(false);world.setThundering(false);world.setClearWeatherDuration(Integer.MAX_VALUE);}
    @EventHandler(priority=EventPriority.HIGHEST) public void onWeather(org.bukkit.event.weather.WeatherChangeEvent e){if(islandWorld(e.getWorld())&&e.toWeatherState())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void onThunder(org.bukkit.event.weather.ThunderChangeEvent e){if(islandWorld(e.getWorld())&&e.toThunderState())e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void onTime(org.bukkit.event.world.TimeSkipEvent e){if(islandWorld(e.getWorld())&&Math.floorMod(e.getWorld().getTime()+e.getSkipAmount(),24000L)!=6000L)e.setCancelled(true);}
    private boolean islandWorld(World world){return world!=null&&world.getName().equals(worldName);}
    private boolean admin(Player p){return p.hasPermission("kasabatr.ada.admin");}
    boolean authenticated(Player p){if(!Bukkit.getPluginManager().isPluginEnabled("KasabaAuth"))return true;var state=Bukkit.getServicesManager().load(tr.kasaba.auth.api.AuthState.class);return state!=null&&state.authenticated(p.getUniqueId());}
    private boolean allowed(Player p,Location at){return !islandWorld(at.getWorld())||!claims.resetting(layout.at(at.getBlockX(),at.getBlockZ()))&&(admin(p)||claims.canAccess(layout.at(at.getBlockX(),at.getBlockZ()),p.getUniqueId()));}
    private void enterIsland(Player p)throws IOException{playerStates.enter(p);creativeMode.enter(p);}
    private void leaveIsland(Player p)throws IOException{creativeMode.leave(p.getUniqueId());playerStates.leave(p);}
    private boolean travelToIsland(Player p,World world,int id)throws IOException{
        Location before=p.getLocation();boolean outside=!islandWorld(before.getWorld()),arrived=false;
        try{
            // Snapshot the ordinary inventory/mode BEFORE world managers run.
            // Creative must already be present in the first world-entry packet.
            enterIsland(p);
            if(!p.teleport(layout.spawn(world,id))||!islandWorld(p.getWorld()))return false;
            enterIsland(p);arrived=true;return true;
        }finally{
            if(!arrived&&outside){
                creativeMode.leave(p.getUniqueId());
                if(islandWorld(p.getWorld())&&!p.teleport(before))getLogger().warning("Ada geçişi geri alınamadı: "+p.getUniqueId());
                playerStates.leave(p);
            }
        }
    }
    private boolean mayEdit(Player p,Block block){return allowed(p,block.getLocation());}
    private int plot(Block block){return layout.at(block.getX(),block.getZ());}
    private void warn(Player p,String text){long now=System.currentTimeMillis();if(now-lastWarning.getOrDefault(p.getUniqueId(),0L)>=2000){tell(p,text);lastWarning.put(p.getUniqueId(),now);}}
    private void tell(Player p,String text){p.sendMessage(PREFIX+text);}
    private String normalize(String text){return text.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    private UUID knownPlayer(String name){Player p=Bukkit.getPlayerExact(name);if(p!=null)return p.getUniqueId();UUID id=claims.playerByName(name);if(id!=null)return id;OfflinePlayer offline=Bukkit.getOfflinePlayerIfCached(name);return offline!=null&&offline.hasPlayedBefore()?offline.getUniqueId():null;}
    private void needIsland(Player p){tell(p,"§eHenüz adan yok. Önce §6/ada al §eyaz (normal: 50 Akçe / VIP: ücretsiz). Sonra Adanı Yap düğmesi seni kendi adana götürür.");p.showDialog(io.papermc.paper.dialog.Dialog.create(builder->builder.empty().base(io.papermc.paper.registry.data.dialog.DialogBase.builder(net.kyori.adventure.text.Component.text("Önce adanı al")).canCloseWithEscape(true).pause(false).body(List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(net.kyori.adventure.text.Component.text("Önce /ada al: normal oyuncu 50 Akçe ile 1 ada, VIP en fazla 2 ücretsiz ada alabilir. 15 gün giriş yapmazsan adaların sıfırlanır. Daha sonra Oyunlar → Adanı Yap üzerinden adana dönebilirsin."),300))).build()).type(io.papermc.paper.registry.data.dialog.type.DialogType.notice())));}
    private final class IslandGame implements tr.kasaba.hub.api.HubGame {
        public tr.kasaba.hub.api.GameInfo info(){return new tr.kasaba.hub.api.GameInfo("adaniyap","Adanı Yap","Yaratıcı adan • ilk kullanımda /ada al • normal oyuncu 1 ada / 50 Akçe; VIP 2 ücretsiz ada.",1,20);}
        public boolean available(){return Bukkit.getWorld(worldName)!=null;}
        public boolean join(List<Player> team){
            if(team.isEmpty())return false;Map<Player,Integer> targets=new java.util.LinkedHashMap<>();for(Player p:team){Integer id=requests.getOrDefault(p.getUniqueId(),claims.islandOf(p.getUniqueId()));if(id==null){needIsland(p);return false;}if(!claims.canAccess(id,p.getUniqueId())){tell(p,"Bu adaya giriş iznin yok.");return false;}targets.put(p,id);}
            Map<Player,Location> moved=new java.util.LinkedHashMap<>();try{for(var entry:targets.entrySet()){Player p=entry.getKey();Location before=p.getLocation();sessions.add(p.getUniqueId());if(!travelToIsland(p,Bukkit.getWorld(worldName),entry.getValue()))throw new IllegalStateException("Ada geçişi engellendi.");moved.put(p,before);tell(p,"§aAda #"+entry.getValue()+" • Yaratıcı mod • Dönüş: /lobi");}return true;}catch(IOException|RuntimeException ex){for(Player p:targets.keySet()){sessions.remove(p.getUniqueId());creativeMode.leave(p.getUniqueId());}for(var entry:moved.entrySet())entry.getKey().teleport(entry.getValue());getLogger().log(java.util.logging.Level.WARNING,"Ada grubu geri alındı.",ex);return false;}
        }
        public void leave(UUID id){sessions.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null)try{leaveIsland(p);}catch(IOException ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada çıkışı kaydedilemedi: "+id,ex);p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);p.setItemOnCursor(null);p.setGameMode(GameMode.SURVIVAL);}}
    }
    private void teleportHome(Player p,World world,int id)throws IOException{
        if(hub!=null&&!islandWorld(p.getWorld())){if(hub.playing(p.getUniqueId())){tell(p,"Önce /lobi ile mevcut oyundan çık, ardından /ada yaz.");return;}requests.put(p.getUniqueId(),id);try{Bukkit.dispatchCommand(p,"oyunlar git adaniyap");}finally{requests.remove(p.getUniqueId());}return;}
        if(travelToIsland(p,world,id))tell(p,"§aAda #"+id+" • Yaratıcı mod");else tell(p,"Işınlanma engellendi; ada sahipliğin korundu.");
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Bu komut oyunda kullanılır.");return true;}if(!authenticated(p)){tell(p,"Önce kayıt/giriş işlemini tamamla.");return true;}if(!p.hasPermission("kasabatr.ada.use")){tell(p,"Ada yetkin yok.");return true;}
        try{claims.remember(p.getUniqueId(),p.getName());World world=ensureWorld();if(world==null){tell(p,"Ada dünyası bulunamadı. Paket içindeki ada klasörü sunucunun ana dizinine kurulmalı.");return true;}Integer own=claims.islandOf(p.getUniqueId());int current=layout.at(p.getLocation().getBlockX(),p.getLocation().getBlockZ());if(islandWorld(p.getWorld())&&p.getUniqueId().equals(claims.ownerOf(current)))own=current;String action=args.length==0?"git":normalize(args[0]);
            if(action.equals("yardim")){tell(p,"/ada al • /ada • /ada <sahip> • /ada ekle <oyuncu> • /ada çıkar <oyuncu> • /ada bilgi • /lobi");return true;}
            if(action.equals("bilgi")){tell(p,"Adaların: "+claims.owned(p.getUniqueId())+" • Sınır: "+(tr.kasaba.hub.api.NetworkFeatures.isVip(p)?2:1)+" • Normal arsa 50 Akçe; VIP 2 ücretsiz arsa.");inactivityWarning(p);return true;}
            if(action.equals("al")){purchasePage(p);return true;}
            if(action.equals("git")&&args.length<=2){List<Integer> all=claims.owned(p.getUniqueId());if(all.isEmpty()){needIsland(p);return true;}int which=args.length==2?Integer.parseInt(args[1]):1;if(which<1||which>all.size())throw new IllegalArgumentException("/ada git <1–"+all.size()+">");int id=all.get(which-1);if(claims.resetting(id))throw new IllegalArgumentException("Bu ada 15 günlük süre dolduğu için temizleniyor.");teleportHome(p,world,id);inactivityWarning(p);return true;}
            if(Set.of("ekle","cikar").contains(action)){if(args.length!=2)throw new IllegalArgumentException("/ada "+action+" <oyuncu>");if(own==null){needIsland(p);return true;}UUID friend=knownPlayer(args[1]);if(friend==null)throw new IllegalArgumentException("Oyuncu daha önce sunucuya girmiş olmalı.");if(friend.equals(p.getUniqueId()))throw new IllegalArgumentException("Kendini ekleyemezsin.");boolean added=action.equals("ekle");boolean changed=added?claims.addMember(own,friend):claims.removeMember(own,friend);tell(p,changed?args[1]+(added?" adana eklendi.":" adandan çıkarıldı."):"Arkadaş kaydı değişmedi.");Player other=Bukkit.getPlayer(friend);if(changed&&other!=null){tell(other,p.getName()+(added?" seni adasına ekledi. /ada "+p.getName()+" ile gidebilirsin.":" seni adasından çıkardı."));if(!added&&islandWorld(other.getWorld())&&layout.contains(own,other.getLocation().getBlockX(),other.getLocation().getBlockZ()))relocate(other);}return true;}
            if(args.length==1||args.length==2&&args[1].matches("[12]")){UUID owner=knownPlayer(args[0]);List<Integer> options=owner==null?List.of():claims.owned(owner);int choice=args.length==2?Integer.parseInt(args[1]):1;Integer id=choice>=1&&choice<=options.size()?options.get(choice-1):null;if(id==null||!claims.canAccess(id,p.getUniqueId()))throw new IllegalArgumentException("Bu adaya giriş iznin yok. Sahibi, ilgili adasında /ada ekle "+p.getName()+" yazmalı.");teleportHome(p,world,id);return true;}throw new IllegalArgumentException("/ada yardım");
        }catch(IOException ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada kaydı başarısız; dosya korundu.",ex);tell(p,"Ada kaydı tamamlanamadı. Yönetici kayıtları kontrol etmeli.");}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}return true;
    }
    private void inactivityWarning(Player p){tell(p,"§e15 gün sunucuya giriş yapmazsan tüm adaların ve yaptığın yapılar sıfırlanır, adalar yeniden satışa açılır. Giriş yapman süreyi yeniler.");}
    private void purchasePage(Player p){boolean vip=tr.kasaba.hub.api.NetworkFeatures.isVip(p);int count=claims.owned(p.getUniqueId()).size(),limit=vip?2:1;if(count>=limit)throw new IllegalArgumentException("Arsa sınırın doldu: "+limit+". /ada git 1"+(count==2?" veya /ada git 2":""));String cost=vip?"Ücretsiz":"50 Akçe";p.showDialog(io.papermc.paper.dialog.Dialog.create(builder->builder.empty().base(io.papermc.paper.registry.data.dialog.DialogBase.builder(net.kyori.adventure.text.Component.text("Arsa satın al • "+cost)).canCloseWithEscape(true).pause(false).body(List.of(io.papermc.paper.registry.data.dialog.body.DialogBody.plainMessage(net.kyori.adventure.text.Component.text("Arsa: "+cost+". 15 gün sunucuya giriş yapmazsan adan ve yapılar sıfırlanır, ada tekrar satılır."),330))).build()).type(io.papermc.paper.registry.data.dialog.type.DialogType.multiAction(List.of(io.papermc.paper.registry.data.dialog.ActionButton.create(net.kyori.adventure.text.Component.text(cost+" • Onayla"),null,160,io.papermc.paper.registry.data.dialog.action.DialogAction.customClick((response,audience)->{if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId())||!isEnabled())return;Bukkit.getScheduler().runTask(this,()->{try{if(!authenticated(p)||tr.kasaba.hub.api.NetworkFeatures.isVip(p)!=vip||claims.owned(p.getUniqueId()).size()!=count)throw new IllegalArgumentException("Arsa hakkın değişti; /ada al ile tekrar kontrol et.");var network=tr.kasaba.hub.api.NetworkFeatures.get();if(network==null)throw new IllegalArgumentException("Akçe sistemi hazır değil; arsa verilmedi.");int reserved=claims.prepare(p.getUniqueId(),vip?"0.00":"50.00");if(reserved==0)throw new IllegalArgumentException("Boş ada bulunamadı.");if(!network.pay(p.getUniqueId(),new java.math.BigDecimal(claims.price(p.getUniqueId())),"ada:"+claims.transaction(p.getUniqueId()))){claims.cancel(p.getUniqueId());throw new IllegalArgumentException("Arsa için 50 Akçe gerekiyor. Bakiyen yetersiz.");}int id=claims.complete(p.getUniqueId());tell(p,"§aAda #"+id+" senin oldu. "+cost);inactivityWarning(p);teleportHome(p,ensureWorld(),id);}catch(Exception ex){tell(p,ex.getMessage()==null?"Arsa işlemi kayıtta; yeniden denenecek.":ex.getMessage());}});},net.kyori.adventure.text.event.ClickCallback.Options.builder().uses(1).lifetime(java.time.Duration.ofMinutes(2)).build())),io.papermc.paper.registry.data.dialog.ActionButton.create(net.kyori.adventure.text.Component.text("Vazgeç"),null,150,null))).columns(2).build())));}
    private void recoverPurchases(){var network=tr.kasaba.hub.api.NetworkFeatures.get();if(network==null||ensureWorld()==null)return;for(UUID owner:claims.pending().keySet())try{if(network.pay(owner,new java.math.BigDecimal(claims.price(owner)),"ada:"+claims.transaction(owner)))claims.complete(owner);else claims.cancel(owner);}catch(Exception ex){getLogger().log(java.util.logging.Level.WARNING,"Bekleyen arsa ödemesi korunuyor: "+owner,ex);}}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){if(args.length==1)return List.of("al","git","ekle","çıkar","bilgi","yardım").stream().filter(s->normalize(s).startsWith(normalize(args[0]))).toList();if(args.length==2&&Set.of("ekle","cikar").contains(normalize(args[0])))return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(s->s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).toList();return List.of();}
    private void relocate(Player p){if(!relocating.add(p.getUniqueId()))return;Bukkit.getScheduler().runTask(this,()->{relocating.remove(p.getUniqueId());if(!p.isOnline()||!islandWorld(p.getWorld())||allowed(p,p.getLocation()))return;Integer own=claims.islandOf(p.getUniqueId());if(own!=null)p.teleport(layout.spawn(p.getWorld(),own));else if(hub!=null)hub.returnToLobby(p.getUniqueId());else for(World world:Bukkit.getWorlds())if(!islandWorld(world)){p.teleport(world.getSpawnLocation());break;}});}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onMove(PlayerMoveEvent e){if(!e.hasChangedBlock()||!islandWorld(e.getTo().getWorld())||allowed(e.getPlayer(),e.getTo()))return;e.setTo(e.getFrom());warn(e.getPlayer(),"Kendi veya eklendiğin adanın sınırlarında kalmalısın.");if(!allowed(e.getPlayer(),e.getFrom()))relocate(e.getPlayer());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onTeleport(PlayerTeleportEvent e){if(!islandWorld(e.getTo().getWorld()))return;if(!allowed(e.getPlayer(),e.getTo())){e.setCancelled(true);warn(e.getPlayer(),"Başkasının adasına veya sahipsiz alana ışınlanamazsın.");}else if(hub!=null&&hub.playing(e.getPlayer().getUniqueId())&&!islandWorld(e.getFrom().getWorld())&&!sessions.contains(e.getPlayer().getUniqueId())){e.setCancelled(true);warn(e.getPlayer(),"Önce /lobi ile mevcut oyundan çık.");}}
    @EventHandler public void onJoin(PlayerJoinEvent e){Bukkit.getScheduler().runTaskLater(this,()->{Player p=e.getPlayer();if(!p.isOnline())return;try{claims.remember(p.getUniqueId(),p.getName());if(!authenticated(p))return;claims.seen(p.getUniqueId(),System.currentTimeMillis());if(islandWorld(p.getWorld())){enterIsland(p);if(!allowed(p,p.getLocation()))relocate(p);}else playerStates.recoverOutside(p);}catch(IOException ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada oyuncu kaydı açılamadı.",ex);warn(p,"Ada envanteri okunamadı; yöneticiye bildir.");}},20);}
    void verifiedPlayer(Player p){if(!p.isOnline()||!authenticated(p))return;try{claims.seen(p.getUniqueId(),System.currentTimeMillis());if(claims.islandOf(p.getUniqueId())!=null)inactivityWarning(p);if(islandWorld(p.getWorld())){enterIsland(p);if(!allowed(p,p.getLocation()))relocate(p);}else playerStates.recoverOutside(p);}catch(IOException ex){getLogger().log(java.util.logging.Level.SEVERE,"Doğrulama sonrası ada yedeği açılamadı.",ex);warn(p,"Ada yedeği okunamadı; yöneticiye bildir.");}}
    @EventHandler(priority=EventPriority.HIGHEST) public void onWorldChange(PlayerChangedWorldEvent e){Player p=e.getPlayer();if(!authenticated(p))return;try{if(islandWorld(p.getWorld())){enterIsland(p);if(!allowed(p,p.getLocation()))relocate(p);}else if(islandWorld(e.getFrom()))leaveIsland(p);}catch(IOException ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada envanteri değiştirilemedi.",ex);warn(p,"Ada envanteri değiştirilemedi; yöneticiye bildir.");if(islandWorld(p.getWorld()))p.teleport(e.getFrom().getSpawnLocation());else{p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);p.setItemOnCursor(null);p.setGameMode(GameMode.SURVIVAL);}}}
    @EventHandler public void onQuit(PlayerQuitEvent e){lastWarning.remove(e.getPlayer().getUniqueId());sessions.remove(e.getPlayer().getUniqueId());creativeMode.leave(e.getPlayer().getUniqueId());if(islandWorld(e.getPlayer().getWorld()))try{playerStates.saveIsland(e.getPlayer());}catch(IOException ex){getLogger().severe(ex.getMessage());}}
    @EventHandler(ignoreCancelled=true) public void onInventoryOpen(InventoryOpenEvent e){if(e.getPlayer() instanceof Player p&&islandWorld(p.getWorld())&&e.getInventory().getType()==InventoryType.ENDER_CHEST){e.setCancelled(true);warn(p,"Yaratıcı ada eşyalarını dışarı taşımamak için ender sandığı kapalı.");}}
    @EventHandler public void onRespawn(PlayerRespawnEvent e){Player p=e.getPlayer();if(!islandWorld(p.getWorld()))return;Integer own=claims.islandOf(p.getUniqueId());int current=layout.at(p.getLocation().getBlockX(),p.getLocation().getBlockZ());if(claims.canAccess(current,p.getUniqueId()))own=current;if(own!=null)e.setRespawnLocation(layout.spawn(p.getWorld(),own));Bukkit.getScheduler().runTask(this,()->{if(p.isOnline()&&authenticated(p)&&islandWorld(p.getWorld()))try{enterIsland(p);}catch(IOException ex){getLogger().log(java.util.logging.Level.SEVERE,"Ada doğma envanteri açılamadı.",ex);}});}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onDrop(org.bukkit.event.player.PlayerDropItemEvent e){if(islandWorld(e.getPlayer().getWorld()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onPortal(org.bukkit.event.player.PlayerPortalEvent e){if(islandWorld(e.getFrom().getWorld())||islandWorld(e.getTo().getWorld())){e.setCancelled(true);warn(e.getPlayer(),"Adadan çıkmak için /lobi kullan.");}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void onDamage(org.bukkit.event.entity.EntityDamageEvent e){if(e.getEntity() instanceof Player p&&islandWorld(p.getWorld()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onVehicleMove(VehicleMoveEvent vehicleMoveEvent) {
        if (!this.islandWorld(vehicleMoveEvent.getTo().getWorld())) {
            return;
        }
        for (Entity entity : vehicleMoveEvent.getVehicle().getPassengers()) {
            Player player;
            if (!(entity instanceof Player) || this.allowed(player = (Player)entity, vehicleMoveEvent.getTo())) continue;
            vehicleMoveEvent.getVehicle().teleport(vehicleMoveEvent.getFrom());
            vehicleMoveEvent.getVehicle().setVelocity(new Vector());
            this.warn(player, "Ada sinirinin disina cikamazsin.");
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBreak(BlockBreakEvent blockBreakEvent) {
        if (!this.mayEdit(blockBreakEvent.getPlayer(), blockBreakEvent.getBlock())) {
            blockBreakEvent.setCancelled(true);
            this.warn(blockBreakEvent.getPlayer(), "Yalnizca kendi adanda blok kirabilirsin.");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPlace(BlockPlaceEvent blockPlaceEvent) {
        if (!this.islandWorld(blockPlaceEvent.getBlockPlaced().getWorld())) {
            return;
        }
        boolean bl = this.mayEdit(blockPlaceEvent.getPlayer(), blockPlaceEvent.getBlockPlaced());
        if (blockPlaceEvent instanceof BlockMultiPlaceEvent) {
            BlockMultiPlaceEvent blockMultiPlaceEvent = (BlockMultiPlaceEvent)blockPlaceEvent;
            for (BlockState blockState : blockMultiPlaceEvent.getReplacedBlockStates()) {
                bl &= this.mayEdit(blockPlaceEvent.getPlayer(), blockState.getBlock());
            }
        }
        if (!bl) {
            blockPlaceEvent.setCancelled(true);
            this.warn(blockPlaceEvent.getPlayer(), "Yalnizca kendi adanda blok koyabilirsin.");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onInteract(PlayerInteractEvent playerInteractEvent) {
        Block block = playerInteractEvent.getClickedBlock();
        if (block != null && !this.mayEdit(playerInteractEvent.getPlayer(), block)) {
            playerInteractEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityInteract(PlayerInteractEntityEvent playerInteractEntityEvent) {
        if (!this.allowed(playerInteractEntityEvent.getPlayer(), playerInteractEntityEvent.getRightClicked().getLocation())) {
            playerInteractEntityEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBucketEmpty(PlayerBucketEmptyEvent playerBucketEmptyEvent) {
        Block block = playerBucketEmptyEvent.getBlockClicked().getRelative(playerBucketEmptyEvent.getBlockFace());
        if (!this.mayEdit(playerBucketEmptyEvent.getPlayer(), block)) {
            playerBucketEmptyEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBucketFill(PlayerBucketFillEvent playerBucketFillEvent) {
        if (!this.mayEdit(playerBucketFillEvent.getPlayer(), playerBucketFillEvent.getBlockClicked())) {
            playerBucketFillEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onHangingPlace(HangingPlaceEvent hangingPlaceEvent) {
        if (!this.islandWorld(hangingPlaceEvent.getEntity().getWorld())) {
            return;
        }
        if (hangingPlaceEvent.getPlayer() == null || !this.allowed(hangingPlaceEvent.getPlayer(), hangingPlaceEvent.getEntity().getLocation())) {
            hangingPlaceEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onLiquid(BlockFromToEvent blockFromToEvent) {
        if (!this.islandWorld(blockFromToEvent.getBlock().getWorld())) {
            return;
        }
        int n = this.plot(blockFromToEvent.getBlock());
        if (n == 0 || !this.claims.isClaimed(n) || this.plot(blockFromToEvent.getToBlock()) != n) {
            blockFromToEvent.setCancelled(true);
        }
    }

    private boolean safePiston(Block block, List<Block> list) {
        int n = this.plot(block);
        if (n == 0 || !this.claims.isClaimed(n)) {
            return false;
        }
        BlockData blockData = block.getBlockData();
        if (!(blockData instanceof Directional)) {
            return false;
        }
        Directional directional = (Directional)blockData;
        for (Block block2 : list) {
            if (this.plot(block2) == n && this.plot(block2.getRelative(directional.getFacing())) == n && this.plot(block2.getRelative(directional.getFacing().getOppositeFace())) == n) continue;
            return false;
        }
        return true;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent blockPistonExtendEvent) {
        if (this.islandWorld(blockPistonExtendEvent.getBlock().getWorld()) && !this.safePiston(blockPistonExtendEvent.getBlock(), blockPistonExtendEvent.getBlocks())) {
            blockPistonExtendEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent blockPistonRetractEvent) {
        if (this.islandWorld(blockPistonRetractEvent.getBlock().getWorld()) && !this.safePiston(blockPistonRetractEvent.getBlock(), blockPistonRetractEvent.getBlocks())) {
            blockPistonRetractEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDispense(BlockDispenseEvent blockDispenseEvent) {
        Directional directional;
        if (!this.islandWorld(blockDispenseEvent.getBlock().getWorld())) {
            return;
        }
        Block block = blockDispenseEvent.getBlock();
        int n = this.plot(block);
        if (n == 0 || !this.claims.isClaimed(n)) {
            blockDispenseEvent.setCancelled(true);
            return;
        }
        BlockData blockData = block.getBlockData();
        if (blockData instanceof Directional && this.plot(block.getRelative((directional = (Directional)blockData).getFacing())) != n) {
            blockDispenseEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onFertilize(BlockFertilizeEvent blockFertilizeEvent) {
        if (!this.islandWorld(blockFertilizeEvent.getBlock().getWorld())) {
            return;
        }
        Player player = blockFertilizeEvent.getPlayer();
        if (player == null || !this.allowed(player, blockFertilizeEvent.getBlock().getLocation())) {
            blockFertilizeEvent.setCancelled(true);
            return;
        }
        for (BlockState blockState : blockFertilizeEvent.getBlocks()) {
            if (this.allowed(player, blockState.getLocation())) continue;
            blockFertilizeEvent.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onSpread(BlockSpreadEvent blockSpreadEvent) {
        if (!this.islandWorld(blockSpreadEvent.getBlock().getWorld())) {
            return;
        }
        int n = this.plot(blockSpreadEvent.getSource());
        if (n == 0 || !this.claims.isClaimed(n) || this.plot(blockSpreadEvent.getBlock()) != n) {
            blockSpreadEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onIgnite(BlockIgniteEvent blockIgniteEvent) {
        if (!this.islandWorld(blockIgniteEvent.getBlock().getWorld())) {
            return;
        }
        int n = this.plot(blockIgniteEvent.getBlock());
        if (n == 0 || !this.claims.isClaimed(n) || blockIgniteEvent.getPlayer() != null && !this.allowed(blockIgniteEvent.getPlayer(), blockIgniteEvent.getBlock().getLocation())) {
            blockIgniteEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBurn(BlockBurnEvent blockBurnEvent) {
        if (this.islandWorld(blockBurnEvent.getBlock().getWorld()) && !this.claims.isClaimed(this.plot(blockBurnEvent.getBlock()))) {
            blockBurnEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityChangeBlock(EntityChangeBlockEvent entityChangeBlockEvent) {
        if (!this.islandWorld(entityChangeBlockEvent.getBlock().getWorld())) {
            return;
        }
        if (entityChangeBlockEvent.getEntityType() == EntityType.ENDERMAN || !this.claims.isClaimed(this.plot(entityChangeBlockEvent.getBlock()))) {
            entityChangeBlockEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent entityExplodeEvent) {
        if (this.islandWorld(entityExplodeEvent.getLocation().getWorld())) {
            entityExplodeEvent.blockList().clear();
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockExplode(BlockExplodeEvent blockExplodeEvent) {
        if (this.islandWorld(blockExplodeEvent.getBlock().getWorld())) {
            blockExplodeEvent.blockList().clear();
        }
    }
}
