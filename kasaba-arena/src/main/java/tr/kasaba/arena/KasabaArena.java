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
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import tr.kasaba.hub.api.GameInfo;
import tr.kasaba.hub.api.HubApi;
import tr.kasaba.hub.api.HubGame;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/** Arena game registered in KasabaHub. All commands and game state run on the server thread. */
public final class KasabaArena extends JavaPlugin implements Listener, CommandExecutor {
    private enum Place { LOBBY, XARENA, WAITING, MATCH }
    private enum Role { WARRIOR("Savaşçı"), ASSASSIN("Suikastçı"), TANK("Tank"), ARCHER("Okçu");
        final String label; Role(String label) { this.label = label; }
    }
    private record Region(UUID world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        boolean contains(Location at) { return at != null && at.getWorld() != null && world.equals(at.getWorld().getUID()) && at.getBlockX() >= minX && at.getBlockX() <= maxX && at.getBlockY() >= minY && at.getBlockY() <= maxY && at.getBlockZ() >= minZ && at.getBlockZ() <= maxZ; }
    }
    private static final class Room { final String code; final UUID host; Room(String code, UUID host) { this.code = code; this.host = host; } }
    private static final class Match {
        final String mode, code; final List<UUID> red, blue; final Set<UUID> down = new HashSet<>();
        int round = 1, redScore, blueScore; boolean paused, finished;
        Match(String mode, String code, List<UUID> red, List<UUID> blue) { this.mode = mode; this.code = code; this.red = new ArrayList<>(red); this.blue = new ArrayList<>(blue); }
        boolean contains(UUID id) { return red.contains(id) || blue.contains(id); }
        boolean sameSide(UUID a, UUID b) { return red.contains(a) && red.contains(b) || blue.contains(a) && blue.contains(b); }
        List<UUID> team(UUID id) { return red.contains(id) ? red : blue; }
        List<UUID> opponents(UUID id) { return red.contains(id) ? blue : red; }
    }
    private record Button(String label, java.util.function.Consumer<io.papermc.paper.dialog.DialogResponseView> action) {}

    private final Map<UUID, Place> places = new HashMap<>();
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, String> selectedKit = new HashMap<>();
    private final Map<UUID, Match> matches = new HashMap<>();
    private final Map<UUID, Room> roomsByPlayer = new HashMap<>();
    private final Map<String, Room> rooms = new HashMap<>();
    private final Map<String, Region> regions = new HashMap<>();
    private final Map<UUID, Location> selectionOne = new HashMap<>(), selectionTwo = new HashMap<>();
    private final Map<UUID, Long> abilityReady = new HashMap<>();
    private final Set<UUID> scoreboards = new HashSet<>();
    private final Map<UUID, List<String>> boardEntries = new HashMap<>();
    private final Map<UUID, Scoreboard> previousBoards = new HashMap<>();
    private final List<UUID> oneQueue = new ArrayList<>(), twoQueue = new ArrayList<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Long> pageTokens = new HashMap<>();
    private long pageSequence;
    private Location lobbySpawn, redSpawn, blueSpawn;
    private final List<Location> xSpawns = new ArrayList<>();
    private HubApi hub;
    private NamespacedKey menuKey, abilityKey;
    private YamlConfiguration snapshots;
    private java.io.File snapshotFile;

