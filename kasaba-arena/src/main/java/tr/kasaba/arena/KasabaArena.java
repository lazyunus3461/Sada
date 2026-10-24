package tr.kasaba.arena;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scoreboard.*;
import tr.kasaba.hub.api.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** Main-thread arena state, room allocation and inventory ownership. */
public final class KasabaArena extends JavaPlugin implements Listener, CommandExecutor {
    private enum Place { LOBBY, XARENA, WAITING, MATCH }
    private record Region(UUID world,int minX,int maxX,int minY,int maxY,int minZ,int maxZ) {
        boolean overlaps(Region other){return world.equals(other.world)&&minX<=other.maxX&&maxX>=other.minX&&minY<=other.maxY&&maxY>=other.minY&&minZ<=other.maxZ&&maxZ>=other.minZ;}
        boolean contains(Location at){return at!=null&&at.getWorld()!=null&&world.equals(at.getWorld().getUID())&&at.getBlockX()>=minX&&at.getBlockX()<=maxX&&at.getBlockY()>=minY&&at.getBlockY()<=maxY&&at.getBlockZ()>=minZ&&at.getBlockZ()<=maxZ;}
    }
    private record ArenaRoom(String mode,int id,Location red,Location blue) {
        String key(){return mode+"-"+id;}
        int capacity(){return mode.equals("1v1")?2:4;}
    }
    private static final class Match {
        final ArenaRoom arena; final String code; final UUID host;
        final List<UUID> red=new ArrayList<>(),blue=new ArrayList<>();final Set<UUID> down=new HashSet<>();
        int round=1,redScore,blueScore;boolean started,paused=true,finished;
        Match(ArenaRoom arena,String code,UUID host){this.arena=arena;this.code=code;this.host=host;}
        List<UUID> players(){List<UUID> all=new ArrayList<>(red);all.addAll(blue);return all;}
        boolean sameSide(UUID a,UUID b){return red.contains(a)&&red.contains(b)||blue.contains(a)&&blue.contains(b);}
        List<UUID> team(UUID id){return red.contains(id)?red:blue;}
    }
    private record Button(String label,java.util.function.Consumer<io.papermc.paper.dialog.DialogResponseView> action){}
    private final Map<UUID,Place> places=new HashMap<>();
    private final Map<UUID,ArenaCharacter> roles=new HashMap<>();
    private final Map<UUID,Match> matches=new HashMap<>();
    private final Map<String,ArenaRoom> arenas=new TreeMap<>();
    private final Map<String,Match> occupied=new HashMap<>(),codes=new HashMap<>();
    private final Map<String,Region> regions=new HashMap<>();
    private final Map<UUID,Location> selectionOne=new HashMap<>(),selectionTwo=new HashMap<>();
    private final Map<UUID,Integer> lastXSpawn=new HashMap<>(),kills=new HashMap<>();
    private final Map<UUID,String> names=new HashMap<>();
    private final Map<UUID,Long> pageTokens=new HashMap<>();
    private final Map<UUID,Scoreboard> previousBoards=new HashMap<>();
    private final Map<UUID,List<String>> boardEntries=new HashMap<>();
    private long pageSequence;
    private Location lobbySpawn;private final List<Location> xSpawns=new ArrayList<>();
    private HubApi hub;private XCombat combat;private ArenaMarket market;private NamespacedKey menuKey,abilityKey;
    private YamlConfiguration snapshots;private java.io.File snapshotFile;

