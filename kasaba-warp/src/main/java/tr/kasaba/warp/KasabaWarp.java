package tr.kasaba.warp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.bukkit.plugin.ServicePriority;
import tr.kasaba.warp.api.*;

public final class KasabaWarp extends JavaPlugin implements Listener,TabCompleter {
    private record Settings(int warmup,int cooldown) {}
    private static final class Pending {
        final Player player;final Warp warp;final WarpPoint point;final Location origin;final long ready;
        long loadingDeadline;boolean loading;int shown=-1;
        Pending(Player player,Warp warp,WarpPoint point,int warmup){this.player=player;this.warp=warp;this.point=point;origin=player.getLocation().clone();ready=System.nanoTime()+TimeUnit.SECONDS.toNanos(warmup);}
    }
    private final Cooldowns cooldowns=new Cooldowns();
    private final Map<UUID,Pending> pending=new HashMap<>();
    private final Map<UUID,Location> permittedTeleports=new HashMap<>();
    private final Map<String,Permission> registered=new HashMap<>();
    private Map<String,Warp> warps=Map.of();
    private volatile List<WarpDestination> regions=List.of();
    private WarpStore store;private Settings settings;private int ticks;

    @Override public void onEnable() {
        saveDefaultConfig();store=new WarpStore(getDataFolder().toPath().resolve("warplar.yml"));
        try {settings=readSettings();warps=store.load();validateIcons(warps);}
        catch(IOException e){getLogger().log(Level.SEVERE,"Warp ayarları okunamadı; mevcut dosyalara dokunulmadı.",e);getServer().getPluginManager().disablePlugin(this);return;}
        regions=regions(warps);
        Bukkit.getServicesManager().register(WarpRegions.class,()->regions,this,ServicePriority.Normal);
        synchronizePermissions();getServer().getPluginManager().registerEvents(this,this);
        Objects.requireNonNull(getCommand("warp")).setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer(this,this::tick,1,1);
    }
    @Override public void onDisable() {
        for(Pending travel:new ArrayList<>(pending.values()))cancel(travel.player,"Warp sistemi durduruldu; ışınlanma iptal edildi.");
        for(Player player:Bukkit.getOnlinePlayers())if(player.getOpenInventory().getTopInventory().getHolder() instanceof WarpMenu)player.closeInventory();
        for(var entry:registered.entrySet())if(Bukkit.getPluginManager().getPermission(entry.getKey())==entry.getValue())Bukkit.getPluginManager().removePermission(entry.getValue());
        registered.clear();permittedTeleports.clear();
        Bukkit.getServicesManager().unregisterAll(this);regions=List.of();
    }
    private Settings readSettings() throws IOException {
        YamlConfiguration yaml=new YamlConfiguration();
        try{yaml.load(getDataFolder().toPath().resolve("config.yml").toFile());}
        catch(InvalidConfigurationException e){throw new IOException("config.yml bozuk",e);}
        return new Settings(integer(yaml,"hazirlik-saniye",3,0,60),integer(yaml,"kullanim-arasi-saniye",20,0,3600));
    }
    private static int integer(YamlConfiguration yaml,String key,int fallback,int min,int max) throws IOException {
        if(yaml.contains(key)&&!yaml.isInt(key))throw new IOException(key+" tam sayı olmalı.");
        int value=yaml.getInt(key,fallback);if(value<min||value>max)throw new IOException(key+" "+min+"–"+max+" arasında olmalı.");return value;
    }
    private void synchronizePermissions() {
        for(Warp warp:warps.values())if(Bukkit.getPluginManager().getPermission(warp.permission())==null) {
            Permission permission=new Permission(warp.permission(),"Warp erişimi: "+warp.id(),PermissionDefault.FALSE);
            Bukkit.getPluginManager().addPermission(permission);registered.put(permission.getName(),permission);
        }
        for(var iterator=registered.entrySet().iterator();iterator.hasNext();) {
            var entry=iterator.next();
            if(warps.values().stream().noneMatch(warp->warp.permission().equals(entry.getKey()))) {
                if(Bukkit.getPluginManager().getPermission(entry.getKey())==entry.getValue())Bukkit.getPluginManager().removePermission(entry.getValue());iterator.remove();
            }
        }
    }
    private boolean admin(CommandSender sender){return sender.hasPermission("kasabawarp.yonet")||sender.hasPermission("kasabaroles.admin");}
    private boolean accessible(Player player,Warp warp){return player.hasPermission("kasabawarp.kullan")&&(!warp.restricted()||admin(player)||player.hasPermission(warp.permission()));}
    private List<Warp> visible(Player player){return warps.values().stream().filter(warp->accessible(player,warp)).sorted(Comparator.comparing(Warp::id)).toList();}
    private void tell(CommandSender sender,String text){sender.sendMessage(Component.text(text,NamedTextColor.YELLOW));}
    private void validateIcons(Map<String,Warp> points) throws IOException {
        for(Warp warp:points.values())try{WarpIcon.decode(warp.icon());}
        catch(RuntimeException e){throw new IOException("Simge okunamadı: "+warp.id()+". Dosyayı yedekleyip simge alanını kontrol et.",e);}
    }
    private void open(Player player,int page){player.openInventory(new WarpMenu(player.getUniqueId(),page,visible(player),MenuLayout.reserved(warps.values()),settings.warmup(),settings.cooldown()).getInventory());}
    private void need(String[] args,int size,String usage){if(args.length<size)throw new IllegalArgumentException("Kullanım: "+usage);}
    private Warp find(String name){Warp warp=warps.get(Warp.id(name));if(warp==null)throw new IllegalArgumentException("Warp bulunamadı: "+name);return warp;}
    private Player player(CommandSender sender){if(sender instanceof Player player)return player;throw new IllegalArgumentException("Bu işlem oyun içinden kullanılır.");}
    private Warp at(String id,String name,String description,boolean restricted,Location location,String icon,int menuSlot) {
        return new Warp(id,name,description,restricted,location.getWorld().getUID(),location.getWorld().getName(),location.getX(),location.getY(),location.getZ(),location.getYaw(),location.getPitch(),icon,menuSlot);
    }
    private WarpPoint point(Location location){return new WarpPoint(location.getX(),location.getY(),location.getZ(),location.getYaw(),location.getPitch());}
    private void put(Warp warp) throws IOException {Map<String,Warp> next=new LinkedHashMap<>(warps);next.put(warp.id(),warp);save(next,warp.id());}
    private List<WarpDestination> regions(Map<String,Warp> points){return points.values().stream().flatMap(w->w.points().stream().map(p->new WarpDestination(w.id(),w.world(),p.x(),p.z()))).toList();}
    private void validatePlacement(Map<String,Warp> next){
        List<WarpDestination> destinations=regions(next);
        for(var registration:Bukkit.getServicesManager().getRegistrations(WarpPlacementGuard.class))
            registration.getProvider().rejection(destinations).ifPresent(reason->{throw new IllegalArgumentException(reason);});
    }
    private void save(Map<String,Warp> next,String changed) throws IOException {
        validatePlacement(next);store.save(next);warps=Collections.unmodifiableMap(new LinkedHashMap<>(next));regions=regions(warps);synchronizePermissions();
        for(Pending travel:new ArrayList<>(pending.values()))if(travel.warp.id().equals(changed))cancel(travel.player,"Warp değiştirildi; yeniden dene.");
        refreshMenus();
    }
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(settings==null){tell(sender,"Warp sistemi kullanılamıyor; konsolu kontrol et.");return true;}
        try {
            if(args.length==0){Player player=player(sender);if(!player.hasPermission("kasabawarp.kullan"))throw new IllegalArgumentException("Warp kullanma yetkin yok.");open(player,0);return true;}
            String action=Warp.id(args[0]);
            if(!Warp.COMMANDS.contains(action)){request(player(sender),find(args[0]));return true;}
            if(!Set.of("bilgi","iptal","yardim").contains(action)&&!admin(sender))throw new IllegalArgumentException("Bu komut için yetkin yok.");
            switch(action) {
                case "yardim" -> {
                    tell(sender,"/warp | /warp <ad> | /warp bilgi <ad> | /warp iptal");
                    if(admin(sender)){
                        tell(sender,"Yönetim: oluştur, taşı, sil, isim, açıklama, simge, yetki, gönder, yenile. Örnek: /warp oluştur orman 1-3");
                        tell(sender,"Çoklu konum: /warp noktaekle orman | /warp noktalar orman | /warp noktasil orman 2");
                    }
                }
                case "iptal" -> {Player player=player(sender);if(!cancel(player,"Işınlanma iptal edildi."))tell(player,"Bekleyen bir ışınlanman yok.");}
                case "bilgi" -> {
                    need(args,2,"/warp bilgi <ad>");Warp warp=find(args[1]);
                    if(sender instanceof Player player&&!accessible(player,warp))throw new IllegalArgumentException("Bu warpa erişimin yok.");
                    tell(sender,warp.name()+" ("+warp.id()+")"+(warp.description().isBlank()?"":" — "+warp.description()));
                    if(admin(sender))tell(sender,String.format(Locale.ROOT,"%s: %.2f, %.2f, %.2f • %s",warp.worldName(),warp.x(),warp.y(),warp.z(),warp.restricted()?warp.permission():"Herkese açık"));
                    if(admin(sender))tell(sender,"Menü yeri: "+(warp.menuSlot()<0?"Otomatik":MenuLayout.label(warp.menuSlot())));
                    tell(sender,"Kayıtlı konum: "+warp.points().size()+(warp.points().size()>1?" • Her kullanımda rastgele biri seçilir.":""));
                }
                case "olustur" -> {
                    need(args,2,"/warp oluştur <ad> [sıra-kutu]");String id=Warp.id(args[1]);if(warps.containsKey(id))throw new IllegalArgumentException("Bu warp zaten var. Simge/menü yeri: /warp simge "+id+" 1-3; konum: /warp taşı "+id);
                    Player player=player(sender);int slot=args.length>2?MenuLayout.parse(args[2]):-1;
                    put(at(id,args[1],"",false,player.getLocation(),WarpIcon.capture(player.getInventory().getItemInMainHand()),slot));
                    tell(sender,"Warp oluşturuldu: "+id+(slot>=0?" • Menü: "+MenuLayout.label(slot):""));
                }
                case "tasi" -> {need(args,2,"/warp taşı <ad>");Warp old=find(args[1]);Location location=player(sender).getLocation();put(old.moved(location.getWorld().getUID(),location.getWorld().getName(),point(location)));tell(sender,"Warpın 1. noktası bulunduğun yere taşındı; ek noktalar korundu: "+old.id());}
                case "noktaekle" -> {
                    need(args,2,"/warp noktaekle <ad>");Warp old=find(args[1]);Location location=player(sender).getLocation();
                    Warp next=old.addedPoint(location.getWorld().getUID(),point(location));put(next);
                    tell(sender,"Warp noktası eklendi: "+old.id()+" • Toplam "+next.points().size()+" konum. Her kullanımda rastgele biri seçilecek.");
                }
                case "noktalar" -> {
                    need(args,2,"/warp noktalar <ad>");Warp warp=find(args[1]);tell(sender,warp.id()+" • "+warp.worldName()+" • "+warp.points().size()+" konum (rastgele seçim)");
                    List<WarpPoint> points=warp.points();for(int i=0;i<points.size();i++){WarpPoint p=points.get(i);tell(sender,String.format(Locale.ROOT,"%d: %.2f, %.2f, %.2f • yön %.1f / %.1f",i+1,p.x(),p.y(),p.z(),p.yaw(),p.pitch()));}
                }
                case "noktasil" -> {
                    need(args,3,"/warp noktasil <ad> <numara>");Warp old=find(args[1]);int number;
                    try{number=Integer.parseInt(args[2]);}catch(NumberFormatException e){throw new IllegalArgumentException("/warp noktalar "+old.id()+" ile listedeki nokta numarasını öğren.");}
                    Warp next=old.removedPoint(number);put(next);tell(sender,"Nokta silindi: "+old.id()+" #"+number+" • Kalan "+next.points().size()+" konum.");
                }
                case "simge" -> {
                    need(args,2,"/warp simge <ad> [sıra-kutu]");Warp old=find(args[1]);Player player=player(sender);
                    if(player.getInventory().getItemInMainHand().getType().isAir())throw new IllegalArgumentException("Simge olarak kullanacağın eşyayı ana eline al.");
                    int slot=args.length>2?MenuLayout.parse(args[2]):old.menuSlot();
                    put(old.displayed(WarpIcon.capture(player.getInventory().getItemInMainHand()),slot));
                    tell(sender,"Warp simgesi güncellendi: "+old.id()+(slot>=0?" • Menü: "+MenuLayout.label(slot):""));
                }
                case "sil" -> {need(args,2,"/warp sil <ad>");Warp warp=find(args[1]);Map<String,Warp> next=new LinkedHashMap<>(warps);next.remove(warp.id());save(next,warp.id());tell(sender,"Warp silindi: "+warp.id());}
                case "isim","aciklama" -> {
                    need(args,3,"/warp "+(action.equals("isim")?"isim":"açıklama")+" <ad> <metin>");Warp warp=find(args[1]);String value=String.join(" ",Arrays.copyOfRange(args,2,args.length));
                    put(action.equals("isim")?warp.renamed(value):warp.described(value));tell(sender,"Warp güncellendi: "+warp.id());
                }
                case "yetki" -> {
                    need(args,3,"/warp yetki <ad> özel|herkes");Warp warp=find(args[1]);String access=Warp.id(args[2]);
                    if(!Set.of("ozel","herkes").contains(access))throw new IllegalArgumentException("Erişim özel veya herkes olmalı.");
                    put(warp.restricted(access.equals("ozel")));tell(sender,access.equals("ozel")?"Warp özel oldu. Rolüne yetki ver: /rol yetki vip "+warp.permission()+" ver":"Warp herkese açıldı: "+warp.id());
                }
                case "gonder" -> {
                    need(args,3,"/warp gönder <oyuncu> <warp>");Player target=Bukkit.getPlayerExact(args[1]);if(target==null)throw new IllegalArgumentException("Oyuncu çevrimiçi değil.");
                    if(request(target,find(args[2])))tell(sender,target.getName()+" için ışınlanma başlatıldı.");else if(sender!=target)tell(sender,"Işınlanma başlatılamadı; oyuncuya nedeni bildirildi.");
                }
                case "yenile" -> {
                    Settings nextSettings=readSettings();Map<String,Warp> nextWarps=store.load();validateIcons(nextWarps);validatePlacement(nextWarps);settings=nextSettings;warps=nextWarps;regions=regions(warps);synchronizePermissions();
                    for(Pending travel:new ArrayList<>(pending.values()))cancel(travel.player,"Warp ayarları yenilendi; ışınlanma iptal edildi.");
                    refreshMenus();tell(sender,"Warp ayarları ve noktalar yenilendi. Kullanım arası süreler korunuyor.");
                }
            }
        }catch(IllegalArgumentException e){tell(sender,e.getMessage());}
        catch(IOException e){getLogger().log(Level.SEVERE,"Warp dosyası işlemi başarısız; mevcut veriler korundu.",e);tell(sender,"Dosya okunamadı veya kaydedilemedi; konsolu kontrol et.");}
        return true;
    }
    private Location destination(Warp warp,WarpPoint point) {
        World world=Bukkit.getWorld(warp.world());
        if(world==null)throw new IllegalArgumentException("Warpın dünyası yüklü değil; yöneticiye haber ver.");
        Location location=new Location(world,point.x(),point.y(),point.z(),point.yaw(),point.pitch());
        if(point.y()<world.getMinHeight()||point.y()>=world.getMaxHeight()||!world.getWorldBorder().isInside(location))throw new IllegalArgumentException("Warp konumu dünyanın sınırları dışında.");
        return location;
    }
    private boolean request(Player player,Warp warp) {
        if(!accessible(player,warp)){tell(player,"Bu warpa gitme yetkin yok.");return false;}
        if(player.isDead()||player.isInsideVehicle()||!player.getPassengers().isEmpty()){tell(player,"Işınlanmak için hayatta olmalı ve araçtan inmelisin.");return false;}
        if(pending.containsKey(player.getUniqueId())){tell(player,"Zaten bir ışınlanma hazırlanıyor. /warp iptal ile durdurabilirsin.");return false;}
        int remaining=cooldowns.remaining(player.getName());if(remaining>0){tell(player,"Tekrar ışınlanmak için "+remaining+" saniye bekle.");return false;}
        WarpPoint point=warp.choosePoint(ThreadLocalRandom.current());
        try{destination(warp,point);}catch(IllegalArgumentException e){tell(player,e.getMessage());return false;}
        Pending travel=new Pending(player,warp,point,settings.warmup());pending.put(player.getUniqueId(),travel);
        tell(player,warp.name()+" için ışınlanma başlatıldı. Sabit kal; iptal: /warp iptal");return true;
    }
    private boolean current(Pending travel){return isEnabled()&&travel.player.isOnline()&&pending.get(travel.player.getUniqueId())==travel;}
    private boolean moved(Pending travel,Location to){return !travel.origin.getWorld().equals(to.getWorld())||travel.origin.distanceSquared(to)>0.01;}
    private boolean cancel(Player player,String reason){Pending travel=pending.remove(player.getUniqueId());if(travel==null)return false;player.sendActionBar(Component.empty());tell(player,reason);return true;}
    private void tick() {
        long now=System.nanoTime();
        for(Pending travel:new ArrayList<>(pending.values())) {
            Player player=travel.player;
            if(!player.isOnline()){pending.remove(player.getUniqueId());continue;}
            if(player.isDead()||moved(travel,player.getLocation())){cancel(player,"Hareket ettiğin için ışınlanma iptal edildi.");continue;}
            if(travel.loading){if(now-travel.loadingDeadline>=0)cancel(player,"Nokta zamanında hazırlanamadı; tekrar dene.");continue;}
            long left=travel.ready-now;
            if(left<=0){loadDestination(travel);continue;}
            int seconds=(int)((left+999_999_999L)/1_000_000_000L);
            if(seconds!=travel.shown){travel.shown=seconds;player.sendActionBar(Component.text(travel.warp.name()+" • "+seconds+" saniye — Sabit kal",NamedTextColor.AQUA));}
        }
        if(++ticks%20==0){cooldowns.prune();refreshMenus();}
    }
    private void loadDestination(Pending travel) {
        Location target;
        try{target=destination(travel.warp,travel.point);}catch(IllegalArgumentException e){cancel(travel.player,e.getMessage());return;}
        travel.loading=true;travel.loadingDeadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        travel.player.sendActionBar(Component.text("Warp noktası hazırlanıyor…",NamedTextColor.AQUA));
        target.getWorld().getChunkAtAsync(target).whenComplete((chunk,error)->{
            if(!isEnabled())return;
            Bukkit.getScheduler().runTask(this,()->{
                if(!current(travel))return;
                Player player=travel.player;
                if(error!=null){getLogger().log(Level.WARNING,"Warp bölgesi yüklenemedi: "+travel.warp.id(),error);cancel(player,"Warp noktası hazırlanamadı; tekrar dene.");return;}
                if(!travel.warp.equals(warps.get(travel.warp.id()))||!accessible(player,travel.warp)||player.isDead()||player.isInsideVehicle()||!player.getPassengers().isEmpty()||moved(travel,player.getLocation())){cancel(player,"Konumun veya warp erişimi değişti; ışınlanma iptal edildi.");return;}
                try{destination(travel.warp,travel.point);}catch(IllegalArgumentException e){cancel(player,e.getMessage());return;}
                permittedTeleports.put(player.getUniqueId(),target.clone());boolean success;
                try{success=player.teleport(target,PlayerTeleportEvent.TeleportCause.PLUGIN);}
                finally{permittedTeleports.remove(player.getUniqueId());}
                pending.remove(player.getUniqueId());player.sendActionBar(Component.empty());
                if(success&&player.isOnline()){player.setFallDistance(0);cooldowns.start(player.getName(),settings.cooldown());tell(player,travel.warp.name()+" noktasına ışınlandın.");}
                else if(player.isOnline())tell(player,"Işınlanma tamamlanamadı veya başka bir eklenti tarafından engellendi.");
            });
        });
    }
    private void refreshMenus(){for(Player player:Bukkit.getOnlinePlayers())if(player.getOpenInventory().getTopInventory().getHolder() instanceof WarpMenu menu&&(!menu.visible.equals(visible(player))||!menu.reserved.equals(MenuLayout.reserved(warps.values()))))open(player,menu.page);}
    private void nextUi(Player player,WarpMenu menu,Runnable action){Bukkit.getScheduler().runTask(this,()->{if(player.isOnline()&&player.getOpenInventory().getTopInventory().getHolder()==menu)action.run();});}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void moved(PlayerMoveEvent event){Pending travel=pending.get(event.getPlayer().getUniqueId());if(travel!=null&&!permittedTeleports.containsKey(event.getPlayer().getUniqueId())&&event.getTo()!=null&&moved(travel,event.getTo()))cancel(event.getPlayer(),"Hareket ettiğin için ışınlanma iptal edildi.");}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent event){if(event.getEntity() instanceof Player player&&event.getFinalDamage()>0)cancel(player,"Hasar aldığın için ışınlanma iptal edildi.");}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent event){
        if(!pending.containsKey(event.getPlayer().getUniqueId()))return;
        Location allowed=permittedTeleports.get(event.getPlayer().getUniqueId());
        if(allowed!=null){if(event.getCause()!=PlayerTeleportEvent.TeleportCause.PLUGIN||!allowed.equals(event.getTo())){event.setCancelled(true);cancel(event.getPlayer(),"Işınlanma hedefi değişti; işlem iptal edildi.");}}
        else cancel(event.getPlayer(),"Başka yere ışınlandığın için warp işlemi iptal edildi.");
    }
    @EventHandler public void quit(PlayerQuitEvent event){pending.remove(event.getPlayer().getUniqueId());}
    @EventHandler public void death(PlayerDeathEvent event){cancel(event.getEntity(),"Işınlanma iptal edildi.");}
    @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent event){
        if(!(event.getView().getTopInventory().getHolder() instanceof WarpMenu menu))return;
        event.setCancelled(true);
        if(!(event.getWhoClicked() instanceof Player player)||!menu.owner.equals(player.getUniqueId())||(event.getClick()!=ClickType.LEFT&&event.getClick()!=ClickType.RIGHT))return;
        int slot=event.getRawSlot();
        if(slot==49){nextUi(player,menu,player::closeInventory);return;}
        if(slot==45&&menu.page>0){nextUi(player,menu,()->open(player,menu.page-1));return;}
        if(slot==53&&menu.page<menu.lastPage){nextUi(player,menu,()->open(player,menu.page+1));return;}
        String id=menu.destinations.get(slot);if(id==null)return;
        Warp warp=warps.get(id);if(warp==null){nextUi(player,menu,()->open(player,menu.page));return;}
        if(request(player,warp))nextUi(player,menu,player::closeInventory);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent event){if(event.getView().getTopInventory().getHolder() instanceof WarpMenu)event.setCancelled(true);}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        if(settings==null||args.length==0)return List.of();List<String> options=new ArrayList<>();boolean manager=admin(sender);
        if(args.length==1){if(sender instanceof Player player)options.addAll(visible(player).stream().map(Warp::id).toList());options.addAll(List.of("bilgi","iptal","yardım"));if(manager)options.addAll(List.of("oluştur","taşı","sil","isim","açıklama","simge","yetki","gönder","yenile","noktaekle","noktalar","noktasil"));}
        else{
            String action;try{action=Warp.id(args[0]);}catch(IllegalArgumentException e){return List.of();}
            if(args.length==2&&action.equals("gonder")&&manager)options.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            else if((args.length==2&&Set.of("bilgi","tasi","sil","isim","aciklama","simge","yetki","noktaekle","noktalar","noktasil").contains(action))||(args.length==3&&action.equals("gonder")&&manager)){
                if(manager)options.addAll(warps.keySet());else if(action.equals("bilgi")&&sender instanceof Player player)options.addAll(visible(player).stream().map(Warp::id).toList());
            }else if(args.length==3&&action.equals("yetki")&&manager)options.addAll(List.of("özel","herkes"));
            else if(args.length==3&&Set.of("olustur","simge").contains(action)&&manager){for(int row=1;row<=5;row++)for(int column=1;column<=9;column++)options.add(row+"-"+column);}
            else if(args.length==3&&action.equals("noktasil")&&manager){
                Warp warp;try{warp=warps.get(Warp.id(args[1]));}catch(IllegalArgumentException e){return List.of();}
                if(warp!=null)for(int i=1;i<=warp.points().size();i++)options.add(Integer.toString(i));
            }
        }
        String prefix;try{prefix=args[args.length-1].isEmpty()?"":Warp.id(args[args.length-1]);}catch(IllegalArgumentException e){return List.of();}
        return options.stream().filter(value->Warp.id(value).startsWith(prefix)).distinct().toList();
    }
}