    @Override public void onEnable() {
        saveDefaultConfig(); menuKey = new NamespacedKey(this, "arena-menu"); abilityKey = new NamespacedKey(this, "arena-ability");
        snapshotFile = new java.io.File(getDataFolder(), "oyuncu-yedekleri.yml"); snapshots = YamlConfiguration.loadConfiguration(snapshotFile);
        loadLocations(); loadStats();
        Objects.requireNonNull(getCommand("arena")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        RegisteredServiceProvider<HubApi> provider = Bukkit.getServicesManager().getRegistration(HubApi.class);
        if (provider == null) { getLogger().severe("KasabaHub API bulunamadı; eklenti kapatılıyor."); Bukkit.getPluginManager().disablePlugin(this); return; }
        hub = provider.getProvider();
        hub.register(this, new ArenaGame());
        long interval = Math.max(20, getConfig().getLong("ayarlar.skor-tahtasi-guncelleme-tik", 40));
        Bukkit.getScheduler().runTaskTimer(this, this::refreshScoreboards, interval, interval);
        Bukkit.getScheduler().runTaskTimer(this, this::refreshCompasses, 20, 40);
        for (Player player : Bukkit.getOnlinePlayers()) recoverSnapshot(player);
    }

    @Override public void onDisable() {
        if (hub != null) { try { hub.unregister(this, "arena"); } catch (RuntimeException ex) { getLogger().log(Level.WARNING, "Arena merkezden çıkarılamadı.", ex); } }
        for (Player p : Bukkit.getOnlinePlayers()) { clearScoreboard(p); removeMenuCompass(p); restoreSnapshot(p); }
        saveStats(); saveSnapshots();
    }

    private final class ArenaGame implements HubGame {
        @Override public GameInfo info() { return new GameInfo("arena", "Arena", "X Arena • kodlu 1v1 • 2v2", 1, 20); }
        @Override public boolean available() { return valid(lobbySpawn); }
        @Override public boolean join(List<Player> team) {
            if (!available() || team.isEmpty()) return false;
            for (Player p : team) if (places.containsKey(p.getUniqueId()) || !valid(lobbySpawn)) return false;
            List<Player> moved = new ArrayList<>();
            try {
                for (Player p : team) {
                    saveSnapshot(p); p.getInventory().clear(); p.getInventory().setArmorContents(new ItemStack[4]); p.getInventory().setItemInOffHand(null);
                    if (!p.teleport(lobbySpawn, PlayerTeleportEvent.TeleportCause.PLUGIN)) throw new IllegalStateException("Işınlanma engellendi");
                    moved.add(p); places.put(p.getUniqueId(), Place.LOBBY); giveMenuCompass(p); p.setFallDistance(0);
                }
                for (Player p : team) p.sendMessage(Component.text("Arena lobisine geldin. Menü: pusulaya sağ tık veya /arena • Çıkış: /lobi", NamedTextColor.GREEN));
                return true;
            } catch (RuntimeException ex) {
                for (Player p : team) { places.remove(p.getUniqueId()); removeMenuCompass(p); restoreSnapshot(p); }
                for (Player p : moved) if (p.isOnline()) p.sendMessage(Component.text("Arena girişi tamamlanamadı; eşyaların geri yüklendi.", NamedTextColor.RED));
                getLogger().log(Level.WARNING, "Arena giriş grubu geri alındı.", ex); return false;
            }
        }
        @Override public void leave(UUID id) { cleanupPlayer(id, true); }
    }

    private void loadLocations() {
        lobbySpawn = readLocation(getConfig().getConfigurationSection("spawns.lobi"));
        redSpawn = readLocation(getConfig().getConfigurationSection("spawns.1v1.kirmizi"));
        blueSpawn = readLocation(getConfig().getConfigurationSection("spawns.1v1.mavi"));
        xSpawns.clear();
        for (Object raw : getConfig().getList("spawns.xarena", List.of())) if (raw instanceof Map<?, ?> map) { Location l = readLocation(map); if (valid(l)) xSpawns.add(l); }
        regions.clear(); ConfigurationSection section = getConfig().getConfigurationSection("regions");
        if (section != null) for (String name : section.getKeys(false)) {
            ConfigurationSection r = section.getConfigurationSection(name); if (r == null) continue;
            try { UUID world = UUID.fromString(r.getString("world", "")); regions.put(name, new Region(world, r.getInt("minX"), r.getInt("maxX"), r.getInt("minY"), r.getInt("maxY"), r.getInt("minZ"), r.getInt("maxZ"))); }
            catch (IllegalArgumentException ignored) { getLogger().warning("Geçersiz arena bölgesi: " + name); }
        }
    }

    private Location readLocation(ConfigurationSection section) {
        if (section == null) return null;
        String worldId = section.getString("world", ""); World world = null;
        try { world = Bukkit.getWorld(UUID.fromString(worldId)); } catch (IllegalArgumentException ignored) {}
        if (world == null) world = Bukkit.getWorld(section.getString("worldName", ""));
        if (world == null) return null;
        return new Location(world, section.getDouble("x"), section.getDouble("y"), section.getDouble("z"), (float)section.getDouble("yaw"), (float)section.getDouble("pitch"));
    }
    private Location readLocation(Map<?, ?> map) {
        try {
            UUID id = UUID.fromString(String.valueOf(map.get("world"))); World world = Bukkit.getWorld(id);
            if (world == null) world = Bukkit.getWorld(String.valueOf(map.get("worldName")));
            if (world == null) return null;
            return new Location(world, number(map.get("x")), number(map.get("y")), number(map.get("z")), (float)number(map.get("yaw")), (float)number(map.get("pitch")));
        } catch (RuntimeException ex) { return null; }
    }
    private double number(Object value) { return value instanceof Number n ? n.doubleValue() : 0; }
    private boolean valid(Location l) { return l != null && l.getWorld() != null && l.getY() >= l.getWorld().getMinHeight() && l.getY() < l.getWorld().getMaxHeight() && l.getWorld().getWorldBorder().isInside(l); }
    private void writeLocation(String path, Location l) {
        getConfig().set(path + ".world", l.getWorld().getUID().toString()); getConfig().set(path + ".worldName", l.getWorld().getName());
        getConfig().set(path + ".x", l.getX()); getConfig().set(path + ".y", l.getY()); getConfig().set(path + ".z", l.getZ());
        getConfig().set(path + ".yaw", l.getYaw()); getConfig().set(path + ".pitch", l.getPitch());
    }
    private Location point(Player p) { return p.getLocation().clone(); }
    private void saveSettings() { saveConfig(); loadLocations(); }
    private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replace("ı", "i").replace("ş", "s").replace("ç", "c").replace("ğ", "g").replace("ü", "u").replace("ö", "o"); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Bu komut oyunda kullanılmalı."); return true; }
        try {
            if (args.length == 0) { requireArena(p); openMenu(p); return true; }
            String action = normalize(args[0]);
            if (action.equals("yardim")) { help(p); return true; }
            if (action.equals("lobi") && args.length >= 2 && normalize(args[1]).equals("ayarla")) { admin(p); writeLocation("spawns.lobi", point(p)); saveSettings(); tell(p, "Arena güvenli lobi noktası ayarlandı."); return true; }
            if (action.equals("dogma")) { spawnCommand(p, args); return true; }
            if (action.equals("bolge")) { regionCommand(p, args); return true; }
            if (action.equals("kit")) { kitCommand(p, args); return true; }
            if (action.equals("yenile")) { admin(p); reloadConfig(); loadLocations(); loadStats(); tell(p, "Arena ayarları yenilendi."); return true; }
            if (action.equals("oda")) { requireArena(p); roomCommand(p, args); return true; }
            if (action.equals("katil")) { requireArena(p); if (args.length < 2) throw new IllegalArgumentException("Kullanım: /arena katıl <kod>"); joinRoom(p, args[1]); return true; }
            requireArena(p);
            if (!places.containsKey(p.getUniqueId())) throw new IllegalArgumentException("Önce /oyunlar arena ile Arena lobisine gir.");
            switch (action) {
                case "xarena", "x" -> joinX(p);
                case "lobi" -> arenaLobby(p);
                case "cikis", "çıkış" -> hub.returnToLobby(p.getUniqueId());
                case "yetenek" -> useAbility(p);
                case "normal" -> { selectedKit.put(p.getUniqueId(), "normal"); tell(p, "Normal kit seçildi."); }
                case "vip" -> { if (!p.hasPermission("kasabaarena.kit.vip")) throw new IllegalArgumentException("VIP kit yetkin yok."); selectedKit.put(p.getUniqueId(), "vip"); tell(p, "VIP kit seçildi."); }
                case "sinif", "sınıf" -> { if (args.length < 2) showClasses(p); else chooseRole(p, args[1]); }
                case "1v1" -> queueMatch(p, false);
                case "2v2" -> queueMatch(p, true);
                default -> openMenu(p);
            }
        } catch (IllegalArgumentException ex) { tell(p, ex.getMessage()); }
        return true;
    }
    private void requireArena(Player p) { if (hub == null) throw new IllegalArgumentException("Arena merkez bağlantısı hazır değil."); }
    private void admin(Player p) { if (!p.hasPermission("kasabaarena.yonet") && !p.hasPermission("kasabaroles.admin")) throw new IllegalArgumentException("Bu komut için arena yönetim yetkisi gerekiyor."); }
    private void tell(Player p, String message) { p.sendMessage(Component.text(message, NamedTextColor.YELLOW)); }
    private void help(Player p) {
        tell(p, "/oyunlar arena • /arena • /arena lobi • /arena xarena • /arena oda oluştur • /arena katıl <kod> • /arena 1v1|2v2");
        if (p.hasPermission("kasabaarena.yonet") || p.isOp()) tell(p, "Yönetim: /arena lobi ayarla • /arena doğma ekle xarena • /arena doğma ayarla kırmızı|mavi • /arena bölge pos1|pos2|kaydet lobi|xarena • /arena kit normal|vip kaydet");
    }

