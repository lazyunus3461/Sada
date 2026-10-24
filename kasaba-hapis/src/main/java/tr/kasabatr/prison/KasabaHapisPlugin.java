/*
 * Decompiled with CFR 0.152.
 */
package tr.kasabatr.prison;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.hub.api.HubApi;

public final class KasabaHapisPlugin
extends JavaPlugin
implements Listener,
TabExecutor, tr.kasaba.hub.api.GameAccessGuard {
    private static final String ADMIN = "kasabatr.hapis.admin";
    private static final int CELL_COUNT = 16;
    private static final Pattern DURATION = Pattern.compile("^([0-9]+)(?:dk|m)?$", 2);
    private static final Pattern PLAYER_NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private final Map<UUID, Sentence> sentences = new ConcurrentHashMap<UUID, Sentence>();
    private final Map<UUID, PendingReturn> pendingReturns = new ConcurrentHashMap<UUID, PendingReturn>();
    private final Set<UUID> internalTeleports = new HashSet<UUID>();
    private final Set<UUID> authReady=new HashSet<>();
    private boolean storageReady;
    private World prison;
    private HubApi hub;
    private File dataFile;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.dataFile = new File(this.getDataFolder(), "sentences.yml");
        try{this.loadData();storageReady=true;EmbeddedWorld.install(this,getConfig().getString("world-name"));}catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"Hapishane kayıtları/haritası açılamadı; dosyalar korunuyor.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        String string = this.getConfig().getString("world-name", "");
        if (!string.matches("[A-Za-z0-9_-]{1,80}")) {
            this.getLogger().severe("Geçersiz hapishane dünya adı.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Path path = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path path2 = path.resolve(string).normalize();
        if (!path2.getParent().equals(path) || !Files.isRegularFile(path2.resolve("level.dat"), new LinkOption[0])) {
            this.getLogger().severe("Hapishane dünyası bulunamadı: " + String.valueOf(path2));
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.prison = Bukkit.getWorld(string);
        if (this.prison == null) {
            this.prison = Bukkit.createWorld(new WorldCreator(string).generator(new EmptyWorldGenerator()).generateStructures(false));
        }
        if (this.prison == null) {
            this.getLogger().severe("Hapishane dünyası açılamadı: " + string);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.prison.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        this.prison.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        this.prison.setGameRule(GameRule.KEEP_INVENTORY, true);
        this.prison.setTime(6000L);
        this.prison.getWorldBorder().setCenter(0.5, 0.5);
        this.prison.getWorldBorder().setSize(155.0);
        this.prison.setSpawnLocation(new Location(this.prison, 0.5, 66.0, 40.5));
        this.hub = Bukkit.getServicesManager().load(HubApi.class);
        if (this.hub == null) {
            this.getLogger().warning("KasabaHub hizmeti bulunamadı; /lobi yedeği kullanılacak.");
        }
        Bukkit.getServicesManager().register(tr.kasaba.hub.api.GameAccessGuard.class,this,this,org.bukkit.plugin.ServicePriority.Normal);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (this.getCommand("hapis") != null) {
            this.getCommand("hapis").setExecutor(this);
        }
        Bukkit.getScheduler().runTaskTimer((Plugin)this, this::tick, 20L, 20L);
        this.getLogger().info("Hapishane açıldı: " + this.prison.getName() + ", hücre: 16, kayıtlı ceza: " + this.sentences.size());
    }

    @Override
    public void onDisable() {
        Bukkit.getServicesManager().unregisterAll(this);
        if (this.dataFile == null||!storageReady) {
            return;
        }
        try {
            this.saveData();
        }
        catch (IOException iOException) {
            this.getLogger().severe("Ceza dosyası kaydedilemedi: " + iOException.getMessage());
        }
    }

    private static GameMode mode(String string) {
        if (string == null || string.isBlank()) {
            return null;
        }
        try {
            return GameMode.valueOf(string);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private void loadData()throws Exception{
        if(!dataFile.isFile())return;YamlConfiguration data=new YamlConfiguration();data.load(dataFile);
        ConfigurationSection section=data.getConfigurationSection("sentences");if(section!=null)for(String key:section.getKeys(false)){UUID id=UUID.fromString(key);String path="sentences."+key+".";long expiry=data.getLong(path+"expires-at");int cell=data.getInt(path+"cell");if(expiry<1||cell<0||cell>=16)throw new IllegalArgumentException("Geçersiz ceza: "+key);sentences.put(id,new Sentence(id,data.getString(path+"name",key),expiry,cell,mode(data.getString(path+"original-mode"))));}
        section=data.getConfigurationSection("pending-returns");if(section!=null)for(String key:section.getKeys(false)){String path="pending-returns."+key+".";pendingReturns.put(UUID.fromString(key),new PendingReturn(data.getString(path+"name",key),mode(data.getString(path+"original-mode"))));}
    }
    private void saveData() throws IOException {
        String string;
        if (!this.getDataFolder().isDirectory() && !this.getDataFolder().mkdirs()) {
            throw new IOException("Eklenti klasörü oluşturulamadı");
        }
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("format-version", 1);
        for (Sentence object : this.sentences.values()) {
            string = "sentences." + String.valueOf(object.id) + ".";
            yamlConfiguration.set(string + "name", object.name);
            yamlConfiguration.set(string + "expires-at", object.expiresAt);
            yamlConfiguration.set(string + "cell", object.cell);
            yamlConfiguration.set(string + "original-mode", object.originalMode == null ? "" : object.originalMode.name());
        }
        for (Map.Entry entry : this.pendingReturns.entrySet()) {
            string = "pending-returns." + String.valueOf(entry.getKey()) + ".";
            yamlConfiguration.set(string + "name", ((PendingReturn)entry.getValue()).name());
            yamlConfiguration.set(string + "original-mode", ((PendingReturn)entry.getValue()).originalMode() == null ? "" : ((PendingReturn)entry.getValue()).originalMode().name());
        }
        Path path = new File(this.getDataFolder(), "sentences.yml.tmp").toPath();
        yamlConfiguration.save(path.toFile());
        try {
            Files.move(path, this.dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        catch (AtomicMoveNotSupportedException atomicMoveNotSupportedException) {
            Files.move(path, this.dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static long parseMinutes(String string, long l) {
        Matcher matcher = DURATION.matcher(string.trim());
        if (!matcher.matches()) {
            return -1L;
        }
        try {
            long l2 = Long.parseLong(matcher.group(1));
            return l2 >= 1L && l2 <= l ? l2 : -1L;
        }
        catch (NumberFormatException numberFormatException) {
            return -1L;
        }
    }

    private int freeCell() {
        int[] nArray = new int[16];
        long l = System.currentTimeMillis();
        for (Sentence sentence : this.sentences.values()) {
            if (sentence.expiresAt <= l) continue;
            int n = sentence.cell;
            nArray[n] = nArray[n] + 1;
        }
        int n = 0;
        for (int i = 1; i < nArray.length; ++i) {
            if (nArray[i] >= nArray[n]) continue;
            n = i;
        }
        return n;
    }

    private Location cellLocation(int n) {
        int n2 = -50 + 14 * (n % 8);
        int n3 = n < 8 ? -44 : -17;
        return new Location(this.prison, (double)n2 + 0.5, 65.0, (double)n3 + 0.5, n < 8 ? 0.0f : 180.0f, 0.0f);
    }

    private boolean inCell(Location location, int n) {
        if (location == null || location.getWorld() != this.prison) {
            return false;
        }
        int n2 = -56 + 14 * (n % 8);
        int n3 = n < 8 ? -52 : -25;
        int n4 = location.getBlockX();
        int n5 = location.getBlockY();
        int n6 = location.getBlockZ();
        return n4 >= n2 + 1 && n4 <= n2 + 12 && n6 >= n3 + 1 && n6 <= n3 + 14 && n5 >= 65 && n5 <= 73;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void teleportInternal(Player player, Location location) {
        UUID uUID = player.getUniqueId();
        this.internalTeleports.add(uUID);
        try {
            if(!player.teleport(location))throw new IllegalStateException("Hapishane ışınlanması engellendi.");
        }
        finally {
            this.internalTeleports.remove(uUID);
        }
    }

    private void enforce(Player player, Sentence sentence) {
        if (sentence.originalMode == null) {
            sentence.originalMode = player.getGameMode();
            try {
                this.saveData();
            }
            catch (IOException iOException) {
                this.getLogger().severe("Oyun modu kaydı yazılamadı: " + iOException.getMessage());
            }
        }
        if(!authenticated(player)||!authReady.contains(player.getUniqueId()))return;
        if (player.getGameMode() != GameMode.ADVENTURE) {
            player.setGameMode(GameMode.ADVENTURE);
        }
        if (!this.inCell(player.getLocation(), sentence.cell)) {
            this.teleportInternal(player, this.cellLocation(sentence.cell));
        }
        player.setFlying(false);player.setAllowFlight(false);player.setFoodLevel(20);
    }

    private void tick() {
        long l = System.currentTimeMillis();
        for (Sentence sentence : List.copyOf(this.sentences.values())) {
            Player player = Bukkit.getPlayer(sentence.id);
            if (player == null || !player.isOnline()||!authenticated(player)||!authReady.contains(player.getUniqueId())) continue;
            if (l >= sentence.expiresAt) {
                this.release(sentence.id, "Cezan bitti.");
                continue;
            }
            this.enforce(player, sentence);
        }
    }

    private void tell(CommandSender commandSender, String string) {
        commandSender.sendMessage("§8[§cHapishane§8] §f" + string);
    }

    private UUID findRecorded(String string) {
        for (Sentence object : this.sentences.values()) {
            if (!object.name.equalsIgnoreCase(string) && !object.id.toString().equalsIgnoreCase(string)) continue;
            return object.id;
        }
        for (Map.Entry entry : this.pendingReturns.entrySet()) {
            if (!((PendingReturn)entry.getValue()).name().equalsIgnoreCase(string) && !((UUID)entry.getKey()).toString().equalsIgnoreCase(string)) continue;
            return (UUID)entry.getKey();
        }
        return null;
    }

    private void sendToLobby(Player player) {
        UUID uUID = player.getUniqueId();
        if (this.hub != null) {
            try {
                this.hub.returnToLobby(uUID);
            }
            catch (RuntimeException runtimeException) {
                this.getLogger().warning("Hub lobi dönüşü: " + runtimeException.getMessage());
            }
        }
        if (player.getWorld() == this.prison) {
            player.performCommand("lobi");
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
            if (!player.isOnline() || player.getWorld() != this.prison || this.sentences.containsKey(uUID)) {
                return;
            }
            Location location = this.configuredLobby();
            if (location == null) {
                for (World world : Bukkit.getWorlds()) {
                    if (world == this.prison) continue;
                    location = world.getSpawnLocation();
                    break;
                }
            }
            if (location != null) {
                this.teleportInternal(player, location);
            } else {
                this.getLogger().severe("Lobi bulunamadı: " + player.getName());
            }
        }, 3L);
    }

    private Location configuredLobby() {
        String string = this.getConfig().getString("lobby.world", "");
        if (string == null || string.isBlank()) {
            return null;
        }
        World world = Bukkit.getWorld(string);
        if (world == null || world == this.prison) {
            return null;
        }
        return new Location(world, this.getConfig().getDouble("lobby.x"), this.getConfig().getDouble("lobby.y"), this.getConfig().getDouble("lobby.z"), (float)this.getConfig().getDouble("lobby.yaw"), (float)this.getConfig().getDouble("lobby.pitch"));
    }

    private boolean release(UUID uUID, String string) {
        Sentence sentence = this.sentences.remove(uUID);
        if (sentence == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(uUID);
        if (player == null || !player.isOnline()||!authenticated(player)) {
            this.pendingReturns.put(uUID, new PendingReturn(sentence.name, sentence.originalMode));
        }
        try {
            this.saveData();
        }
        catch (IOException iOException) {
            this.sentences.put(uUID, sentence);
            this.pendingReturns.remove(uUID);
            this.getLogger().severe("Serbest bırakma kaydedilemedi: " + iOException.getMessage());
            return false;
        }
        if (player != null && player.isOnline()&&authenticated(player)) {
            if (sentence.originalMode != null) {
                player.setGameMode(sentence.originalMode);
            }
            this.sendToLobby(player);
            this.tell(player, string + " Lobiye döndün.");
        }
        return true;
    }

    private void finishPending(Player player) {
        UUID uUID = player.getUniqueId();
        PendingReturn pendingReturn = this.pendingReturns.remove(uUID);
        if (pendingReturn == null) {
            return;
        }
        try {
            this.saveData();
        }
        catch (IOException iOException) {
            this.pendingReturns.put(uUID, pendingReturn);
            this.getLogger().severe("Bekleyen dönüş kaydı silinemedi: " + iOException.getMessage());
            return;
        }
        if (pendingReturn.originalMode() != null) {
            player.setGameMode(pendingReturn.originalMode());
        }
        this.sendToLobby(player);
        this.tell(player, "Cezan sona erdi. Lobiye döndün.");
    }

    private void status(CommandSender commandSender, String string) {
        UUID uUID = this.findRecorded(string);
        if (uUID == null) {
            this.tell(commandSender, "Bu oyuncuya ait hapis kaydı yok.");
            return;
        }
        PendingReturn pendingReturn = this.pendingReturns.get(uUID);
        if (pendingReturn != null) {
            this.tell(commandSender, pendingReturn.name() + " serbest; girişte lobiye dönecek.");
            return;
        }
        Sentence sentence = this.sentences.get(uUID);
        if (sentence == null) {
            this.tell(commandSender, "Kayıt bulunamadı.");
            return;
        }
        long l = Math.max(0L, (sentence.expiresAt - System.currentTimeMillis() + 999L) / 1000L);
        this.tell(commandSender, sentence.name + " • Hücre " + (sentence.cell + 1) + " • Kalan " + l / 60L + " dk " + l % 60L + " sn" + (l == 0L ? " (girişte serbest)" : ""));
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        GameMode gameMode;
        int n;
        Object object;
        String string2;
        UUID uUID;
        if (stringArray.length == 0) {
            this.tell(commandSender, "Kullanım: /hapis <oyuncu> <dakika|60dk> • /hapis durum [oyuncu] • /hapis birak <oyuncu>");
            return true;
        }
        String string3 = norm(stringArray[0]);
        if (string3.equals("durum") || string3.equals("status")) {
            if (stringArray.length == 1 && commandSender instanceof Player) {
                Player player = (Player)commandSender;
                this.status(commandSender, player.getUniqueId().toString());
            } else if (stringArray.length == 2 && commandSender.hasPermission(ADMIN)) {
                this.status(commandSender, stringArray[1]);
            } else {
                this.tell(commandSender, "Kullanım: /hapis durum [oyuncu]");
            }
            return true;
        }
        if (!commandSender.hasPermission(ADMIN)) {
            this.tell(commandSender, "Bu komut için yetkin yok.");
            return true;
        }
        if (string3.equals("setlobi")) {
            Player player;
            if (!(commandSender instanceof Player) || (player = (Player)commandSender).getWorld() == this.prison) {
                this.tell(commandSender, "Ana lobide durup /hapis setlobi yaz.");
                return true;
            }
            Location location = player.getLocation();
            this.getConfig().set("lobby.world", location.getWorld().getName());
            this.getConfig().set("lobby.x", location.getX());
            this.getConfig().set("lobby.y", location.getY());
            this.getConfig().set("lobby.z", location.getZ());
            this.getConfig().set("lobby.yaw", Float.valueOf(location.getYaw()));
            this.getConfig().set("lobby.pitch", Float.valueOf(location.getPitch()));
            this.saveConfig();
            this.tell(commandSender, "Ceza sonrası dönüş noktası ana lobi olarak kaydedildi.");
            return true;
        }
        if (string3.equals("birak") || string3.equals("serbest") || string3.equals("release")) {
            if (stringArray.length != 2) {
                this.tell(commandSender, "Kullanım: /hapis birak <oyuncu>");
                return true;
            }
            UUID uUID2 = this.findRecorded(stringArray[1]);
            if (uUID2 == null) {
                this.tell(commandSender, "Aktif hapis kaydı bulunamadı.");
                return true;
            }
            if (this.sentences.containsKey(uUID2)) {
                if (this.release(uUID2, "Yönetici seni serbest bıraktı.")) {
                    this.tell(commandSender, "Oyuncu serbest bırakıldı.");
                } else {
                    this.tell(commandSender, "Ceza kaydedilemedi; sunucu günlüğünü kontrol et.");
                }
            } else {
                this.tell(commandSender, "Oyuncu zaten serbest; girişte lobiye dönecek.");
            }
            return true;
        }
        if (stringArray.length != 2 || !PLAYER_NAME.matcher(stringArray[0]).matches()) {
            this.tell(commandSender, "Kullanım: /hapis <oyuncu> <dakika|60dk>");
            return true;
        }
        long l = KasabaHapisPlugin.parseMinutes(stringArray[1], this.getConfig().getLong("max-minutes", 43200L));
        if (l < 1L) {
            this.tell(commandSender, "Süre 1–43200 dakika olmalı. Örnek: /hapis Oyuncu 60dk");
            return true;
        }
        Player player = Bukkit.getPlayerExact(stringArray[0]);
        if (player != null) {
            uUID = player.getUniqueId();
            string2 = player.getName();
        } else {
            org.bukkit.OfflinePlayer offline = Bukkit.getOfflinePlayer(stringArray[0]);
            if (!offline.hasPlayedBefore()) {
                this.tell(commandSender, "Bu oyuncu daha önce sunucuya girmemiş; UUID güvenle doğrulanamadı.");
                return true;
            }
            uUID = offline.getUniqueId();
            string2 = offline.getName() == null ? stringArray[0] : offline.getName();
        }
        object = this.sentences.get(uUID);
        if (player != null && object == null && this.hub != null && this.hub.playing(uUID)) {
            try {
                this.hub.returnToLobby(uUID);
            }
            catch (RuntimeException runtimeException) {
                this.tell(commandSender, "Mevcut oyundan güvenle çıkılamadı: " + runtimeException.getMessage());
                return true;
            }
        }
        int n2 = n = object == null ? this.freeCell() : ((Sentence)object).cell;
        gameMode = object == null ? (player == null||!authenticated(player) ? null : player.getGameMode()) : ((Sentence)object).originalMode;
        if (gameMode == null && this.pendingReturns.containsKey(uUID)) {
            gameMode = this.pendingReturns.get(uUID).originalMode();
        }
        long l2 = System.currentTimeMillis() + l * 60000L;
        Sentence sentence = new Sentence(uUID, string2, l2, n, gameMode);
        this.sentences.put(uUID, sentence);
        PendingReturn pendingReturn = this.pendingReturns.remove(uUID);
        try {
            this.saveData();
        }
        catch (IOException iOException) {
            if (object == null) {
                this.sentences.remove(uUID);
            } else {
                this.sentences.put(uUID, (Sentence)object);
            }
            if (pendingReturn != null) {
                this.pendingReturns.put(uUID, pendingReturn);
            }
            this.tell(commandSender, "Ceza dosyası yazılamadı: " + iOException.getMessage());
            return true;
        }
        if (player != null && player.isOnline()&&authenticated(player)) {
            player.closeDialog();player.closeInventory();authReady.add(uUID);this.enforce(player, sentence);
            this.tell(player, "§c" + l + " dakika hapis cezası aldın. §fKalan süre: /hapis durum");
        }
        this.tell(commandSender, string2 + " oyuncusu " + l + " dakika cezalandırıldı (hücre " + (n + 1) + ")." + (player == null ? " Girişte hapishaneye alınacak." : ""));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String string2, String[] stringArray) {
        if (stringArray.length == 1) {
            ArrayList<String> arrayList = new ArrayList<String>(List.of("durum"));
            if (commandSender.hasPermission(ADMIN)) {
                arrayList.addAll(List.of("birak", "setlobi"));
            }
            return arrayList.stream().filter(string -> string.startsWith(stringArray[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (stringArray.length == 2 && commandSender.hasPermission(ADMIN) && (stringArray[0].equalsIgnoreCase("birak") || stringArray[0].equalsIgnoreCase("durum"))) {
            return this.sentences.values().stream().map(sentence -> sentence.name).filter(string -> string.toLowerCase(Locale.ROOT).startsWith(stringArray[1].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }

    private boolean authenticated(Player p){var auth=Bukkit.getServicesManager().load(tr.kasaba.auth.api.AuthState.class);return auth!=null&&auth.authenticated(p.getUniqueId());}
    @Override public boolean blocked(UUID id){return sentences.containsKey(id)||pendingReturns.containsKey(id);}
    @Override public String message(UUID id){return "Hapis cezan bitene kadar oyunlara katılamazsın. /hapis durum";}
    @Override public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String name,String id){return new EmptyWorldGenerator();}
    @EventHandler public void multiverse(org.bukkit.event.server.PluginEnableEvent e){if(e.getPlugin().getName().equals("Multiverse-Core"))Bukkit.getScheduler().runTask(this,()->new MultiverseBridge(this).register(prison,prison.getSpawnLocation()));}
    @EventHandler public void onJoin(PlayerJoinEvent e){authReady.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void onQuit(org.bukkit.event.player.PlayerQuitEvent e){authReady.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.HIGHEST) public void verified(tr.kasaba.auth.api.PlayerAuthenticatedEvent e){Bukkit.getScheduler().runTask(this,()->arrival(e.getPlayer()));}
    private void arrival(Player p){if(!p.isOnline()||!authenticated(p))return;authReady.add(p.getUniqueId());if(pendingReturns.containsKey(p.getUniqueId())){finishPending(p);return;}Sentence sentence=sentences.get(p.getUniqueId());if(sentence==null)return;if(System.currentTimeMillis()>=sentence.expiresAt)release(sentence.id,"Cezan bitti.");else{p.closeDialog();p.closeInventory();enforce(p,sentence);tell(p,"Hapis cezan devam ediyor. /hapis durum");}}
    private static String norm(String s){return s.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    @EventHandler(ignoreCancelled=true) public void inventory(org.bukkit.event.inventory.InventoryClickEvent e){if(sentences.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler(ignoreCancelled=true) public void drag(org.bukkit.event.inventory.InventoryDragEvent e){if(sentences.containsKey(e.getWhoClicked().getUniqueId()))e.setCancelled(true);}
    @EventHandler
    public void onChangedWorld(PlayerChangedWorldEvent playerChangedWorldEvent) {
        Sentence sentence = this.sentences.get(playerChangedWorldEvent.getPlayer().getUniqueId());
        if (sentence == null || !authReady.contains(playerChangedWorldEvent.getPlayer().getUniqueId()) || playerChangedWorldEvent.getPlayer().getWorld() == this.prison) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            if (playerChangedWorldEvent.getPlayer().isOnline() && this.sentences.containsKey(playerChangedWorldEvent.getPlayer().getUniqueId())) {
                this.enforce(playerChangedWorldEvent.getPlayer(), sentence);
            }
        });
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onTeleport(PlayerTeleportEvent playerTeleportEvent) {
        UUID uUID = playerTeleportEvent.getPlayer().getUniqueId();
        if (this.sentences.containsKey(uUID) && authReady.contains(uUID) && !this.internalTeleports.contains(uUID)) {
            playerTeleportEvent.setCancelled(true);
            this.tell(playerTeleportEvent.getPlayer(), "Cezan bitene kadar ışınlanamazsın.");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Sentence sentence = this.sentences.get(playerMoveEvent.getPlayer().getUniqueId());
        if (sentence == null || !authReady.contains(playerMoveEvent.getPlayer().getUniqueId()) || internalTeleports.contains(playerMoveEvent.getPlayer().getUniqueId()) || playerMoveEvent.getTo() == null) {
            return;
        }
        if (!this.inCell(playerMoveEvent.getTo(), sentence.cell)) {
            playerMoveEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        Sentence sentence = this.sentences.get(playerRespawnEvent.getPlayer().getUniqueId());
        if (sentence != null) {
            playerRespawnEvent.setRespawnLocation(this.cellLocation(sentence.cell));
        }
    }

    @EventHandler
    public void onGameMode(PlayerGameModeChangeEvent playerGameModeChangeEvent) {
        if (this.sentences.containsKey(playerGameModeChangeEvent.getPlayer().getUniqueId()) && authReady.contains(playerGameModeChangeEvent.getPlayer().getUniqueId()) && playerGameModeChangeEvent.getNewGameMode() != GameMode.ADVENTURE) {
            playerGameModeChangeEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent playerCommandPreprocessEvent) {
        UUID uUID = playerCommandPreprocessEvent.getPlayer().getUniqueId();
        if (!this.sentences.containsKey(uUID)) {
            return;
        }
        String string = playerCommandPreprocessEvent.getMessage().trim().toLowerCase(Locale.ROOT);
        String[] stringArray = string.split("\\s+");
        String string2 = stringArray[0].substring(1);if(string2.contains(":"))string2=string2.substring(string2.indexOf(":")+1);string2=norm(string2);
        boolean bl = (string2.equals("hapis")||string2.equals("hapishane")) && stringArray.length == 2 && (stringArray[1].equals("durum") || stringArray[1].equals("status"));
        boolean bl2 = Set.of("kasaba", "giris", "kayit", "login", "register").contains(string2);
        if (!bl && !bl2) {
            playerCommandPreprocessEvent.setCancelled(true);
            this.tell(playerCommandPreprocessEvent.getPlayer(), "Cezan bitene kadar komut kullanamazsın. /hapis durum yazabilirsin.");
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onChat(AsyncPlayerChatEvent asyncPlayerChatEvent) {
        if (!this.sentences.containsKey(asyncPlayerChatEvent.getPlayer().getUniqueId())) {
            return;
        }
        asyncPlayerChatEvent.setCancelled(true);
        Bukkit.getScheduler().runTask((Plugin)this, () -> this.tell(asyncPlayerChatEvent.getPlayer(), "Cezan bitene kadar sohbet edemezsin."));
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    public void onPaperChat(AsyncChatEvent asyncChatEvent) {
        if (!this.sentences.containsKey(asyncChatEvent.getPlayer().getUniqueId())) {
            return;
        }
        asyncChatEvent.setCancelled(true);
        Bukkit.getScheduler().runTask((Plugin)this, () -> this.tell(asyncChatEvent.getPlayer(), "Cezan bitene kadar sohbet edemezsin."));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent blockBreakEvent) {
        if (this.sentences.containsKey(blockBreakEvent.getPlayer().getUniqueId()) || blockBreakEvent.getBlock().getWorld() == this.prison && !blockBreakEvent.getPlayer().hasPermission(ADMIN)) {
            blockBreakEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent blockPlaceEvent) {
        if (this.sentences.containsKey(blockPlaceEvent.getPlayer().getUniqueId()) || blockPlaceEvent.getBlock().getWorld() == this.prison && !blockPlaceEvent.getPlayer().hasPermission(ADMIN)) {
            blockPlaceEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent playerDropItemEvent) {
        if (this.sentences.containsKey(playerDropItemEvent.getPlayer().getUniqueId())) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent entityPickupItemEvent) {
        Player player;
        LivingEntity livingEntity = entityPickupItemEvent.getEntity();
        if (livingEntity instanceof Player && this.sentences.containsKey((player = (Player)livingEntity).getUniqueId())) {
            entityPickupItemEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent playerInteractEvent) {
        if (this.sentences.containsKey(playerInteractEvent.getPlayer().getUniqueId())) {
            playerInteractEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent playerInteractEntityEvent) {
        if (this.sentences.containsKey(playerInteractEntityEvent.getPlayer().getUniqueId())) {
            playerInteractEntityEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent vehicleEnterEvent) {
        Player player;
        Entity entity = vehicleEnterEvent.getEntered();
        if (entity instanceof Player && this.sentences.containsKey((player = (Player)entity).getUniqueId())) {
            vehicleEnterEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent entityDamageEvent) {
        Player player;
        Entity entity = entityDamageEvent.getEntity();
        if (entity instanceof Player && this.sentences.containsKey((player = (Player)entity).getUniqueId())) {
            entityDamageEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent foodLevelChangeEvent) {
        Player player;
        HumanEntity humanEntity = foodLevelChangeEvent.getEntity();
        if (humanEntity instanceof Player && this.sentences.containsKey((player = (Player)humanEntity).getUniqueId())) {
            foodLevelChangeEvent.setCancelled(true);
        }
    }

    private static final class Sentence {
        final UUID id;
        final String name;
        final long expiresAt;
        final int cell;
        GameMode originalMode;

        Sentence(UUID uUID, String string, long l, int n, GameMode gameMode) {
            this.id = uUID;
            this.name = string;
            this.expiresAt = l;
            this.cell = n;
            this.originalMode = gameMode;
        }
    }

    private record PendingReturn(String name, GameMode originalMode) {
    }
}