    @Override public void onEnable(){
        saveDefaultConfig();menuKey=new NamespacedKey(this,"arena-menu");abilityKey=new NamespacedKey(this,"arena-ability");
        snapshotFile=new java.io.File(getDataFolder(),"oyuncu-yedekleri.yml");snapshots=YamlConfiguration.loadConfiguration(snapshotFile);
        loadLocations();loadStats();Objects.requireNonNull(getCommand("arena")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this,this);
        market=new ArenaMarket(this,p->places.get(p.getUniqueId())==Place.XARENA);
        combat=new XCombat(this,p->places.get(p.getUniqueId())==Place.XARENA,p->roles.getOrDefault(p.getUniqueId(),ArenaCharacter.WARRIOR),market::meleeBonus);
        hub=Bukkit.getServicesManager().load(HubApi.class);
        if(hub==null){getLogger().severe("KasabaHub API bulunamadı.");Bukkit.getPluginManager().disablePlugin(this);return;}
        hub.register(this,new ArenaGame());
        Bukkit.getScheduler().runTaskTimer(this,this::refreshScoreboards,40,Math.max(20,getConfig().getLong("ayarlar.skor-tahtasi-guncelleme-tik",40)));
        Bukkit.getScheduler().runTaskTimer(this,this::refreshCompasses,20,40);
        // Multiverse may load the saved arena world after this plugin has enabled.
        Bukkit.getScheduler().runTaskLater(this,this::loadLocations,40);
        for(Player p:Bukkit.getOnlinePlayers())recoverSnapshot(p);
    }
    @Override public void onDisable(){
        if(hub!=null)try{hub.unregister(this,"arena");}catch(RuntimeException ex){getLogger().log(Level.WARNING,"Arena merkezden çıkarılamadı.",ex);}
        for(Player p:Bukkit.getOnlinePlayers()){if(combat!=null)combat.clear(p);clearScoreboard(p);removeMenuCompass(p);restoreSnapshot(p);}
        saveStats();saveSnapshots();
    }
    private final class ArenaGame implements HubGame {
        public GameInfo info(){return new GameInfo("arena","Arena","X Arena • beşer odalı 1v1 ve 2v2",1,20);}
        public boolean available(){if(!valid(lobbySpawn))loadLocations();return valid(lobbySpawn);}
        public boolean join(List<Player> team){
            if(!available()||team.isEmpty()||team.stream().anyMatch(p->places.containsKey(p.getUniqueId())))return false;
            Map<Player,Location> before=new LinkedHashMap<>();
            try{
                for(Player p:team){saveSnapshot(p);before.put(p,p.getLocation());resetFighter(p,true);if(!p.teleport(lobbySpawn,PlayerTeleportEvent.TeleportCause.PLUGIN))throw new IllegalStateException("Işınlanma engellendi");places.put(p.getUniqueId(),Place.LOBBY);giveMenuCompass(p);}
                for(Player p:team)tell(p,"Arena lobisine geldin. Menü: pusula veya /arena • Çıkış: /lobi");return true;
            }catch(RuntimeException ex){for(var entry:before.entrySet()){Player p=entry.getKey();places.remove(p.getUniqueId());restoreSnapshot(p);p.teleport(entry.getValue(),PlayerTeleportEvent.TeleportCause.PLUGIN);}getLogger().log(Level.WARNING,"Arena girişi geri alındı.",ex);return false;}
        }
        public boolean lobby(UUID id){return places.get(id)==Place.LOBBY||places.get(id)==Place.WAITING;}
        public void leave(UUID id){cleanupPlayer(id);}
    }
    private String normalize(String value){return value.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    private String mode(String raw){return switch(normalize(raw)){case "1v1","1vs1"->"1v1";case "2v2","2vs2"->"2v2";default->throw new IllegalArgumentException("Oyun türü 1v1 veya 2v2 olmalı.");};}
    private int roomId(String raw){try{int id=Integer.parseInt(raw);if(id>=1&&id<=5)return id;}catch(NumberFormatException ignored){}throw new IllegalArgumentException("Oda numarası 1 ile 5 arasında olmalı.");}
    private void admin(Player p){if(!p.hasPermission("kasabaarena.yonet")&&!p.hasPermission("kasabaroles.admin"))throw new IllegalArgumentException("Arena yönetim yetkin yok.");}
    private void tell(Player p,String message){p.sendMessage(Component.text(message,NamedTextColor.YELLOW));}
    private void requireLobby(Player p){if(places.get(p.getUniqueId())!=Place.LOBBY)throw new IllegalArgumentException("Önce /oyunlar git arena ile Arena lobisine gir.");}
    private String kit(Player p){return "normal";}
    private void invalidatePage(Player p){pageTokens.remove(p.getUniqueId());p.closeDialog();}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        if(!(sender instanceof Player p)){sender.sendMessage("Bu komut oyunda kullanılmalı.");return true;}
        try{
            if(args.length==0){openMenu(p);return true;}String action=normalize(args[0]);
            if(action.equals("yardim")){tell(p,"/arena • /arena 1v1|2v2 • /arena oda oluştur|liste • /arena katıl <kod> • /arena xarena • /arena yetenek 1|2|3|4 • /arena lobi");return true;}
            if(action.equals("yenile")){admin(p);if(!occupied.isEmpty())throw new IllegalArgumentException("Odalar boşalınca ayarları yenile.");reloadConfig();loadLocations();loadStats();tell(p,valid(lobbySpawn)?"Arena kayıtları yeniden okundu; lobi hazır.":"Kayıtlar korundu. Lobi dünyası yüklenmemiş veya spawns.lobi ayarı eksik. /arena durum ile kontrol et.");return true;}
            if(action.equals("durum")){admin(p);Object raw=getConfig().get("spawns.lobi");tell(p,valid(lobbySpawn)?"Arena hazır • "+lobbySpawn.getWorld().getName()+" • "+lobbySpawn.getBlockX()+", "+lobbySpawn.getBlockY()+", "+lobbySpawn.getBlockZ():raw==null?"Lobi kaydı bulunamadı. plugins/KasabaArena/config.yml ve yedeğini kontrol et. Yeni JAR mevcut config'i silmez.":"Lobi kaydı dosyada var; dünya henüz yüklenmemiş veya konum geçersiz. /arena yenile kullan.");return true;}
            if(action.equals("lobi")&&args.length>1&&normalize(args[1]).equals("ayarla")){admin(p);writeLocation("spawns.lobi",p.getLocation());saveSettings();tell(p,"Arena güvenli lobi noktası ayarlandı.");return true;}
            if(action.equals("dogma")){spawnCommand(p,args);return true;}
            if(action.equals("bolge")){regionCommand(p,args);return true;}
            if(action.equals("kit")){kitCommand(p,args);return true;}
            if(action.equals("market")){marketCommand(p,args);return true;}
            if(!places.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Önce /oyunlar git arena ile Arena lobisine gir.");
            switch(action){
                case "xarena","x"->joinX(p);
                case "lobi"->arenaLobby(p);
                case "cikis"->hub.returnToLobby(p.getUniqueId());
                case "1v1","1vs1","2v2","2vs2"->joinPublic(p,mode(action));
                case "oda"->{if(args.length>1&&normalize(args[1]).equals("olustur"))createRoom(p);else showRooms(p);}
                case "katil"->{if(args.length<2)throw new IllegalArgumentException("/arena katıl <kod>");joinCode(p,args[1]);}

                case "vip"->throw new IllegalArgumentException("VIP kiti kaldırıldı. Maçlarda normal kit kullanılır.");
                case "sinif","karakter"->{if(args.length<2)showClasses(p);else chooseRole(p,ArenaCharacter.parse(args[1]));}
                case "yetenek"->{int number=1;try{if(args.length>1)number=Integer.parseInt(args[1]);}catch(NumberFormatException ex){throw new IllegalArgumentException("/arena yetenek 1|2|3|4");}combat.use(p,number);}
                default->openMenu(p);
            }
        }catch(IllegalArgumentException ex){tell(p,ex.getMessage());}return true;
    }
    private boolean valid(Location l){return l!=null&&l.getWorld()!=null&&l.getY()>=l.getWorld().getMinHeight()&&l.getY()<l.getWorld().getMaxHeight()&&l.getWorld().getWorldBorder().isInside(l);}
    private Location readLocation(ConfigurationSection section){return section==null?null:readLocation(section.getValues(false));}
    private Location readLocation(Map<?,?> map){try{World world=null;try{world=Bukkit.getWorld(UUID.fromString(String.valueOf(map.get("world"))));}catch(IllegalArgumentException ignored){}if(world==null)world=Bukkit.getWorld(String.valueOf(map.get("worldName")));if(world==null)return null;return new Location(world,number(map.get("x")),number(map.get("y")),number(map.get("z")),(float)number(map.get("yaw")),(float)number(map.get("pitch")));}catch(RuntimeException ex){return null;}}
    private double number(Object value){return value instanceof Number n?n.doubleValue():0;}
    private Map<String,Object> locationMap(Location l){Map<String,Object> m=new LinkedHashMap<>();m.put("world",l.getWorld().getUID().toString());m.put("worldName",l.getWorld().getName());m.put("x",l.getX());m.put("y",l.getY());m.put("z",l.getZ());m.put("yaw",l.getYaw());m.put("pitch",l.getPitch());return m;}
    private Location configuredLocation(String path){Object value=getConfig().get(path);if(value instanceof ConfigurationSection section)return readLocation(section);if(value instanceof Map<?,?> map)return readLocation(map);return null;}
    private void writeLocation(String path,Location l){getConfig().createSection(path,locationMap(l));}
    private void loadLocations(){
        lobbySpawn=configuredLocation("spawns.lobi");xSpawns.clear();
        for(Object raw:getConfig().getList("spawns.xarena",List.of()))if(raw instanceof Map<?,?> map){Location at=readLocation(map);if(valid(at))xSpawns.add(at);}
        if(!getConfig().contains("odalar.1v1.1.kirmizi")&&getConfig().contains("spawns.1v1.kirmizi")){
            Location red=configuredLocation("spawns.1v1.kirmizi"),blue=configuredLocation("spawns.1v1.mavi");
            if(valid(red))writeLocation("odalar.1v1.1.kirmizi",red);if(valid(blue))writeLocation("odalar.1v1.1.mavi",blue);saveConfig();
        }
        arenas.clear();for(String type:List.of("1v1","2v2"))for(int id=1;id<=5;id++){String path="odalar."+type+"."+id;Location red=configuredLocation(path+".kirmizi"),blue=configuredLocation(path+".mavi");if(valid(red)&&valid(blue)&&red.getWorld().equals(blue.getWorld())){ArenaRoom room=new ArenaRoom(type,id,red,blue);arenas.put(room.key(),room);}}
        regions.clear();ConfigurationSection section=getConfig().getConfigurationSection("regions");if(section!=null)for(String key:section.getKeys(false)){ConfigurationSection r=section.getConfigurationSection(key);if(r!=null)try{regions.put(key,new Region(UUID.fromString(r.getString("world","")),r.getInt("minX"),r.getInt("maxX"),r.getInt("minY"),r.getInt("maxY"),r.getInt("minZ"),r.getInt("maxZ")));}catch(IllegalArgumentException ex){getLogger().warning("Geçersiz bölge: "+key);}}
    }
    private void saveSettings(){saveConfig();loadLocations();}
    @EventHandler public void worldReady(org.bukkit.event.world.WorldLoadEvent e){Bukkit.getScheduler().runTask(this,this::loadLocations);}
    private void spawnCommand(Player p,String[] a){
        admin(p);if(a.length<2)throw new IllegalArgumentException("/arena doğma ekle xarena • /arena doğma ayarla 1v1|2v2 <1-5> kırmızı|mavi");String op=normalize(a[1]);
        if(a.length>2&&normalize(a[2]).equals("xarena")){
            if(op.equals("ekle")){xSpawns.add(p.getLocation());getConfig().set("spawns.xarena",xSpawns.stream().map(this::locationMap).toList());saveSettings();tell(p,"X Arena doğma noktası eklendi. Toplam: "+xSpawns.size());return;}
            if(op.equals("sil")){if(xSpawns.isEmpty())throw new IllegalArgumentException("Silinecek nokta yok.");xSpawns.removeLast();getConfig().set("spawns.xarena",xSpawns.stream().map(this::locationMap).toList());saveSettings();tell(p,"Son X Arena doğma noktası silindi.");return;}
            if(op.equals("liste")){tell(p,"Kayıtlı X Arena doğma noktası: "+xSpawns.size());return;}
        }
        if(op.equals("ayarla")&&a.length>=3){String type="1v1",side=normalize(a[2]);int id=1;if(a.length>=5){type=mode(a[2]);id=roomId(a[3]);side=normalize(a[4]);}side=switch(side){case "kirmizi","red"->"kirmizi";case "mavi","blue"->"mavi";default->throw new IllegalArgumentException("/arena doğma ayarla 1v1|2v2 <1-5> kırmızı|mavi");};if(roomNumberOccupied(id))throw new IllegalArgumentException("Bu ortak alan kullanılırken noktasını değiştiremezsin.");writeLocation("odalar."+type+"."+id+"."+side,p.getLocation());saveSettings();tell(p,type+" oda "+id+" • "+side+" doğma noktası kaydedildi.");return;}
        throw new IllegalArgumentException("/arena doğma ekle xarena • /arena doğma ayarla 1v1|2v2 <1-5> kırmızı|mavi");
    }
    private String regionKey(String[] a){
        if(a.length<3)throw new IllegalArgumentException("/arena bölge kaydet lobi|xarena|ortak|1v1|2v2 [1-5] [alan-adı]");
        String type=normalize(a[2]),key;int label;
        if(Set.of("lobi","xarena").contains(type)){key=type;label=3;}
        else{if(type.equals("arena"))type="ortak";if(!type.equals("ortak"))type=mode(type);key=type+"-"+(a.length>3?roomId(a[3]):1);label=4;}
        if(a.length>label){String part=normalize(a[label]);if(!part.matches("[a-z0-9_-]{1,32}"))throw new IllegalArgumentException("Alan adı harf, rakam, alt çizgi veya tire olmalı.");key+="__"+part;}
        return key;
    }
    private void regionCommand(Player p,String[] a){
        admin(p);if(a.length<2)throw new IllegalArgumentException("/arena bölge pos1|pos2|kaydet|sil|liste");String op=normalize(a[1]);
        if(op.equals("pos")&&a.length>2)op="pos"+a[2];
        if(op.equals("pos1")){selectionOne.put(p.getUniqueId(),p.getLocation().clone());tell(p,"Birinci köşe seçildi: "+p.getWorld().getName());return;}
        if(op.equals("pos2")){selectionTwo.put(p.getUniqueId(),p.getLocation().clone());tell(p,"İkinci köşe seçildi: "+p.getWorld().getName());return;}
        if(op.equals("liste")){if(regions.isEmpty())tell(p,"Henüz korunan alan yok.");else for(String key:new TreeSet<>(regions.keySet()))tell(p,"Korunan alan: "+key);return;}
        if(!Set.of("kaydet","sil").contains(op))throw new IllegalArgumentException("/arena bölge pos1|pos2|kaydet|sil|liste");
        if(!occupied.isEmpty())throw new IllegalArgumentException("Koruma alanlarını odalar boşken düzenle.");String key=regionKey(a),path="regions."+key;
        if(op.equals("sil")){if(!regions.containsKey(key))throw new IllegalArgumentException("Böyle bir koruma alanı yok: "+key);getConfig().set(path,null);saveSettings();tell(p,"Koruma kaldırıldı: "+key);return;}
        Location one=selectionOne.get(p.getUniqueId()),two=selectionTwo.get(p.getUniqueId());if(one==null||two==null)throw new IllegalArgumentException("Önce /arena bölge pos1 ve /arena bölge pos2 ile iki köşeyi seç.");if(!one.getWorld().equals(two.getWorld()))throw new IllegalArgumentException("İki köşe farklı dünyada. Aynı dünyada yeniden seç.");
        getConfig().set(path+".world",one.getWorld().getUID().toString());getConfig().set(path+".minX",Math.min(one.getBlockX(),two.getBlockX()));getConfig().set(path+".maxX",Math.max(one.getBlockX(),two.getBlockX()));getConfig().set(path+".minY",Math.min(one.getBlockY(),two.getBlockY()));getConfig().set(path+".maxY",Math.max(one.getBlockY(),two.getBlockY()));getConfig().set(path+".minZ",Math.min(one.getBlockZ(),two.getBlockZ()));getConfig().set(path+".maxZ",Math.max(one.getBlockZ(),two.getBlockZ()));saveSettings();tell(p,"Blok kırma/koyma koruması kaydedildi: "+key);
    }
    private ItemStack clean(ItemStack item){return item==null||isMenu(item)||item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("kasabahub","lobi-menusu"))||combat.temporary(item)||market.marked(item)?null:item.clone();}
    private ItemStack[] clean(ItemStack[] items){return Arrays.stream(items).map(this::clean).toArray(ItemStack[]::new);}
    private ItemStack[] readItems(String path,int size){ItemStack[] result=new ItemStack[size];List<?> raw=getConfig().getList(path,List.of());for(int i=0;i<Math.min(size,raw.size());i++)if(raw.get(i) instanceof ItemStack item)result[i]=clean(item);return result;}
    private void kitCommand(Player p,String[] a){admin(p);if(a.length<3||!normalize(a[2]).equals("kaydet")||!normalize(a[1]).equals("normal"))throw new IllegalArgumentException("Envanterini hazırla: /arena kit normal kaydet. VIP kiti kaldırıldı.");if(places.get(p.getUniqueId())==Place.XARENA||places.get(p.getUniqueId())==Place.MATCH)throw new IllegalArgumentException("Kitini Arena oyunu dışındayken hazırla.");String path="kits."+normalize(a[1]);getConfig().set(path+".items",Arrays.asList(clean(p.getInventory().getStorageContents())));getConfig().set(path+".armor",Arrays.asList(clean(p.getInventory().getArmorContents())));getConfig().set(path+".offhand",clean(p.getInventory().getItemInOffHand()));saveConfig();tell(p,"Kit, zırh ve ikinci el eşyası kaydedildi: "+a[1]);}
    private double maxHealth(Player p){var attr=p.getAttribute(Attribute.MAX_HEALTH);return attr==null?20:attr.getValue();}
    private void resetFighter(Player p,boolean inventory){combat.clear(p);for(PotionEffect effect:p.getActivePotionEffects())p.removePotionEffect(effect.getType());p.setAbsorptionAmount(0);p.setFireTicks(0);p.setFallDistance(0);p.setInvulnerable(false);p.setNoDamageTicks(0);p.setGameMode(GameMode.SURVIVAL);p.setItemOnCursor(null);if(inventory){p.getInventory().clear();p.getInventory().setArmorContents(new ItemStack[4]);p.getInventory().setItemInOffHand(null);}if(!p.isDead())p.setHealth(Math.min(20,maxHealth(p)));p.setFoodLevel(20);p.setSaturation(10);}
    private void applyKit(Player p){
        resetFighter(p,true);String selected=kit(p),path="kits."+selected;
        if(getConfig().isList(path+".items")){p.getInventory().setStorageContents(readItems(path+".items",36));p.getInventory().setArmorContents(readItems(path+".armor",4));p.getInventory().setItemInOffHand(clean(getConfig().getItemStack(path+".offhand")));}
        else{p.getInventory().setItem(0,new ItemStack(Material.IRON_SWORD));p.getInventory().setItem(1,new ItemStack(Material.COOKED_BEEF,16));p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));}
        if(NetworkFeatures.isVip(p)){Material[] types={Material.NETHERITE_BOOTS,Material.NETHERITE_LEGGINGS,Material.NETHERITE_CHESTPLATE,Material.NETHERITE_HELMET};ItemStack[] armor=new ItemStack[4];for(int i=0;i<4;i++){armor[i]=new ItemStack(types[i]);armor[i].addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.PROTECTION,3);}p.getInventory().setArmorContents(armor);}
        p.getInventory().setHeldItemSlot(0);
    }
    private boolean belongsToRoom(String key,ArenaRoom room){String own=room.key(),shared="ortak-"+room.id();return key.equals(own)||key.startsWith(own+"__")||key.equals(shared)||key.startsWith(shared+"__");}
    private boolean roomsConflict(ArenaRoom one,ArenaRoom two){
        if(one.key().equals(two.key()))return true;
        if(getConfig().getBoolean("ayarlar.ortak-oda-numaralari",true)&&one.id()==two.id())return true;
        for(var a:regions.entrySet())if(belongsToRoom(a.getKey(),one))for(var b:regions.entrySet())if(belongsToRoom(b.getKey(),two)&&a.getValue().overlaps(b.getValue()))return true;
        return false;
    }
    private boolean roomNumberOccupied(int id){return occupied.values().stream().anyMatch(m->m.arena.id()==id);}
    private Match blockingMatch(ArenaRoom room){return occupied.values().stream().filter(match->roomsConflict(room,match.arena)).findFirst().orElse(null);}
    private ArenaRoom freeArena(String type){return arenas.values().stream().filter(r->r.mode().equals(type)&&blockingMatch(r)==null).findFirst().orElseThrow(()->new IllegalArgumentException("Hazır ve boş "+type+" alanı yok. Aynı alanı kullanan 1v1/2v2 odası boşalınca tekrar dene."));}
    private void joinPublic(Player p,String type){requireLobby(p);Match match=occupied.values().stream().filter(m->m.arena.mode().equals(type)&&m.code==null&&!m.started&&!m.finished&&m.players().size()<m.arena.capacity()).sorted(Comparator.comparingInt(m->m.arena.id())).findFirst().orElse(null);if(match==null){match=new Match(freeArena(type),null,p.getUniqueId());occupied.put(match.arena.key(),match);}enterRoom(p,match);}
    private void createRoom(Player p){requireLobby(p);ArenaRoom room=freeArena("1v1");String code;do{code=String.format(Locale.ROOT,"%06d",ThreadLocalRandom.current().nextInt(1_000_000));}while(codes.containsKey(code));Match match=new Match(room,code,p.getUniqueId());codes.put(code,match);occupied.put(room.key(),match);enterRoom(p,match);tell(p,"Özel oda kodun: "+code+" • Arkadaşın /arena katıl "+code+" yazsın.");}
    private void joinCode(Player p,String code){requireLobby(p);Match match=codes.get(code);if(match==null||match.started||match.finished)throw new IllegalArgumentException("Bu oda kodu yok, oda doldu veya maç başladı.");enterRoom(p,match);}
    private Location spawn(Match m,UUID id){List<UUID> team=m.team(id);Location base=(m.red.contains(id)?m.arena.red():m.arena.blue()).clone();if(team.size()>1){double offset=(team.indexOf(id)-(team.size()-1)/2.0)*1.2;base.add(offset,0,0);}return base;}
    private void enterRoom(Player p,Match match){
        UUID id=p.getUniqueId();if(match.started||match.finished||match.players().size()>=match.arena.capacity())throw new IllegalArgumentException("Bu oda dolu.");
        invalidatePage(p);resetFighter(p,true);if(match.red.size()<=match.blue.size())match.red.add(id);else match.blue.add(id);matches.put(id,match);places.put(id,Place.WAITING);
        if(!p.teleport(spawn(match,id),PlayerTeleportEvent.TeleportCause.PLUGIN)){detachRoom(id);places.put(id,Place.LOBBY);giveMenuCompass(p);throw new IllegalArgumentException("Odaya ışınlanma engellendi.");}
        for(UUID member:match.players()){Player player=Bukkit.getPlayer(member);if(player!=null)tell(player,match.arena.mode()+" oda "+match.arena.id()+" • "+match.players().size()+"/"+match.arena.capacity()+" oyuncu. Kit seçimi: /arena");}
        if(match.players().size()==match.arena.capacity())startRound(match);
    }
    private void startRound(Match match){
        if(match.finished)return;match.started=true;match.paused=true;match.down.clear();int round=match.round;
        for(UUID id:match.players()){Player p=Bukkit.getPlayer(id);if(p==null||!p.isOnline()){closeRoom(match,"Oyuncu ayrıldı; oda kapandı.");return;}places.put(id,Place.MATCH);invalidatePage(p);if(p.isDead())p.spigot().respawn();applyKit(p);p.teleport(spawn(match,id),PlayerTeleportEvent.TeleportCause.PLUGIN);tell(p,"Raunt "+round+" • 4 saniye sonra başlayacak. Seçilen kit: "+kit(p));}
        Bukkit.getScheduler().runTaskLater(this,()->{if(match.finished||match.round!=round)return;match.paused=false;for(UUID id:match.players()){Player p=Bukkit.getPlayer(id);if(p!=null&&matches.get(id)==match){p.setInvulnerable(false);p.setNoDamageTicks(0);p.sendTitle("§6RAUNT "+round,"§f"+match.arena.mode()+" • Oda "+match.arena.id(),5,35,10);}}},80);
    }
    private void winRound(Match match,boolean redWins){if(match.finished||match.paused)return;match.paused=true;if(redWins)match.redScore++;else match.blueScore++;for(UUID id:match.players()){Player p=Bukkit.getPlayer(id);if(p!=null)tell(p,(redWins?"Kırmızı":"Mavi")+" takım rauntu aldı. Skor: "+match.redScore+" - "+match.blueScore);}if(match.redScore>=2||match.blueScore>=2||match.round>=3){finishMatch(match);return;}match.round++;Bukkit.getScheduler().runTaskLater(this,()->startRound(match),60);}
    private void finishMatch(Match match){if(match.finished)return;match.finished=true;match.paused=true;for(UUID id:match.players()){Player p=Bukkit.getPlayer(id);if(p!=null){resetFighter(p,true);p.sendTitle("§6MAÇ BİTTİ","§f"+(match.redScore>match.blueScore?"Kırmızı":"Mavi")+" kazandı • "+match.redScore+" - "+match.blueScore,5,50,15);}}Bukkit.getScheduler().runTaskLater(this,()->closeRoom(match,"Maç bitti; Arena lobisine döndün."),60);}
    private void closeRoom(Match match,String message){match.finished=true;match.paused=true;occupied.remove(match.arena.key(),match);if(match.code!=null)codes.remove(match.code,match);for(UUID id:match.players()){if(matches.get(id)!=match)continue;matches.remove(id);Player p=Bukkit.getPlayer(id);if(p!=null&&p.isOnline()&&places.containsKey(id)){lobbySelf(p);tell(p,message);}}}
    private void detachRoom(UUID id){Match match=matches.remove(id);if(match==null)return;match.red.remove(id);match.blue.remove(id);match.down.remove(id);if(match.started||match.code!=null&&match.host.equals(id)){closeRoom(match,"Oyuncu ayrıldı; oda kapandı.");}else if(match.players().isEmpty()){occupied.remove(match.arena.key(),match);if(match.code!=null)codes.remove(match.code,match);}}
    private void joinX(Player p){requireLobby(p);if(xSpawns.isEmpty())throw new IllegalArgumentException("X Arena doğma noktaları ayarlanmamış.");invalidatePage(p);places.put(p.getUniqueId(),Place.XARENA);teleportRandom(p);loadX(p);updateScoreboard(p);tell(p,"X Arena: beceriler 1–4, yakın dövüş silahı 5. yuvada. Hazır olan her beceriyi istediğin sırada kullan. Okçu yayı 6. yuvada.");}
    private Location randomSpawn(Player p){int count=xSpawns.size(),previous=lastXSpawn.getOrDefault(p.getUniqueId(),-1),index=ThreadLocalRandom.current().nextInt(count);if(count>1&&index==previous)index=(index+1+ThreadLocalRandom.current().nextInt(count-1))%count;lastXSpawn.put(p.getUniqueId(),index);return xSpawns.get(index).clone();}
    private void teleportRandom(Player p){p.teleport(randomSpawn(p),PlayerTeleportEvent.TeleportCause.PLUGIN);p.setFallDistance(0);}
    private void arenaLobby(Player p){detachRoom(p.getUniqueId());lobbySelf(p);}
    private void lobbySelf(Player p){if(!places.containsKey(p.getUniqueId())||!valid(lobbySpawn))return;invalidatePage(p);places.put(p.getUniqueId(),Place.LOBBY);clearScoreboard(p);resetFighter(p,true);if(p.isDead())p.spigot().respawn();p.teleport(lobbySpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);giveMenuCompass(p);}
    private void showRooms(Player p){List<String> lines=new ArrayList<>();for(String type:List.of("1v1","2v2"))for(int id=1;id<=5;id++){String key=type+"-"+id;Match m=occupied.get(key);lines.add(type+" oda "+id+": "+(!arenas.containsKey(key)?"Ayarlanmamış":m==null?(blockingMatch(arenas.get(key))==null?"Boş":"Ortak alan kullanımda"):m.code!=null?"Özel oda":m.started?"Maç sürüyor":m.players().size()+"/"+m.arena.capacity()+" oyuncu"));}show(p,"Arena odaları",lines,List.of(),List.of(button("1v1",()->joinPublic(p,"1v1")),button("2v2",()->joinPublic(p,"2v2")),button("Geri",()->openMenu(p))));}
    private void openMenu(Player p){
        Place place=places.get(p.getUniqueId());if(place==null){tell(p,"Önce /oyunlar git arena yaz.");return;}List<Button> buttons=new ArrayList<>();List<String> lines=new ArrayList<>();
        if(place==Place.XARENA){lines.add("Karakter: "+roles.getOrDefault(p.getUniqueId(),ArenaCharacter.WARRIOR).label+" • dört beceri • düşük kılıç hasarı");buttons.add(button("Becerilerim",()->showSkills(p)));buttons.add(button("Karakter seç",()->showClasses(p)));buttons.add(button("Arena lobisine dön",()->arenaLobby(p)));}
        else{
            lines.add("1v1 ve 2v2: boş yeri olan odaya doğrudan girersin; oda dolunca maç başlar.");lines.add("Maç silahları normal kittendir. VIP zırhı netherit Korunma III olur; karakter ve beceriler X Arena'ya aittir.");lines.add("Seçili maç kiti: "+kit(p));Match match=matches.get(p.getUniqueId());if(match!=null)lines.add(match.arena.mode()+" • Oda "+match.arena.id()+" • "+match.players().size()+"/"+match.arena.capacity());
            if(place==Place.LOBBY){buttons.add(button("X Arena",()->joinX(p)));buttons.add(button("1v1",()->joinPublic(p,"1v1")));buttons.add(button("2v2",()->joinPublic(p,"2v2")));buttons.add(button("Odaları gör",()->showRooms(p)));buttons.add(button("Özel 1v1 kodu oluştur",()->createRoom(p)));buttons.add(new Button("Oda kodu",response->{String code=response.getText("kod");if(code==null||!code.matches("[0-9]{6}"))throw new IllegalArgumentException("Altı haneli oda kodunu yaz.");joinCode(p,code);}));buttons.add(button("X Arena karakteri",()->showClasses(p)));}

            if(place!=Place.LOBBY)buttons.add(button("Arena lobisine dön",()->arenaLobby(p)));
        }
        buttons.add(button("Lobi",()->hub.returnToLobby(p.getUniqueId())));buttons.add(button("Burada kal",()->{}));List<DialogInput> inputs=place==Place.LOBBY?List.of(DialogInput.text("kod",Component.text("Özel oda kodu",NamedTextColor.WHITE)).maxLength(6).width(220).build()):List.of();show(p,"Kasaba • Arena",lines,inputs,buttons);
    }
    private void loadX(Player p){combat.loadout(p);market.render(p);}
    private void marketCommand(Player p,String[] a){
        if(a.length==1){showMarket(p);return;}String op=normalize(a[1]);
        if(op.equals("ekle")){admin(p);if(a.length<5)throw new IllegalArgumentException("Eline ürünü al: /arena market ekle <ad> <fiyat> <gün> [adet]. Gün 0: tüketilene kadar.");if(places.get(p.getUniqueId())==Place.XARENA||places.get(p.getUniqueId())==Place.MATCH)throw new IllegalArgumentException("Market ürününü Arena oyunu dışında hazırla.");try{market.add(p,a[2],a[3],Integer.parseInt(a[4]),a.length>5?Integer.parseInt(a[5]):1);}catch(NumberFormatException ex){throw new IllegalArgumentException("Gün ve adet tam sayı olmalı.");}tell(p,"X Arena market ürünü kaydedildi: "+a[2]);return;}
        if(op.equals("sil")){admin(p);if(a.length<3)throw new IllegalArgumentException("/arena market sil <ad>");market.remove(a[2]);tell(p,"Ürün listeden kaldırıldı; mevcut satın alımlar korunur.");return;}
        if(op.equals("al")&&a.length>2){ArenaMarket.Product product=market.products().stream().filter(item->item.id().equals(a[2])).findFirst().orElseThrow(()->new IllegalArgumentException("Ürün bulunamadı."));confirmProduct(p,product);return;}
        throw new IllegalArgumentException("/arena market • Yönetim: /arena market ekle <ad> <fiyat> <gün> [adet]");
    }
    private void showMarket(Player p){showMarket(p,0);}
    private void showMarket(Player p,int requested){List<ArenaMarket.Product> products=market.products();int last=Math.max(0,(products.size()-1)/8),page=Math.clamp(requested,0,last);List<String> lines=new ArrayList<>(List.of("Satın alınanlar yalnızca X Arena'ya girince verilir. 1v1/2v2'de kullanılamaz.","Süreli eşyaların süresi gerçek zamanla işler; yenebilir eşyalarda kullanılan adet azalır.",market.balance(p)));List<Button> buttons=new ArrayList<>();if(products.isEmpty())lines.add("Henüz ürün eklenmedi. Yönetici elindeki eşyayı market ürünü olarak kaydedebilir.");for(int index=page*8;index<Math.min(products.size(),page*8+8);index++){ArenaMarket.Product product=products.get(index);lines.add(product.id()+": "+product.description());buttons.add(button(product.id()+" • "+product.price().toPlainString(),()->confirmProduct(p,product)));}if(page>0)buttons.add(button("Önceki",()->showMarket(p,page-1)));if(page<last)buttons.add(button("Sonraki",()->showMarket(p,page+1)));buttons.add(button("Geri",()->openMenu(p)));buttons.add(button("Burada kal",()->{}));show(p,"Kasaba • X Arena Market",lines,List.of(),buttons);}
    private void confirmProduct(Player p,ArenaMarket.Product product){show(p,"Satın almayı onayla",List.of(product.id()+": "+product.description(),market.balance(p),"Bu ürün yalnızca X Arena'da kullanılır."),List.of(),List.of(button("Satın al • "+product.price().toPlainString(),()->{market.buy(p,product);showMarket(p);}),button("Vazgeç",()->showMarket(p))));}
    private void chooseRole(Player p,ArenaCharacter kind){Place place=places.get(p.getUniqueId());if(place!=Place.LOBBY&&place!=Place.XARENA)throw new IllegalArgumentException("Karakterler yalnızca X Arena içindir.");if(place==Place.XARENA&&!combat.mayChoose(p))throw new IllegalArgumentException("Karakterini öldükten sonra veya Arena lobisinde değiştir.");if(roles.getOrDefault(p.getUniqueId(),ArenaCharacter.WARRIOR)!=kind){roles.put(p.getUniqueId(),kind);if(place==Place.XARENA){loadX(p);combat.lockChoice(p);}}tell(p,"X Arena karakterin: "+kind.label);}
    private void showClasses(Player p){Place place=places.get(p.getUniqueId());if(place==Place.MATCH||place==Place.WAITING)throw new IllegalArgumentException("1v1/2v2 odalarında karakter ve beceri kullanılmaz.");List<Button> buttons=new ArrayList<>();for(ArenaCharacter kind:ArenaCharacter.values())buttons.add(button(kind.label,()->{chooseRole(p,kind);if(places.get(p.getUniqueId())==Place.XARENA)showSkills(p);else openMenu(p);}));buttons.add(button("Geri",()->openMenu(p)));show(p,"X Arena karakteri",List.of("Her karakterin dört bağımsız becerisi vardır; istediğin sırada kullan.","Beceriler 1–4, yakın dövüş silahı 5. yuvada. Zırh hasarı azaltır; dövüş dışında can yenilenir.","Karakterini lobide ve ölümden sonra değiştirebilirsin."),List.of(),buttons);}
    private void showSkills(Player p){if(places.get(p.getUniqueId())!=Place.XARENA)throw new IllegalArgumentException("Beceriler sadece X Arena'da kullanılabilir.");ArenaCharacter kind=roles.getOrDefault(p.getUniqueId(),ArenaCharacter.WARRIOR);List<String> lines=new ArrayList<>();List<Button> buttons=new ArrayList<>();for(int i=0;i<4;i++){int skill=i+1;ArenaCharacter.Skill spec=kind.skills.get(i);lines.add(skill+" • "+spec.name()+": "+spec.description()+" • "+NetworkFeatures.cooldown(p,spec.cooldownSeconds())+" sn");buttons.add(button(skill+" • "+spec.name(),()->{combat.use(p,skill);showSkills(p);}));}buttons.add(button("Geri",()->openMenu(p)));show(p,kind.label+" • Dört beceri",lines,List.of(),buttons);}
    private void show(Player p,String title,List<String> lines,List<DialogInput> inputs,List<Button> buttons){long token=++pageSequence;pageTokens.put(p.getUniqueId(),token);List<ActionButton> actions=new ArrayList<>();for(Button button:buttons){DialogAction click=DialogAction.customClick((response,audience)->{if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId())||!isEnabled())return;Bukkit.getScheduler().runTask(this,()->{if(!p.isOnline()||!Objects.equals(pageTokens.get(p.getUniqueId()),token))return;pageTokens.remove(p.getUniqueId());p.closeDialog();try{button.action().accept(response);}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}});},ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build());actions.add(ActionButton.create(Component.text(button.label(),NamedTextColor.AQUA),null,150,click));}List<DialogBody> body=lines.stream().<DialogBody>map(line->DialogBody.plainMessage(Component.text(line,NamedTextColor.GRAY),320)).toList();p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(Component.text(title,NamedTextColor.GOLD)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.NONE).body(body).inputs(inputs).build()).type(DialogType.multiAction(actions).columns(2).build())));}
    private Button button(String label,Runnable action){return new Button(label,response->action.run());}
    private boolean isMenu(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(menuKey);}
    private boolean isAbility(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(abilityKey);}
    private void removeMenuCompass(Player p){for(int i=0;i<p.getInventory().getSize();i++)if(isMenu(p.getInventory().getItem(i)))p.getInventory().setItem(i,null);if(isMenu(p.getItemOnCursor()))p.setItemOnCursor(null);}
    private void giveMenuCompass(Player p){removeMenuCompass(p);if(places.get(p.getUniqueId())!=Place.LOBBY)return;ItemStack item=new ItemStack(Material.COMPASS);item.editMeta(meta->{meta.displayName(Component.text("Kasaba • Arena Menüsü",NamedTextColor.GOLD));meta.getPersistentDataContainer().set(menuKey,PersistentDataType.BYTE,(byte)1);});p.getInventory().setItem(8,item);}
    private void refreshCompasses(){for(Player p:Bukkit.getOnlinePlayers())if(places.get(p.getUniqueId())==Place.LOBBY&&Arrays.stream(p.getInventory().getContents()).noneMatch(this::isMenu))giveMenuCompass(p);}
    private boolean canFight(Player a,Player b){if(safeLobby(a.getLocation())||safeLobby(b.getLocation()))return false;Place one=places.get(a.getUniqueId()),two=places.get(b.getUniqueId());if(one==Place.XARENA&&two==Place.XARENA)return true;Match m=matches.get(a.getUniqueId());return m!=null&&m==matches.get(b.getUniqueId())&&m.started&&!m.finished&&!m.paused&&!m.down.contains(a.getUniqueId())&&!m.down.contains(b.getUniqueId())&&!m.sameSide(a.getUniqueId(),b.getUniqueId());}
    private Player resolvePlayer(Entity entity){if(entity instanceof Player p)return p;if(entity instanceof Projectile projectile&&projectile.getShooter() instanceof Player p)return p;return null;}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){Player p=e.getPlayer();if(isMenu(e.getItem())){e.setCancelled(true);if(e.getHand()==org.bukkit.inventory.EquipmentSlot.HAND&&(e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK))openMenu(p);return;}if(isAbility(e.getItem())){e.setCancelled(true);if(e.getHand()==org.bukkit.inventory.EquipmentSlot.HAND&&(e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK))try{combat.useItem(p,e.getItem());}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}return;}if(e.getAction()==Action.RIGHT_CLICK_BLOCK&&places.containsKey(p.getUniqueId())&&e.getClickedBlock()!=null&&e.getClickedBlock().getState() instanceof org.bukkit.inventory.InventoryHolder)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventoryClick(InventoryClickEvent e){if(!(e.getWhoClicked() instanceof Player p))return;boolean special=isMenu(e.getCurrentItem())||isMenu(e.getCursor())||isAbility(e.getCurrentItem())||isAbility(e.getCursor());if(e.getHotbarButton()>=0){ItemStack hotbar=p.getInventory().getItem(e.getHotbarButton());special|=isAbility(hotbar)||isMenu(hotbar);}if(special||places.containsKey(p.getUniqueId())&&e.getView().getTopInventory().getType()!=InventoryType.CRAFTING)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void inventoryDrag(InventoryDragEvent e){if(isAbility(e.getOldCursor())||isMenu(e.getOldCursor())||e.getWhoClicked() instanceof Player p&&places.containsKey(p.getUniqueId())&&e.getView().getTopInventory().getType()!=InventoryType.CRAFTING)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void swap(PlayerSwapHandItemsEvent e){if(isMenu(e.getMainHandItem())||isAbility(e.getMainHandItem())||isMenu(e.getOffHandItem())||isAbility(e.getOffHandItem()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drop(PlayerDropItemEvent e){if(places.containsKey(e.getPlayer().getUniqueId())||isMenu(e.getItemDrop().getItemStack())||isAbility(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p&&places.containsKey(p.getUniqueId())||isMenu(e.getItem().getItemStack())||isAbility(e.getItem().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){if(!(e.getEntity() instanceof Player target))return;Place place=places.get(target.getUniqueId());if(place==Place.LOBBY||place==Place.WAITING||safeLobby(target.getLocation())){e.setCancelled(true);return;}Match match=matches.get(target.getUniqueId());if(match!=null&&(match.paused||match.finished||match.down.contains(target.getUniqueId()))){e.setCancelled(true);return;}if(e instanceof EntityDamageByEntityEvent by){Player attacker=resolvePlayer(by.getDamager());if(attacker!=null&&(places.containsKey(attacker.getUniqueId())||place!=null)&&!canFight(attacker,target))e.setCancelled(true);}}
    private boolean protectedBuild(Player p,Location at){return places.containsKey(p.getUniqueId())||regions.values().stream().anyMatch(region->region.contains(at));}
    private boolean safeLobby(Location at){return regions.entrySet().stream().anyMatch(entry->(entry.getKey().equals("lobi")||entry.getKey().startsWith("lobi__"))&&entry.getValue().contains(at));}
    private boolean regionContains(String name,Location at){Region region=regions.get(name);return region!=null&&region.contains(at);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(protectedBuild(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void placeBlock(BlockPlaceEvent e){if(protectedBuild(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explode(EntityExplodeEvent e){e.blockList().removeIf(block->regions.values().stream().anyMatch(region->region.contains(block.getLocation())));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explode(BlockExplodeEvent e){e.blockList().removeIf(block->regions.values().stream().anyMatch(region->region.contains(block.getLocation())));}
    @EventHandler(priority=EventPriority.HIGHEST) public void death(PlayerDeathEvent e){
        Player p=e.getEntity();Place place=places.get(p.getUniqueId());if(place==null)return;e.getDrops().clear();e.setDroppedExp(0);e.setKeepInventory(true);e.setKeepLevel(true);combat.clear(p);p.getInventory().clear();
        if(place==Place.XARENA){Player killer=p.getKiller();if(killer!=null&&killer!=p&&places.get(killer.getUniqueId())==Place.XARENA){kills.merge(killer.getUniqueId(),1,Integer::sum);names.put(killer.getUniqueId(),killer.getName());saveStats();}}
        if(place==Place.MATCH){Match match=matches.get(p.getUniqueId());if(match!=null&&!match.paused&&!match.finished){match.down.add(p.getUniqueId());Bukkit.getScheduler().runTask(this,()->{if(!match.finished&&!match.paused&&match.team(p.getUniqueId()).stream().allMatch(match.down::contains))winRound(match,!match.red.contains(p.getUniqueId()));});}}
        Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&p.isDead()&&places.containsKey(p.getUniqueId()))p.spigot().respawn();},2);
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void respawn(PlayerRespawnEvent e){Player p=e.getPlayer();Place place=places.get(p.getUniqueId());if(place==Place.XARENA&&!xSpawns.isEmpty())e.setRespawnLocation(randomSpawn(p));else if(place==Place.MATCH||place==Place.WAITING){Match m=matches.get(p.getUniqueId());if(m!=null)e.setRespawnLocation(spawn(m,p.getUniqueId()));}else if(place==Place.LOBBY&&valid(lobbySpawn))e.setRespawnLocation(lobbySpawn);
        Bukkit.getScheduler().runTask(this,()->{if(!p.isOnline())return;Place now=places.get(p.getUniqueId());if(now==Place.XARENA){loadX(p);updateScoreboard(p);showClasses(p);}else if(now==Place.MATCH){Match m=matches.get(p.getUniqueId());if(m!=null&&m.down.contains(p.getUniqueId())){resetFighter(p,true);p.setGameMode(GameMode.SPECTATOR);}}else if(now==Place.LOBBY)giveMenuCompass(p);});}
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTaskLater(this,()->{if(!places.containsKey(e.getPlayer().getUniqueId()))recoverSnapshot(e.getPlayer());},2);}
    @EventHandler(priority=EventPriority.MONITOR) public void quit(PlayerQuitEvent e){cleanupPlayer(e.getPlayer().getUniqueId());}
    private void cleanupPlayer(UUID id){Player p=Bukkit.getPlayer(id);places.remove(id);detachRoom(id);pageTokens.remove(id);roles.remove(id);lastXSpawn.remove(id);if(p!=null){combat.clear(p);clearScoreboard(p);removeMenuCompass(p);restoreSnapshot(p);}}
    private void loadStats(){kills.clear();names.clear();ConfigurationSection k=getConfig().getConfigurationSection("stats.kills"),n=getConfig().getConfigurationSection("stats.names");if(k!=null)for(String key:k.getKeys(false))try{kills.put(UUID.fromString(key),k.getInt(key));}catch(IllegalArgumentException ignored){}if(n!=null)for(String key:n.getKeys(false))try{names.put(UUID.fromString(key),n.getString(key,"Oyuncu"));}catch(IllegalArgumentException ignored){}}
    private void saveStats(){for(UUID id:kills.keySet()){getConfig().set("stats.kills."+id,kills.get(id));getConfig().set("stats.names."+id,names.getOrDefault(id,"Oyuncu"));}saveConfig();}
    private void refreshScoreboards(){for(Player p:Bukkit.getOnlinePlayers())if(places.get(p.getUniqueId())==Place.XARENA)updateScoreboard(p);}
    private void updateScoreboard(Player p){UUID id=p.getUniqueId();Scoreboard board=p.getScoreboard();if(!previousBoards.containsKey(id)){Scoreboard previous=board;board=Bukkit.getScoreboardManager().getNewScoreboard();for(Team source:previous.getTeams()){Team copy=board.registerNewTeam(source.getName());copy.prefix(source.prefix());copy.suffix(source.suffix());copy.setColor(source.getColor());for(String entry:source.getEntries())copy.addEntry(entry);}previousBoards.put(id,previous);p.setScoreboard(board);}Objective objective=board.getObjective("kasaba_arena");if(objective==null)objective=board.registerNewObjective("kasaba_arena",Criteria.DUMMY,Component.text("X Arena • Öldürme",NamedTextColor.GOLD));objective.setDisplaySlot(DisplaySlot.SIDEBAR);for(String entry:boardEntries.getOrDefault(id,List.of()))board.resetScores(entry);List<UUID> top=kills.keySet().stream().sorted(Comparator.<UUID>comparingInt(player->kills.get(player)).reversed().thenComparing(player->names.getOrDefault(player,""))).limit(5).toList();List<String> entries=new ArrayList<>();if(top.isEmpty()){entries.add("§7Henüz öldürme yok");objective.getScore(entries.getFirst()).setScore(0);}else{int rank=1;for(UUID player:top){String entry="§e"+(rank++)+". §f"+names.getOrDefault(player,"Oyuncu");entries.add(entry);objective.getScore(entry).setScore(kills.get(player));}}boardEntries.put(id,entries);}
    private void clearScoreboard(Player p){Scoreboard previous=previousBoards.remove(p.getUniqueId());boardEntries.remove(p.getUniqueId());if(previous!=null)p.setScoreboard(previous);}
    private void saveSnapshot(Player p){String path="players."+p.getUniqueId();snapshots.set(path+".items",Arrays.asList(p.getInventory().getStorageContents()));snapshots.set(path+".armor",Arrays.asList(p.getInventory().getArmorContents()));snapshots.set(path+".offhand",p.getInventory().getItemInOffHand());snapshots.set(path+".cursor",p.getItemOnCursor());snapshots.set(path+".level",p.getLevel());snapshots.set(path+".exp",p.getExp());snapshots.set(path+".food",p.getFoodLevel());snapshots.set(path+".saturation",p.getSaturation());snapshots.set(path+".health",p.getHealth());snapshots.set(path+".gamemode",p.getGameMode().name());snapshots.set(path+".effects",new ArrayList<>(p.getActivePotionEffects()));snapshots.set(path+".absorption",p.getAbsorptionAmount());snapshots.set(path+".invulnerable",p.isInvulnerable());saveSnapshots();}
    private void restoreSnapshot(Player p){String path="players."+p.getUniqueId();if(!snapshots.contains(path))return;resetFighter(p,true);List<?> items=snapshots.getList(path+".items"),armor=snapshots.getList(path+".armor");if(items!=null)p.getInventory().setStorageContents(items.toArray(new ItemStack[0]));if(armor!=null)p.getInventory().setArmorContents(armor.toArray(new ItemStack[0]));p.getInventory().setItemInOffHand(snapshots.getItemStack(path+".offhand"));p.setItemOnCursor(snapshots.getItemStack(path+".cursor"));p.setLevel(snapshots.getInt(path+".level"));p.setExp((float)snapshots.getDouble(path+".exp"));p.setFoodLevel(snapshots.getInt(path+".food",20));p.setSaturation((float)snapshots.getDouble(path+".saturation",10));if(!p.isDead())p.setHealth(Math.min(maxHealth(p),Math.max(1,snapshots.getDouble(path+".health",20))));for(Object raw:snapshots.getList(path+".effects",List.of()))if(raw instanceof PotionEffect effect)p.addPotionEffect(effect);p.setAbsorptionAmount(snapshots.getDouble(path+".absorption",0));p.setInvulnerable(snapshots.getBoolean(path+".invulnerable",false));try{p.setGameMode(GameMode.valueOf(snapshots.getString(path+".gamemode","SURVIVAL")));}catch(IllegalArgumentException ignored){}snapshots.set(path,null);saveSnapshots();}
    private void recoverSnapshot(Player p){if(p.isOnline()&&snapshots.contains("players."+p.getUniqueId())){restoreSnapshot(p);tell(p,"Önceki Arena oturumundaki eşyaların ve etkilerin geri yüklendi.");}}
    private void saveSnapshots(){try{snapshots.save(snapshotFile);}catch(IOException ex){throw new IllegalStateException("Oyuncu envanter yedeği kaydedilemedi.",ex);}}
}