    private void spawnCommand(Player p, String[] args) {
        admin(p); if (args.length < 2) throw new IllegalArgumentException("/arena doğma ekle xarena | /arena doğma ayarla kırmızı|mavi");
        String operation = normalize(args[1]);
        if (operation.equals("ekle") && args.length >= 3 && normalize(args[2]).equals("xarena")) {
            List<Map<String, Object>> values = new ArrayList<>();
            for (Location l : xSpawns) values.add(locationMap(l)); values.add(locationMap(point(p)));
            getConfig().set("spawns.xarena", values); saveSettings(); tell(p, "X Arena doğma noktası eklendi. Kayıtlı nokta: " + xSpawns.size()); return;
        }
        if (operation.equals("sil") && args.length >= 3 && normalize(args[2]).equals("xarena")) {
            if (xSpawns.isEmpty()) throw new IllegalArgumentException("Silinecek X Arena noktası yok.");
            xSpawns.remove(xSpawns.size()-1); List<Map<String, Object>> values = xSpawns.stream().map(this::locationMap).toList(); getConfig().set("spawns.xarena", values); saveSettings(); tell(p, "Son eklenen X Arena doğma noktası silindi."); return;
        }
        if (operation.equals("ayarla") && args.length >= 3) {
            String side = normalize(args[2]); String path = switch (side) { case "kirmizi", "red" -> "spawns.1v1.kirmizi"; case "mavi", "blue" -> "spawns.1v1.mavi"; default -> throw new IllegalArgumentException("Renk kırmızı veya mavi olmalı."); };
            writeLocation(path, point(p)); saveSettings(); tell(p, (side.equals("mavi") || side.equals("blue") ? "Mavi" : "Kırmızı") + " takım doğma noktası ayarlandı."); return;
        }
        if (operation.equals("liste") && args.length >= 3 && normalize(args[2]).equals("xarena")) { tell(p, "X Arena için " + xSpawns.size() + " doğma noktası kayıtlı."); return; }
        throw new IllegalArgumentException("/arena doğma ekle xarena | sil xarena | liste xarena | ayarla kırmızı|mavi");
    }
    private Map<String,Object> locationMap(Location l) { Map<String,Object> m = new LinkedHashMap<>(); m.put("world", l.getWorld().getUID().toString()); m.put("worldName", l.getWorld().getName()); m.put("x", l.getX()); m.put("y", l.getY()); m.put("z", l.getZ()); m.put("yaw", l.getYaw()); m.put("pitch", l.getPitch()); return m; }

    private void regionCommand(Player p, String[] args) {
        admin(p); if (args.length < 2) throw new IllegalArgumentException("/arena bölge pos1|pos2|kaydet lobi|xarena");
        String op = normalize(args[1]);
        if (op.equals("pos1")) { selectionOne.put(p.getUniqueId(), point(p)); tell(p, "Bölge ilk köşesi seçildi."); return; }
        if (op.equals("pos2")) { selectionTwo.put(p.getUniqueId(), point(p)); tell(p, "Bölge ikinci köşesi seçildi."); return; }
        if (op.equals("kaydet") && args.length >= 3) {
            String name = normalize(args[2]); if (!Set.of("lobi", "xarena").contains(name)) throw new IllegalArgumentException("Bölge adı lobi veya xarena olmalı.");
            Location a = selectionOne.get(p.getUniqueId()), b = selectionTwo.get(p.getUniqueId());
            if (a == null || b == null || !a.getWorld().equals(b.getWorld())) throw new IllegalArgumentException("Aynı dünyada pos1 ve pos2 seçmelisin.");
            int minX=Math.min(a.getBlockX(),b.getBlockX()), maxX=Math.max(a.getBlockX(),b.getBlockX()), minY=Math.min(a.getBlockY(),b.getBlockY()), maxY=Math.max(a.getBlockY(),b.getBlockY()), minZ=Math.min(a.getBlockZ(),b.getBlockZ()), maxZ=Math.max(a.getBlockZ(),b.getBlockZ());
            String path="regions."+name; getConfig().set(path+".world",a.getWorld().getUID().toString()); getConfig().set(path+".minX",minX); getConfig().set(path+".maxX",maxX); getConfig().set(path+".minY",minY); getConfig().set(path+".maxY",maxY); getConfig().set(path+".minZ",minZ); getConfig().set(path+".maxZ",maxZ); saveSettings(); tell(p, name.equals("lobi") ? "Lobi koruma alanı kaydedildi." : "X Arena alandaki blok kırma/koyma koruması kaydedildi."); return;
        }
        throw new IllegalArgumentException("/arena bölge pos1|pos2|kaydet lobi|xarena");
    }

    private void kitCommand(Player p, String[] args) {
        admin(p); if (args.length < 3 || !normalize(args[2]).equals("kaydet")) throw new IllegalArgumentException("Kitini envanterine koyup /arena kit normal kaydet veya /arena kit vip kaydet yaz.");
        String kit = normalize(args[1]); if (!Set.of("normal", "vip").contains(kit)) throw new IllegalArgumentException("Kit normal veya vip olmalı.");
        getConfig().set("kits."+kit+".items", p.getInventory().getStorageContents()); getConfig().set("kits."+kit+".armor", p.getInventory().getArmorContents()); saveConfig(); tell(p, (kit.equals("vip")?"VIP":"Normal") + " kit mevcut envanterinden kaydedildi.");
    }

    private void roomCommand(Player p, String[] args) {
        if (args.length >= 2 && normalize(args[1]).equals("olustur")) { createRoom(p); return; }
        throw new IllegalArgumentException("/arena oda oluştur • Arkadaşın /arena katıl <kod> yazar.");
    }
    private void createRoom(Player p) {
        requireLobby(p); if (!valid(redSpawn) || !valid(blueSpawn)) throw new IllegalArgumentException("Önce yönetici 1v1 kırmızı ve mavi doğma noktalarını ayarlasın.");
        String code; do { code = String.format(Locale.ROOT, "%06d", ThreadLocalRandom.current().nextInt(1_000_000)); } while (rooms.containsKey(code));
        Room room = new Room(code, p.getUniqueId()); rooms.put(code, room); roomsByPlayer.put(p.getUniqueId(), room); places.put(p.getUniqueId(), Place.WAITING); removeMenuCompass(p); p.getInventory().clear(); p.teleport(redSpawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
        p.sendMessage(Component.text("Özel 1v1 odan açıldı. Arkadaşına kodu ver: ", NamedTextColor.GREEN).append(Component.text(code, NamedTextColor.GOLD)));
        p.sendMessage(Component.text("Arkadaşın /arena katıl " + code + " yazsın. Beklerken /arena lobi ile dönebilirsin.", NamedTextColor.GRAY));
    }
    private void joinRoom(Player p, String code) {
        requireLobby(p); requireLobby(p); Room room = rooms.get(code);
        if (room == null) throw new IllegalArgumentException("Bu oda kodu bulunamadı veya sahibi ayrıldı.");
        if (room.host.equals(p.getUniqueId())) throw new IllegalArgumentException("Kendi odana katılamazsın.");
        if (!valid(redSpawn)||!valid(blueSpawn)) throw new IllegalArgumentException("Yönetici 1v1 doğma noktalarını ayarlamalı.");
        Player host = Bukkit.getPlayer(room.host); if (host == null || !host.isOnline()) { rooms.remove(code); throw new IllegalArgumentException("Oda sahibi çevrimdışı."); }
        rooms.remove(code); roomsByPlayer.remove(host.getUniqueId()); roomsByPlayer.remove(p.getUniqueId());
        Match match = new Match("1v1", code, List.of(host.getUniqueId()), List.of(p.getUniqueId())); matches.put(host.getUniqueId(),match); matches.put(p.getUniqueId(),match); places.put(host.getUniqueId(),Place.MATCH); places.put(p.getUniqueId(),Place.MATCH);
        for (Player player : List.of(host,p)) { player.getInventory().clear(); applyKit(player); giveAbility(player); player.setGameMode(GameMode.SURVIVAL); player.setInvulnerable(true); }
        host.teleport(redSpawn,PlayerTeleportEvent.TeleportCause.PLUGIN); p.teleport(blueSpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);
        tell(host,"Rakibin geldi. 1v1 üç raunt üzerinden başlayacak."); tell(p,"Odaya katıldın. 1v1 üç raunt üzerinden başlayacak."); startRound(match);
    }

    private void requireLobby(Player p) { if (!places.containsKey(p.getUniqueId())) throw new IllegalArgumentException("Önce /oyunlar arena ile Arena lobisine gir."); if (places.get(p.getUniqueId()) != Place.LOBBY) throw new IllegalArgumentException("Bu işlemi Arena güvenli lobisinde yapmalısın."); }

    private void queueMatch(Player p, boolean doubles) {
        requireLobby(p); if (!valid(redSpawn)||!valid(blueSpawn)) throw new IllegalArgumentException("1v1/2v2 doğma noktaları henüz ayarlanmamış.");
        List<UUID> queue = doubles ? twoQueue : oneQueue;
        if (queue.contains(p.getUniqueId())) { queue.remove(p.getUniqueId()); tell(p,"Sıradan çıktın."); return; }
        queue.add(p.getUniqueId()); tell(p, doubles ? "2v2 sırasına girdin. 4 oyuncu olunca maç başlayacak." : "1v1 sırasına girdin. Rakip bekleniyor. İptal etmek için /arena 1v1 yaz.");
        int needed=doubles?4:2;
        while(queue.size()>=needed) {
            List<UUID> players=new ArrayList<>(queue.subList(0,needed)); queue.subList(0,needed).clear();
            List<UUID> red=doubles?players.subList(0,2):players.subList(0,1), blue=doubles?players.subList(2,4):players.subList(1,2);
            Match match=new Match(doubles?"2v2":"1v1",null,red,blue);
            for(UUID id:players){Player member=Bukkit.getPlayer(id);if(member==null)continue;matches.put(id,match);places.put(id,Place.MATCH);removeMenuCompass(member);member.getInventory().clear();applyKit(member);giveAbility(member);member.setInvulnerable(true);}
            for(int i=0;i<red.size();i++){Player member=Bukkit.getPlayer(red.get(i));if(member!=null)member.teleport(redSpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);}
            for(int i=0;i<blue.size();i++){Player member=Bukkit.getPlayer(blue.get(i));if(member!=null)member.teleport(blueSpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);}
            startRound(match);
        }
    }

    private void startRound(Match match) {
        match.paused=true; match.down.clear(); int round=match.round;
        Bukkit.getScheduler().runTaskLater(this,()->{
            if(match.finished || match.round!=round) return;
            match.paused=false;
            for(UUID id:allPlayers(match)){Player p=Bukkit.getPlayer(id);if(p!=null){p.setInvulnerable(false);p.setGameMode(GameMode.SURVIVAL);p.setHealth(maxHealth(p));p.setFoodLevel(20);p.setSaturation(20);applyKit(p);giveAbility(p);p.sendTitle("§6RAUNT "+match.round,"§7Üç raunt • İlk iki puan kazanır",5,35,10);}}
        },80L);
    }
    private Set<UUID> allPlayers(Match m) { Set<UUID> all=new LinkedHashSet<>(m.red);all.addAll(m.blue);return all; }
    private double maxHealth(Player p) { var a=p.getAttribute(Attribute.MAX_HEALTH);return a==null?20:a.getValue(); }
    private void winRound(Match m, List<UUID> winners) {
        if(m.finished||m.paused)return; m.paused=true; if(winners==m.red || m.red.contains(winners.get(0)))m.redScore++;else m.blueScore++;
        String winning=m.red.contains(winners.get(0))?"Kırmızı":"Mavi";
        for(UUID id:allPlayers(m)){Player p=Bukkit.getPlayer(id);if(p!=null){p.setInvulnerable(true);p.getInventory().clear();p.sendMessage(Component.text(winning+" takım rauntu kazandı. Skor: "+m.redScore+" - "+m.blueScore,NamedTextColor.GOLD));}}
        if(m.redScore>=2||m.blueScore>=2||m.round>=3){finishMatch(m);return;}
        m.round++; m.down.clear();
        Bukkit.getScheduler().runTaskLater(this,()->{if(m.finished)return;for(UUID id:m.red){Player p=Bukkit.getPlayer(id);if(p!=null)p.teleport(redSpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);}for(UUID id:m.blue){Player p=Bukkit.getPlayer(id);if(p!=null)p.teleport(blueSpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);}startRound(m);},80L);
    }
    private void finishMatch(Match m) {
        if(m.finished)return;m.finished=true;String result=m.redScore==m.blueScore?"Berabere":m.redScore>m.blueScore?"Kırmızı takım kazandı":"Mavi takım kazandı";
        for(UUID id:allPlayers(m)){Player p=Bukkit.getPlayer(id);matches.remove(id);if(p!=null){p.sendTitle("§6MAÇ BİTTİ","§f"+result+" §7(§e"+m.redScore+" - "+m.blueScore+"§7)",5,50,15);Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline())toArenaLobby(p);},60L);}}
    }

    private void joinX(Player p) {
        requireLobby(p); if(xSpawns.isEmpty())throw new IllegalArgumentException("X Arena doğma noktası ayarlı değil. Yönetici /arena doğma ekle xarena kullanmalı.");
        places.put(p.getUniqueId(),Place.XARENA);removeMenuCompass(p);p.getInventory().clear();p.setGameMode(GameMode.SURVIVAL);p.setInvulnerable(false);teleportRandom(p);applyKit(p);giveAbility(p);updateScoreboard(p);tell(p,"X Arena'ya girdin. Öldürmelerin kalıcı olarak kaydedilir. Menü: /arena • Çıkış: /arena lobi veya /lobi");
    }
    private void teleportRandom(Player p) { if(xSpawns.isEmpty())return;Location at=xSpawns.get(ThreadLocalRandom.current().nextInt(xSpawns.size()));p.teleport(at,PlayerTeleportEvent.TeleportCause.PLUGIN);p.setFallDistance(0); }
    private void arenaLobby(Player p) {
        Place place=places.get(p.getUniqueId());
        if(place==Place.MATCH){Match match=matches.get(p.getUniqueId());if(match!=null)finishMatch(match);return;}
        if(place==Place.WAITING){Room room=roomsByPlayer.remove(p.getUniqueId());if(room!=null){rooms.remove(room.code);roomsByPlayer.remove(room.host);Player host=Bukkit.getPlayer(room.host);if(host!=null&&host.isOnline()){roomsByPlayer.remove(host.getUniqueId());toArenaLobby(host);tell(host,"Oda sahibi beklemekten vazgeçti; oda kapatıldı.");}}}
        toArenaLobby(p);
    }
    private void toArenaLobby(Player p) {
        if(!valid(lobbySpawn)){tell(p,"Arena lobi noktası ayarlı değil.");return;}
        Match old=matches.remove(p.getUniqueId());if(old!=null&&!old.finished){old.finished=true;for(UUID id:allPlayers(old)){matches.remove(id);Player other=Bukkit.getPlayer(id);if(other!=null&&other!=p)toArenaLobby(other);}}
        places.put(p.getUniqueId(),Place.LOBBY);clearScoreboard(p);p.setInvulnerable(false);p.setGameMode(GameMode.SURVIVAL);p.getInventory().clear();p.teleport(lobbySpawn,PlayerTeleportEvent.TeleportCause.PLUGIN);giveMenuCompass(p);tell(p,"Güvenli Arena lobisine döndün. Menü: pusula veya /arena");
    }

    private void openMenu(Player p) {
        Place place=places.get(p.getUniqueId());if(place==null){tell(p,"Arena'ya girmek için /oyunlar arena yaz.");return;}
        List<Button> buttons=new ArrayList<>();List<String> lines=new ArrayList<>();
        lines.add("X Arena: sınırsız PvP • ölümde rastgele yeniden doğma • eşya düşmez.");
        lines.add("1v1 özel oda: kod oluştur, arkadaşın /arena katıl <kod> yazar. Üç raunt oynanır.");
        lines.add("2v2: dört kişi sıra doldurunca maç başlar. VIP kit izni KasabaRoles üzerinden verilebilir.");
        lines.add("Seçili sınıf: "+roles.getOrDefault(p.getUniqueId(),Role.WARRIOR).label+" • Kit: "+selectedKit.getOrDefault(p.getUniqueId(),"normal"));
        if(place==Place.LOBBY){buttons.add(button("X Arena'ya gir",()->joinX(p)));buttons.add(button("1v1 oda kodu oluştur",()->createRoom(p)));buttons.add(new Button("Oda koduna katıl",response->{String code=response.getText("kod");if(code==null||!code.matches("[0-9]{6}"))throw new IllegalArgumentException("Altı haneli oda kodunu yaz.");joinRoom(p,code);}));buttons.add(button("1v1 sıraya gir",()->queueMatch(p,false)));buttons.add(button("2v2 sıraya gir",()->queueMatch(p,true)));}
        else if(place==Place.XARENA){buttons.add(button("Sınıf değiştir",()->showClasses(p)));buttons.add(button("Arena lobisine dön",()->arenaLobby(p)));buttons.add(button("1v1 oda oluştur",()->{toArenaLobby(p);createRoom(p);}));}
        else {buttons.add(button("Sınıf değiştir",()->showClasses(p)));buttons.add(button("Arena lobisine dön",()->arenaLobby(p)));}
        buttons.add(button("Normal kit",()->{selectedKit.put(p.getUniqueId(),"normal");if(place!=Place.LOBBY)applyKit(p);openMenu(p);}));
        if(p.hasPermission("kasabaarena.kit.vip"))buttons.add(button("VIP kit",()->{selectedKit.put(p.getUniqueId(),"vip");if(place!=Place.LOBBY)applyKit(p);openMenu(p);}));
        buttons.add(button("Sınıf seç",()->showClasses(p)));buttons.add(button("Kapat",()->{}));
        List<DialogInput> inputs=place==Place.LOBBY?List.of(DialogInput.text("kod",Component.text("Altı haneli oda kodu",NamedTextColor.WHITE)).maxLength(6).width(220).build()):List.of();
        show(p,"Kasaba • Arena",lines,inputs,buttons);
    }
    private void showClasses(Player p) {
        Role selected=roles.getOrDefault(p.getUniqueId(),Role.WARRIOR);List<Button> buttons=new ArrayList<>();
        for(Role role:Role.values())buttons.add(button(role.label+(role==selected?" ✓":""),()->{roles.put(p.getUniqueId(),role);if(places.get(p.getUniqueId())==Place.XARENA||places.get(p.getUniqueId())==Place.MATCH){applyKit(p);giveAbility(p);}tell(p,"Sınıfın: "+role.label);openMenu(p);}));
        buttons.add(button("Geri",()->openMenu(p)));show(p,"Karakterini seç",List.of("Savaşçı: yakındaki rakibe güçlü vuruş.","Suikastçı: kısa mesafeli atılma.","Tank: 3 saniye hasar azaltma.","Okçu: yakın rakibe üç ok atışı.","Yetenek eşyasına sağ tıkla veya /arena yetenek kullan."),List.of(),buttons);
    }
    private void chooseRole(Player p,String raw) { Role role;try{role=Role.valueOf(normalize(raw).toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){throw new IllegalArgumentException("Sınıflar: savaşçı, suikastçı, tank, okçu.");}roles.put(p.getUniqueId(),role);tell(p,"Sınıf seçildi: "+role.label); }

    private void show(Player p,String title,List<String> lines,List<DialogInput> inputs,List<Button> buttons) {
        long token=++pageSequence;pageTokens.put(p.getUniqueId(),token);List<ActionButton> actions=new ArrayList<>();
        for(Button button:buttons){DialogAction click=DialogAction.customClick((response,audience)->{if(!(audience instanceof Player actor)||!actor.getUniqueId().equals(p.getUniqueId())||!isEnabled())return;Bukkit.getScheduler().runTask(this,()->{if(!p.isOnline()||!Objects.equals(pageTokens.get(p.getUniqueId()),token))return;pageTokens.remove(p.getUniqueId());p.closeDialog();try{button.action().accept(response);}catch(IllegalArgumentException ex){tell(p,ex.getMessage());}});},ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(2)).build());actions.add(ActionButton.create(Component.text(button.label(),NamedTextColor.AQUA),null,150,click));}
        List<DialogBody> body=lines.stream().<DialogBody>map(line->DialogBody.plainMessage(Component.text(line,NamedTextColor.GRAY),320)).toList();
        p.showDialog(Dialog.create(builder->builder.empty().base(DialogBase.builder(Component.text(title,NamedTextColor.GOLD)).canCloseWithEscape(true).pause(false).afterAction(DialogBase.DialogAfterAction.NONE).body(body).inputs(inputs).build()).type(DialogType.multiAction(actions).columns(2).build())));
    }
    private Button button(String label,Runnable action){return new Button(label,response->action.run());}

    private void giveMenuCompass(Player p) {
        removeMenuCompass(p);if(places.get(p.getUniqueId())!=Place.LOBBY)return;ItemStack item=new ItemStack(Material.COMPASS);item.editMeta(meta->{meta.displayName(Component.text("Kasaba • Arena Menüsü",NamedTextColor.GOLD));meta.lore(List.of(Component.text("Sağ tıkla: oyun ve sınıf seç",NamedTextColor.AQUA)));meta.getPersistentDataContainer().set(menuKey,PersistentDataType.BYTE,(byte)1);});
        int slot=p.getInventory().getItem(8)==null?8:p.getInventory().firstEmpty();if(slot>=0&&slot<36)p.getInventory().setItem(slot,item);else tell(p,"Arena menü pusulası için envanterinde yer aç; /arena ile de açabilirsin.");
    }
    private boolean isMenu(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(menuKey);}
    private boolean isAbility(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(abilityKey);}
    private void removeMenuCompass(Player p){for(int i=0;i<p.getInventory().getSize();i++)if(isMenu(p.getInventory().getItem(i)))p.getInventory().setItem(i,null);if(isMenu(p.getItemOnCursor()))p.setItemOnCursor(null);}
    private void refreshCompasses(){for(Player p:Bukkit.getOnlinePlayers())if(places.get(p.getUniqueId())==Place.LOBBY){boolean found=false;for(ItemStack item:p.getInventory().getContents())if(isMenu(item)){if(found)p.getInventory().remove(item);else found=true;}if(!found)giveMenuCompass(p);}}

    private void applyKit(Player p) {
        String kit=selectedKit.getOrDefault(p.getUniqueId(),"normal");if(kit.equals("vip")&&!p.hasPermission("kasabaarena.kit.vip"))kit="normal";
        String path="kits."+kit;List<?> raw=getConfig().getList(path+".items");ItemStack[] contents=raw==null?null:raw.toArray(new ItemStack[0]);
        if(contents==null||Arrays.stream(contents).allMatch(i->i==null||i.getType().isAir())){p.getInventory().clear();p.getInventory().setItem(0,new ItemStack(Material.IRON_SWORD));p.getInventory().setItem(1,new ItemStack(Material.COOKED_BEEF,16));p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));}
        else {p.getInventory().clear();p.getInventory().setStorageContents(contents);List<?> armorRaw=getConfig().getList(path+".armor");p.getInventory().setArmorContents(armorRaw==null?new ItemStack[4]:armorRaw.toArray(new ItemStack[0]));}
    }
    private void giveAbility(Player p) {
        Role role=roles.getOrDefault(p.getUniqueId(),Role.WARRIOR);Material material=role==Role.ARCHER?Material.FEATHER:role==Role.TANK?Material.SHIELD:role==Role.ASSASSIN?Material.SUGAR:Material.BLAZE_POWDER;
        ItemStack item=new ItemStack(material);item.editMeta(meta->{meta.displayName(Component.text("Yetenek • "+role.label,NamedTextColor.LIGHT_PURPLE));meta.lore(List.of(Component.text("Sağ tıkla veya /arena yetenek",NamedTextColor.GRAY)));meta.getPersistentDataContainer().set(abilityKey,PersistentDataType.STRING,role.name());});
        for(int i=0;i<p.getInventory().getSize();i++)if(isAbility(p.getInventory().getItem(i)))p.getInventory().setItem(i,null);
        int slot=p.getInventory().firstEmpty();if(slot<0)slot=8;p.getInventory().setItem(slot,item);
    }
    private void useAbility(Player p) {
        Place place=places.get(p.getUniqueId());if(place!=Place.XARENA&&place!=Place.MATCH)throw new IllegalArgumentException("Yetenek sadece X Arena veya maçta kullanılabilir.");
        long now=System.currentTimeMillis();if(abilityReady.getOrDefault(p.getUniqueId(),0L)>now){tell(p,"Yetenek bekleme süresi: "+Math.max(1,(abilityReady.get(p.getUniqueId())-now+999)/1000)+" saniye.");return;}
        Role role=roles.getOrDefault(p.getUniqueId(),Role.WARRIOR);Player target=nearestOpponent(p,role==Role.ARCHER?24:6);
        switch(role){
            case WARRIOR -> {if(target==null)throw new IllegalArgumentException("Yakında vurabileceğin rakip yok.");target.damage(7,p);target.setVelocity(p.getLocation().getDirection().multiply(-0.7).setY(0.25));}
            case ASSASSIN -> {p.setVelocity(p.getLocation().getDirection().multiply(1.65).setY(0.3));p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED,40,1));}
            case TANK -> p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE,60,2));
            case ARCHER -> {if(target==null)throw new IllegalArgumentException("24 blok içinde rakip yok.");for(int i=-1;i<=1;i++){org.bukkit.util.Vector direction=target.getEyeLocation().toVector().subtract(p.getEyeLocation().toVector()).normalize().add(new org.bukkit.util.Vector(i*0.05,0,0));Arrow arrow=p.getWorld().spawnArrow(p.getEyeLocation(),direction,2.2f,1.0f);arrow.setShooter(p);arrow.setDamage(2.0);}}
        }
        abilityReady.put(p.getUniqueId(),now+(role==Role.ASSASSIN?9000:role==Role.TANK?14000:12000));tell(p,role.label+" yeteneğini kullandın.");
    }
    private Player nearestOpponent(Player p,double radius) {
        List<Player> candidates=new ArrayList<>();for(Entity e:p.getNearbyEntities(radius,Math.max(4,radius/2),radius))if(e instanceof Player other&&other!=p&&canFight(p,other))candidates.add(other);
        return candidates.stream().min(Comparator.comparingDouble(other -> other.getLocation().distanceSquared(p.getLocation()))).orElse(null);
    }
    private boolean canFight(Player a,Player b) {
        Place one=places.get(a.getUniqueId()),two=places.get(b.getUniqueId());if(one==Place.XARENA&&two==Place.XARENA)return true;
        Match ma=matches.get(a.getUniqueId()),mb=matches.get(b.getUniqueId());return ma!=null&&ma==mb&&!ma.paused&&!ma.sameSide(a.getUniqueId(),b.getUniqueId());
    }

    @EventHandler(priority=EventPriority.HIGHEST) public void compass(PlayerInteractEvent e) {
        if(isMenu(e.getItem())){e.setCancelled(true);if(e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK)openMenu(e.getPlayer());return;}
        if(isAbility(e.getItem())&&(e.getAction()==Action.RIGHT_CLICK_AIR||e.getAction()==Action.RIGHT_CLICK_BLOCK)){e.setCancelled(true);try{useAbility(e.getPlayer());}catch(IllegalArgumentException ex){tell(e.getPlayer(),ex.getMessage());}}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void inventoryClick(InventoryClickEvent e) {
        if(e.getWhoClicked() instanceof Player p&&(isMenu(e.getCurrentItem())||isMenu(e.getCursor())||isAbility(e.getCurrentItem())||isAbility(e.getCursor())))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void preventSpecialItemDrop(PlayerDropItemEvent e){if(isMenu(e.getItemDrop().getItemStack())||isAbility(e.getItemDrop().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void preventSpecialItemPickup(org.bukkit.event.entity.EntityPickupItemEvent e){if(isMenu(e.getItem().getItemStack())||isAbility(e.getItem().getItemStack()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e) {
        if(!(e.getEntity() instanceof Player target))return;
        if(regionContains("lobi",target.getLocation())||places.get(target.getUniqueId())==Place.LOBBY||places.get(target.getUniqueId())==Place.WAITING){e.setCancelled(true);return;}
        if(e instanceof EntityDamageByEntityEvent by){Player attacker=resolvePlayer(by.getDamager());if(attacker!=null&&(places.containsKey(attacker.getUniqueId())||places.containsKey(target.getUniqueId()))&&!canFight(attacker,target))e.setCancelled(true);}
    }
    private Player resolvePlayer(Entity entity){if(entity instanceof Player p)return p;if(entity instanceof Arrow arrow&&arrow.getShooter() instanceof Player p)return p;return null;}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(protectedBuild(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void placeBlock(BlockPlaceEvent e){if(protectedBuild(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    private boolean protectedBuild(Player p,Location at){Place place=places.get(p.getUniqueId());return regionContains("lobi",at)||regionContains("xarena",at)||place==Place.LOBBY||place==Place.WAITING;}
    private boolean regionContains(String name,Location at){Region region=regions.get(name);return region!=null&&region.contains(at);}

    @EventHandler(priority=EventPriority.HIGHEST) public void death(PlayerDeathEvent e) {
        Player p=e.getEntity();Place place=places.get(p.getUniqueId());if(place==null)return;e.getDrops().clear();e.setDroppedExp(0);e.setKeepInventory(true);e.setKeepLevel(true);
        if(place==Place.XARENA){Player killer=p.getKiller();if(killer!=null&&killer!=p&&places.get(killer.getUniqueId())==Place.XARENA){kills.merge(killer.getUniqueId(),1,Integer::sum);names.put(killer.getUniqueId(),killer.getName());saveStats();killer.sendMessage(Component.text("+1 öldürme • Toplam: "+kills.get(killer.getUniqueId()),NamedTextColor.GREEN));}p.getInventory().clear();}
        else if(place==Place.MATCH){p.getInventory().clear();Match match=matches.get(p.getUniqueId());if(match!=null){match.down.add(p.getUniqueId());Bukkit.getScheduler().runTask(this,()->{if(!match.finished&&!match.paused&&match.team(p.getUniqueId()).stream().allMatch(match.down::contains))winRound(match,match.opponents(p.getUniqueId()));});}}
    }
    @EventHandler(priority=EventPriority.HIGHEST) public void respawn(PlayerRespawnEvent e) {
        Player p=e.getPlayer();Place place=places.get(p.getUniqueId());
        if(place==Place.XARENA&&!xSpawns.isEmpty())e.setRespawnLocation(xSpawns.get(ThreadLocalRandom.current().nextInt(xSpawns.size())));
        else if(place==Place.MATCH){Match m=matches.get(p.getUniqueId());if(m!=null){List<Location> side=m.red.contains(p.getUniqueId())?List.of(redSpawn):List.of(blueSpawn);if(valid(side.get(0)))e.setRespawnLocation(side.get(0));}}
        else if(place==Place.LOBBY&&valid(lobbySpawn))e.setRespawnLocation(lobbySpawn);
        Bukkit.getScheduler().runTask(this,()->{if(!p.isOnline())return;Place current=places.get(p.getUniqueId());if(current==Place.XARENA){teleportRandom(p);applyKit(p);giveAbility(p);p.setInvulnerable(true);long duration=Math.max(0,getConfig().getLong("ayarlar.yeniden-dogma-koruma-saniye",3))*20;Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&places.get(p.getUniqueId())==Place.XARENA)p.setInvulnerable(false);},duration);updateScoreboard(p);showClasses(p);}
            else if(current==Place.MATCH){Match m=matches.get(p.getUniqueId());if(m!=null){applyKit(p);giveAbility(p);p.setInvulnerable(m.paused);if(!m.paused){p.setInvulnerable(true);Bukkit.getScheduler().runTaskLater(this,()->{if(p.isOnline()&&matches.get(p.getUniqueId())==m&&!m.paused)p.setInvulnerable(false);},40);}}}
            else if(current==Place.LOBBY)giveMenuCompass(p);
        });
    }
    @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTaskLater(this,()->recoverSnapshot(e.getPlayer()),2);}
    @EventHandler(priority=EventPriority.MONITOR) public void quit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();pageTokens.remove(id);Place place=places.get(id);if(place==Place.XARENA)clearScoreboard(e.getPlayer());if(place==Place.WAITING){Room room=roomsByPlayer.get(id);if(room!=null){rooms.remove(room.code);roomsByPlayer.remove(room.host);}}oneQueue.remove(id);twoQueue.remove(id);}

    private void loadStats(){kills.clear();names.clear();ConfigurationSection k=getConfig().getConfigurationSection("stats.kills"),n=getConfig().getConfigurationSection("stats.names");if(k!=null)for(String key:k.getKeys(false))try{kills.put(UUID.fromString(key),k.getInt(key));}catch(IllegalArgumentException ignored){}if(n!=null)for(String key:n.getKeys(false))try{names.put(UUID.fromString(key),n.getString(key,"Oyuncu"));}catch(IllegalArgumentException ignored){}}
    private void saveStats(){for(UUID id:new HashSet<>(kills.keySet())){getConfig().set("stats.kills."+id,kills.get(id));getConfig().set("stats.names."+id,names.getOrDefault(id,"Oyuncu"));}saveConfig();}
    private void refreshScoreboards(){for(Player p:Bukkit.getOnlinePlayers())if(places.get(p.getUniqueId())==Place.XARENA)updateScoreboard(p);}
    private void updateScoreboard(Player p){
        UUID playerId=p.getUniqueId();Scoreboard board=p.getScoreboard();
        if(!scoreboards.contains(playerId)){
            Scoreboard previous=board==null?Bukkit.getScoreboardManager().getMainScoreboard():board;board=Bukkit.getScoreboardManager().getNewScoreboard();
            for(Team source:previous.getTeams()){Team copy=board.registerNewTeam(source.getName());copy.setDisplayName(source.getDisplayName());copy.setPrefix(source.getPrefix());copy.setSuffix(source.getSuffix());copy.setColor(source.getColor());copy.setNameTagVisibility(source.getNameTagVisibility());copy.setOption(Team.Option.COLLISION_RULE,source.getOption(Team.Option.COLLISION_RULE));for(String entry:source.getEntries())copy.addEntry(entry);}
            previousBoards.put(playerId,previous);p.setScoreboard(board);
        }
        Objective objective=board.getObjective("kasaba_arena");
        if(objective==null)objective=board.registerNewObjective("kasaba_arena",Criteria.DUMMY,Component.text("X Arena • Öldürme",NamedTextColor.GOLD));
        objective.displayName(Component.text("X Arena • Öldürme",NamedTextColor.GOLD));objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        for(String entry:boardEntries.getOrDefault(p.getUniqueId(),List.of()))board.resetScores(entry);
        List<UUID> top=kills.keySet().stream().sorted(Comparator.<UUID>comparingInt(id->kills.getOrDefault(id,0)).reversed().thenComparing(id->names.getOrDefault(id,""),String.CASE_INSENSITIVE_ORDER)).limit(5).toList();
        List<String> entries=new ArrayList<>();
        if(top.isEmpty()){String entry="§7Henüz öldürme yok";objective.getScore(entry).setScore(0);entries.add(entry);}else{int rank=1;for(UUID id:top){String name=names.getOrDefault(id,Bukkit.getOfflinePlayer(id).getName());if(name==null)name="Oyuncu";String entry="§e"+(rank++)+". §f"+name.substring(0,Math.min(14,name.length()));objective.getScore(entry).setScore(kills.getOrDefault(id,0));entries.add(entry);}}
        boardEntries.put(playerId,entries);scoreboards.add(playerId);
    }
    private void clearScoreboard(Player p){UUID id=p.getUniqueId();if(!scoreboards.remove(id))return;Scoreboard board=p.getScoreboard();for(String entry:boardEntries.getOrDefault(id,List.of()))board.resetScores(entry);boardEntries.remove(id);Objective objective=board==null?null:board.getObjective("kasaba_arena");if(objective!=null)objective.unregister();Scoreboard previous=previousBoards.remove(id);if(previous!=null&&p.isOnline())p.setScoreboard(previous);}

    private void saveSnapshot(Player p) {
        String path="players."+p.getUniqueId();snapshots.set(path+".items",p.getInventory().getStorageContents());snapshots.set(path+".armor",p.getInventory().getArmorContents());snapshots.set(path+".offhand",p.getInventory().getItemInOffHand());snapshots.set(path+".level",p.getLevel());snapshots.set(path+".exp",p.getExp());snapshots.set(path+".food",p.getFoodLevel());snapshots.set(path+".saturation",p.getSaturation());snapshots.set(path+".health",p.getHealth());snapshots.set(path+".gamemode",p.getGameMode().name());saveSnapshots();
    }
    private void restoreSnapshot(Player p) {
        String path="players."+p.getUniqueId();if(!snapshots.contains(path))return;
        List<?> items=snapshots.getList(path+".items");p.getInventory().clear();if(items!=null)p.getInventory().setStorageContents(items.toArray(new ItemStack[0]));List<?> armor=snapshots.getList(path+".armor");p.getInventory().setArmorContents(armor==null?new ItemStack[4]:armor.toArray(new ItemStack[0]));p.getInventory().setItemInOffHand(snapshots.getItemStack(path+".offhand"));p.setLevel(snapshots.getInt(path+".level"));p.setExp((float)snapshots.getDouble(path+".exp"));p.setFoodLevel(snapshots.getInt(path+".food",20));p.setSaturation((float)snapshots.getDouble(path+".saturation",20));p.setHealth(Math.min(maxHealth(p),Math.max(1,snapshots.getDouble(path+".health",20))));try{p.setGameMode(GameMode.valueOf(snapshots.getString(path+".gamemode","SURVIVAL")));}catch(IllegalArgumentException ignored){}
        p.setInvulnerable(false);snapshots.set(path,null);saveSnapshots();
    }
    private void recoverSnapshot(Player p){if(snapshots.contains("players."+p.getUniqueId())){restoreSnapshot(p);tell(p,"Önceki Arena oturumundaki eşyaların geri yüklendi.");}}
    private void saveSnapshots(){try{snapshots.save(snapshotFile);}catch(IOException ex){getLogger().log(Level.SEVERE,"Oyuncu yedekleri kaydedilemedi.",ex);}}
    private void cleanupPlayer(UUID id,boolean restore){
        Player p=Bukkit.getPlayer(id);places.remove(id);roles.remove(id);selectedKit.remove(id);abilityReady.remove(id);pageTokens.remove(id);oneQueue.remove(id);twoQueue.remove(id);if(p!=null){clearScoreboard(p);removeMenuCompass(p);if(restore)restoreSnapshot(p);}
        Room room=roomsByPlayer.remove(id);if(room!=null){rooms.remove(room.code);Player host=Bukkit.getPlayer(room.host);if(host!=null&&host.isOnline()&&!room.host.equals(id)){roomsByPlayer.remove(room.host);toArenaLobby(host);}}
        Match match=matches.remove(id);if(match!=null&&!match.finished){match.finished=true;for(UUID other:allPlayers(match)){matches.remove(other);Player opponent=Bukkit.getPlayer(other);if(opponent!=null&&opponent.isOnline()&&!other.equals(id))toArenaLobby(opponent);}}
    }
}
