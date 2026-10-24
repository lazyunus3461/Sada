/*
 * Decompiled with CFR 0.152.
 */
package tr.kasabatr.yaris;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import tr.kasaba.hub.api.GameInfo;
import tr.kasaba.hub.api.HubApi;
import tr.kasaba.hub.api.HubGame;
import tr.kasabatr.yaris.Mode;
import tr.kasabatr.yaris.PlayerBackup;
import tr.kasabatr.yaris.RaceMap;
import tr.kasabatr.yaris.RaceRoom;
import tr.kasabatr.yaris.WorldInstances;

public final class KasabaYarisPlugin
extends JavaPlugin
implements HubGame,
Listener,
TabExecutor {
    private static final String GAME_ID = "atyarisi";
    private static final String BOAT_ID="tekneyarisi";
    private GameScreens screens;
    private final Set<UUID> quitting=new HashSet<>();
    private final Map<UUID,List<UUID>> entryTeams=new HashMap<>();
    private final Map<Integer, RaceRoom> rooms = new LinkedHashMap<Integer, RaceRoom>();
    private final Map<UUID, RaceRoom> membership = new HashMap<UUID, RaceRoom>();
    private final Set<UUID> visitors = new HashSet<UUID>();
    private final Set<UUID> internalTeleports = new HashSet<UUID>();
    private int nextRoom = 101;
    private WorldInstances instances;
    private RaceMap map;
    private HubApi hub;
    private NamespacedKey menuItemKey;
    private boolean shuttingDown;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();screens=new GameScreens(this);
        try{EmbeddedWorld.install(this,getConfig().getString("template-world"));}catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"Yarış haritası kurulamadı; mevcut veriler korunuyor.",e);Bukkit.getPluginManager().disablePlugin(this);return;}
        this.menuItemKey = new NamespacedKey(this, "race_menu");
        File file = new File(this.getDataFolder(), "race-map.yml");
        if (!file.isFile()) {
            try {
                this.saveResource("race-map.yml", false);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                this.getLogger().severe("Pakette race-map.yml bulunamadı.");
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
        }
        try {
            this.map = new RaceMap(file);
        }
        catch (RuntimeException runtimeException) {
            this.getLogger().severe("Harita koordinatları okunamadı: " + runtimeException.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        if (!this.map.worldName.equals(this.getConfig().getString("template-world"))) {
            this.getLogger().severe("config.yml template-world ile race-map.yml world aynı olmalı.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.instances = new WorldInstances(this);
        this.instances.clearStaleCopies();
        if (this.instances.templateReady() && Bukkit.getWorld(this.map.worldName) == null) {
            try {
                Bukkit.createWorld(new WorldCreator(this.map.worldName).generator(new EmptyWorldGenerator()).generateStructures(false));
            }
            catch (RuntimeException runtimeException) {
                this.getLogger().severe("Harita yüklenemedi: " + runtimeException.getMessage());
            }
        }
        this.hub = Bukkit.getServicesManager().load(HubApi.class);
        if (this.hub == null) {
            this.getLogger().severe("KasabaHub API bulunamadı.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        try {
            this.hub.register(this, this);
            this.hub.register(this,new HubGame(){public GameInfo info(){return new GameInfo(BOAT_ID,"Tekne Yarışı","Ortak yarış lobisi • 2–5 kişi • iki tur",1,maximum());}public boolean available(){return KasabaYarisPlugin.this.available();}public boolean join(List<Player> team){return KasabaYarisPlugin.this.join(team);}public void leave(UUID id){KasabaYarisPlugin.this.leave(id);}public boolean lobby(UUID id){return KasabaYarisPlugin.this.lobby(id);}});
        }
        catch (RuntimeException runtimeException) {
            this.getLogger().severe("KasabaHub kayıt hatası: " + runtimeException.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("yaris").setExecutor(this);
        this.getCommand("yaris").setTabCompleter(this);
        Bukkit.getScheduler().runTaskTimer((Plugin)this, () -> {
            for (RaceRoom raceRoom : List.copyOf(this.rooms.values())) {
                raceRoom.tick();
            }
        }, 1L, 1L);
        if (!this.available()) {
            this.getLogger().warning("Yarış dünyası yok. ZIP'i sunucu köküne çıkar: " + this.map.worldName);
        }
    }

    @Override
    public void onDisable() {
        this.shuttingDown = true;
        if (this.hub != null) {
            try {
                this.hub.unregister(this, GAME_ID);this.hub.unregister(this,BOAT_ID);
            }
            catch (RuntimeException runtimeException) {
                this.getLogger().warning("Hub kaydı kaldırılamadı: " + runtimeException.getMessage());
            }
        }
        for (RaceRoom object : List.copyOf(this.rooms.values())) {
            this.closeRoom(object);
        }
        for (UUID uUID : List.copyOf(this.visitors)) {
            Player player = Bukkit.getPlayer(uUID);
            if (player == null) continue;
            this.detach(player, true);
        }
    }

    @Override
    public GameInfo info() {
        return new GameInfo(GAME_ID, "At Yarışı", "Ortak yarış lobisi • 2–5 kişi • iki tur", 1, this.maximum());
    }

    @Override
    public boolean available() {
        return this.map != null && this.instances != null && this.instances.templateReady() && Bukkit.getWorld(this.map.worldName) != null;
    }

    int minimum() {
        return Math.max(2, Math.min(5, this.getConfig().getInt("min-players", 2)));
    }

    int maximum() {
        return Math.max(this.minimum(), Math.min(5, this.getConfig().getInt("max-players", 5)));
    }

    int laps() {
        return Math.max(1, Math.min(5, this.getConfig().getInt("laps", 2)));
    }

    int timeLimitSeconds() {
        return Math.max(60, this.getConfig().getInt("race-time-limit-seconds", 600));
    }

    int resultsSeconds() {
        return Math.max(3, Math.min(60, this.getConfig().getInt("results-seconds", 12)));
    }

    RaceMap map() {
        return this.map;
    }

    WorldInstances instances() {
        return this.instances;
    }

    @Override
    public boolean join(List<Player> list) {
        if (!this.available() || list.isEmpty() || list.size() > this.maximum()) {
            return false;
        }
        for (Player audience : list) {
            if (authenticated(audience) && audience.isOnline() && !this.visitors.contains(audience.getUniqueId()) && !PlayerBackup.exists(this.getDataFolder(), audience.getUniqueId())) continue;
            return false;
        }
        ArrayList<Player> arrayList = new ArrayList<>();
        try {
            World exception = Bukkit.getWorld(this.map.worldName);List<UUID> teamIds=list.stream().map(Player::getUniqueId).toList();
            for (Player player : list) {
                PlayerBackup.save(this.getDataFolder(), player);
                arrayList.add(player);
                this.visitors.add(player.getUniqueId());entryTeams.put(player.getUniqueId(),teamIds);
                this.prepareLobby(player, exception);
                KasabaYarisPlugin.tell(player, "§eTekne §fveya §eAt §fyarışını seç. Pusulaya sağ tıkla veya §e/yaris§f yaz.");
            }
            if (this.getConfig().getBoolean("auto-open-menu-on-join", true)) {
                Bukkit.getScheduler().runTaskLater((Plugin)this, () -> {
                    for (Player player : arrayList) {
                        if (!player.isOnline() || !this.visitors.contains(player.getUniqueId())) continue;
                        this.showMain(player);
                    }
                }, 10L);
            }
            return true;
        }
        catch (Exception exception) {
            this.getLogger().severe("Yarış lobisine giriş başarısız: " + exception.getMessage());
            Iterator iterator = arrayList.iterator();
            while (iterator.hasNext()) {
                Player player = (Player)iterator.next();
                this.detach(player, true);
            }
            return false;
        }
    }

    @Override
    public void leave(UUID uUID) {
        Player player = Bukkit.getPlayer(uUID);
        if (player != null) {
            this.detach(player, true);
        } else {
            RaceRoom raceRoom = this.membership.remove(uUID);
            if (raceRoom != null) {
                raceRoom.remove(uUID);
            }
            this.visitors.remove(uUID);
        }
    }

    private void detach(Player player, boolean bl) {
        UUID uUID = player.getUniqueId();
        RaceRoom raceRoom = this.membership.remove(uUID);
        if (raceRoom != null) {
            raceRoom.remove(uUID);
        }
        this.visitors.remove(uUID);entryTeams.remove(uUID);screens.clear(uUID);
        player.closeDialog();player.closeInventory();
        if (!quitting.contains(uUID)&&PlayerBackup.exists(this.getDataFolder(), uUID)) {
            try {
                PlayerBackup.restore(this.getDataFolder(), player, bl);
            }
            catch (Exception exception) {
                this.getLogger().severe("Oyuncu kaydı geri yüklenemedi: " + player.getName() + ": " + exception.getMessage());
                KasabaYarisPlugin.tell(player, "§cEnvanter kurtarma kaydın saklandı. Yöneticiye haber ver.");
            }
        }
    }

    void closeRoom(RaceRoom raceRoom) {
        if (raceRoom.state == RaceRoom.State.CLOSED) {
            return;
        }
        this.rooms.remove(raceRoom.id);
        raceRoom.close();
        World world = this.map == null ? null : Bukkit.getWorld(this.map.worldName);
        for (RaceRoom.Racer racer : raceRoom.racers.values()) {
            Player player;
            boolean bl;
            boolean bl2;
            boolean bl3 = bl2 = this.membership.get(racer.id) == raceRoom;
            if (bl2) {
                this.membership.remove(racer.id);
            }
            boolean bl4 = bl = (player = Bukkit.getPlayer(racer.id)) != null && raceRoom.world != null && player.getWorld() == raceRoom.world;
            if (player == null || !player.isOnline() || !this.visitors.contains(racer.id) || world == null || !bl2 && !bl) continue;
            this.prepareLobby(player, world);
            KasabaYarisPlugin.tell(player, "§fYarış lobisine döndün. Yeni yarış için §e/yaris§f.");
        }
        if (raceRoom.world != null) {
            if(shuttingDown)this.instances.close(raceRoom.world);else Bukkit.getScheduler().runTask(this,()->this.instances.close(raceRoom.world));
        }
    }

    private void prepareLobby(Player player, World world) {
        if(tr.kasaba.hub.api.NetworkFeatures.get()!=null)tr.kasaba.hub.api.NetworkFeatures.get().beforeGame(player);screens.clear(player.getUniqueId());player.closeDialog();player.setItemOnCursor(null);player.setAbsorptionAmount(0);player.setGravity(true);player.setFlying(false);player.setAllowFlight(false);
        player.closeInventory();
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setHealth(player.getMaxHealth());
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        this.teleportInternal(player, this.map.lobby.at(world));player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().setItem(4, this.menuItem());
        player.updateInventory();
    }

    void prepareRacer(Player player, Location location) {
        if(tr.kasaba.hub.api.NetworkFeatures.get()!=null)tr.kasaba.hub.api.NetworkFeatures.get().beforeGame(player);screens.clear(player.getUniqueId());player.closeDialog();player.setItemOnCursor(null);player.setAbsorptionAmount(0);player.setGravity(true);player.setFlying(false);player.setAllowFlight(false);
        player.closeInventory();
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.setFoodLevel(20);
        player.setSaturation(20.0f);
        player.setHealth(player.getMaxHealth());
        for(PotionEffect effect:player.getActivePotionEffects())player.removePotionEffect(effect.getType());
        this.teleportInternal(player, location);player.setGameMode(GameMode.ADVENTURE);
    }

    void teleportInternal(Player player, Location location) {
        this.internalTeleports.add(player.getUniqueId());
        try {
            if (!player.teleport(location)) {
                throw new IllegalStateException("Işınlanma başarısız: " + player.getName());
            }
        }
        finally {
            this.internalTeleports.remove(player.getUniqueId());
        }
    }

    private ItemStack menuItem() {
        ItemStack itemStack = new ItemStack(Material.COMPASS);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName("§b§lKasaba Yarış Menüsü");
        itemMeta.getPersistentDataContainer().set(this.menuItemKey, PersistentDataType.BYTE, (byte)1);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack item(Material material, String string, String ... stringArray) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(string);
        if (stringArray.length > 0) {
            itemMeta.setLore(List.of(stringArray));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void showMain(Player p){
        ready(p);RaceRoom own=membership.get(p.getUniqueId());boolean safe=lobby(p.getUniqueId());List<GameScreens.Button> buttons=new ArrayList<>();
        if(own==null){buttons.add(new GameScreens.Button("At Yarışı",()->createRoom(p,Mode.HORSE)));buttons.add(new GameScreens.Button("Tekne Yarışı",()->createRoom(p,Mode.BOAT)));buttons.add(new GameScreens.Button("Açık odalar",()->showRooms(p)));}
        else{if(own.state==RaceRoom.State.WAITING&&own.owner.equals(p.getUniqueId()))buttons.add(new GameScreens.Button("Maçı başlat",()->startRoom(p)));buttons.add(new GameScreens.Button("Odadan ayrıl",()->leaveRoom(p)));}
        buttons.add(new GameScreens.Button(safe?"Bu lobide kal":"Yarışa dön",()->{if(own!=null&&own.state==RaceRoom.State.WAITING)leaveRoom(p);screens.clear(p.getUniqueId());p.closeDialog();}));buttons.add(new GameScreens.Button("Ana spawna dön",()->hub.returnToLobby(p.getUniqueId())));
        screens.show(p,"Kasaba • At ve Tekne Yarışı",List.of("İki oyun aynı seçim lobisini kullanır. At veya tekne odası seç.","2–5 kişi • İki tur • Altı kontrol noktasını sırayla geç.",own==null?"Bir oda seçmeden lobide bekleyebilirsin.":"Odan: "+own.id+" • "+own.mode.title),buttons,safe);
    }
    private void showRooms(Player p){ready(p);List<GameScreens.Button> buttons=new ArrayList<>();for(RaceRoom room:rooms.values())if(room.state==RaceRoom.State.WAITING&&room.racers.size()<maximum())buttons.add(new GameScreens.Button("Oda "+room.id+" • "+room.mode.title+" • "+room.racers.size()+"/5",()->joinRoom(p,room.id)));buttons.add(new GameScreens.Button("Geri",()->showMain(p)));buttons.add(new GameScreens.Button("Bu lobide kal",()->{}));screens.show(p,"Yarışlar • Açık Odalar",List.of("Başlamamış bir odayı seç. Aynı anda farklı maçlar oynanabilir."),buttons,true);}
    private void createRoom(Player player, Mode mode) {
        if (!this.visitors.contains(player.getUniqueId())) {
            KasabaYarisPlugin.tell(player, "§cÖnce /oyunlar menüsünden Yarış Arenası'na gir.");
            return;
        }
        if (this.membership.containsKey(player.getUniqueId())) {
            KasabaYarisPlugin.tell(player, "§cZaten bir odadasın. /yaris ayril");
            return;
        }
        if (this.rooms.size() >= Math.max(1, this.getConfig().getInt("max-concurrent-rooms", 8))) {
            KasabaYarisPlugin.tell(player, "§cŞu anda bütün oda yerleri dolu.");
            return;
        }
        RaceRoom raceRoom = new RaceRoom(this, this.nextRoom++, mode, player);
        List<Player> team=team(player);if(team.isEmpty())return;
        this.rooms.put(raceRoom.id,raceRoom);this.membership.put(player.getUniqueId(),raceRoom);
        for(Player member:team)if(member!=player){if(!raceRoom.add(member))throw new IllegalStateException("Grup odaya alınamadı.");membership.put(member.getUniqueId(),raceRoom);}
        KasabaYarisPlugin.tell(player, "§a" + mode.title + " odası kuruldu: §e#" + raceRoom.id + "§f. Oyuncular /yaris odalar ile katılır.");
        KasabaYarisPlugin.tell(player, "§fEn az " + this.minimum() + " kişi olduğunda §e/yaris baslat§f yaz.");
        player.closeInventory();
    }

    private void joinRoom(Player player, int n) {
        if (!this.visitors.contains(player.getUniqueId())) {
            KasabaYarisPlugin.tell(player, "§cÖnce /oyunlar menüsünden Yarış Arenası'na gir.");
            return;
        }
        if (this.membership.containsKey(player.getUniqueId())) {
            KasabaYarisPlugin.tell(player, "§cÖnce mevcut odandan ayrıl.");
            return;
        }
        RaceRoom raceRoom = this.rooms.get(n);List<Player> team=team(player);
        if (raceRoom == null || raceRoom.state!=RaceRoom.State.WAITING || raceRoom.racers.size()+team.size()>maximum()) {
            KasabaYarisPlugin.tell(player, "§cBu oda yok, dolu veya yarış başladı.");
            return;
        }
        for(Player member:team){if(!raceRoom.add(member))throw new IllegalStateException("Grup odaya alınamadı.");membership.put(member.getUniqueId(),raceRoom);}
        KasabaYarisPlugin.tell(player, "§a" + raceRoom.summary() + " §fodasına katıldın.");
        player.closeInventory();
    }

    private void startRoom(Player player) {
        RaceRoom raceRoom = this.membership.get(player.getUniqueId());
        if (raceRoom == null) {
            KasabaYarisPlugin.tell(player, "§cBir odada değilsin.");
            return;
        }
        if (!raceRoom.owner.equals(player.getUniqueId())) {
            KasabaYarisPlugin.tell(player, "§cOdayı yalnızca sahibi başlatabilir.");
            return;
        }
        if (raceRoom.state != RaceRoom.State.WAITING) {
            KasabaYarisPlugin.tell(player, "§cYarış zaten başladı.");
            return;
        }
        if (raceRoom.racers.size() < this.minimum()) {
            KasabaYarisPlugin.tell(player, "§cEn az " + this.minimum() + " kişi gerekli.");
            return;
        }
        player.closeInventory();
        try {
            raceRoom.start();
        }
        catch (Exception exception) {
            this.getLogger().severe("Oda " + raceRoom.id + " açılamadı: " + exception.getMessage());
            raceRoom.announce("§cYarış başlatılamadı: " + exception.getMessage());
            this.closeRoom(raceRoom);
        }
    }

    private void leaveRoom(Player player) {
        RaceRoom raceRoom = this.membership.remove(player.getUniqueId());
        if (raceRoom == null) {
            KasabaYarisPlugin.tell(player, "§cBir odada değilsin.");
            return;
        }
        raceRoom.remove(player.getUniqueId());
        World world = Bukkit.getWorld(this.map.worldName);
        if (world != null && this.visitors.contains(player.getUniqueId())) {
            this.prepareLobby(player, world);
        }
        KasabaYarisPlugin.tell(player, "§fOdadan ayrıldın; yarış lobisindesin.");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String string, String[] stringArray) {
        String string2;
        if (!(commandSender instanceof Player)) {
            commandSender.sendMessage("Bu komutu oyuncu kullanır.");
            return true;
        }
        Player player = (Player)commandSender;
        if(!authenticated(player)){tell(player,"Önce hesabına giriş yap.");return true;}if(!visitors.contains(player.getUniqueId())){if(hub.playing(player.getUniqueId())){tell(player,"Önce /lobi ile mevcut oyundan çık.");return true;}Bukkit.dispatchCommand(player,"oyunlar git "+GAME_ID);if(!visitors.contains(player.getUniqueId()))return true;}
        if (stringArray.length == 0 || stringArray[0].equalsIgnoreCase("menu")) {
            this.showMain(player);
            return true;
        }
        switch (string2 = norm(stringArray[0])) {
            case "olustur":
            case "kur": {
                Mode mode;
                Mode mode2 = mode = stringArray.length > 1 ? Mode.parse(stringArray[1]) : null;
                if (mode == null) {
                    KasabaYarisPlugin.tell(player, "§e/yaris olustur tekne|at");
                    break;
                }
                this.createRoom(player, mode);
                break;
            }
            case "odalar": {
                this.showRooms(player);
                break;
            }
            case "katil": {
                if (stringArray.length < 2) {
                    KasabaYarisPlugin.tell(player, "§e/yaris katil <oda numarası>");
                    break;
                }
                try {
                    this.joinRoom(player, Integer.parseInt(stringArray[1]));
                }
                catch (NumberFormatException numberFormatException) {
                    KasabaYarisPlugin.tell(player, "§cOda numarası sayı olmalı.");
                }
                break;
            }
            case "baslat": {
                this.startRoom(player);
                break;
            }
            case "ayril": {
                this.leaveRoom(player);
                break;
            }
            case "cik": {
                if (!this.visitors.contains(player.getUniqueId())) {
                    KasabaYarisPlugin.tell(player, "§cYarış alanında değilsin.");
                    break;
                }
                this.detach(player, true);
                if (this.hub == null || !this.hub.playing(player.getUniqueId())) break;
                this.hub.returnToLobby(player.getUniqueId());
                break;
            }
            default: {
                KasabaYarisPlugin.tell(player, "§e/yaris §7• olustur tekne|at • odalar • katil <id> • baslat • ayril • cik");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String string2, String[] stringArray) {
        if (stringArray.length == 1) {
            return List.of("menu", "olustur", "odalar", "katil", "baslat", "ayril", "cik").stream().filter(string -> string.startsWith(stringArray[0].toLowerCase(Locale.ROOT))).toList();
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("olustur")) {
            return List.of("tekne", "at");
        }
        if (stringArray.length == 2 && stringArray[0].equalsIgnoreCase("katil")) {
            return this.rooms.values().stream().filter(raceRoom -> raceRoom.state == RaceRoom.State.WAITING).map(raceRoom -> Integer.toString(raceRoom.id)).toList();
        }
        return List.of();
    }

    @EventHandler
    public void onClick(InventoryClickEvent inventoryClickEvent) {
        InventoryHolder inventoryHolder = inventoryClickEvent.getWhoClicked();
        if (!(inventoryHolder instanceof Player)) {
            return;
        }
        Player player = (Player)inventoryHolder;
        InventoryHolder inventoryHolder2 = inventoryClickEvent.getView().getTopInventory().getHolder();
        if (!(inventoryHolder2 instanceof Menu)) {
            if (this.visitors.contains(player.getUniqueId())) {
                inventoryClickEvent.setCancelled(true);
            }
            return;
        }
        inventoryHolder = (Menu)inventoryHolder2;
        inventoryClickEvent.setCancelled(true);
        if (inventoryClickEvent.getClickedInventory() != inventoryClickEvent.getView().getTopInventory()) {
            return;
        }
        int n = inventoryClickEvent.getRawSlot();
        if (!this.visitors.contains(player.getUniqueId())) {
            Bukkit.getScheduler().runTask((Plugin)this, () -> player.closeInventory());
            return;
        }
        if (((Menu)inventoryHolder).roomList) {
            if (n == 49) {
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.showMain(player));
            } else if (((Menu)inventoryHolder).slotRooms.containsKey(n)) {
                int n2 = ((Menu)inventoryHolder).slotRooms.get(n);
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.joinRoom(player, n2));
            }
            return;
        }
        switch (n) {
            case 10: {
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.createRoom(player, Mode.BOAT));
                break;
            }
            case 12: {
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.createRoom(player, Mode.HORSE));
                break;
            }
            case 14: {
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.showRooms(player));
                break;
            }
            case 16: {
                Bukkit.getScheduler().runTask((Plugin)this, () -> this.startRoom(player));
                break;
            }
            case 22: {
                Bukkit.getScheduler().runTask((Plugin)this, () -> {
                    if (this.membership.containsKey(player.getUniqueId())) {
                        this.leaveRoom(player);
                    } else {
                        this.detach(player, true);
                        if (this.hub != null && this.hub.playing(player.getUniqueId())) {
                            this.hub.returnToLobby(player.getUniqueId());
                        }
                    }
                });
                break;
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent inventoryDragEvent) {
        Player player;
        HumanEntity humanEntity = inventoryDragEvent.getWhoClicked();
        if (humanEntity instanceof Player && this.visitors.contains((player = (Player)humanEntity).getUniqueId())) {
            inventoryDragEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent playerInteractEvent) {
        if (playerInteractEvent.getHand() != EquipmentSlot.HAND || !this.visitors.contains(playerInteractEvent.getPlayer().getUniqueId())) {
            return;
        }
        if (playerInteractEvent.getAction() != Action.RIGHT_CLICK_AIR && playerInteractEvent.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack itemStack = playerInteractEvent.getItem();
        if (itemStack == null || !itemStack.hasItemMeta() || !itemStack.getItemMeta().getPersistentDataContainer().has(this.menuItemKey, PersistentDataType.BYTE)) {
            return;
        }
        playerInteractEvent.setCancelled(true);
        this.showMain(playerInteractEvent.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent playerMoveEvent) {
        Player player = playerMoveEvent.getPlayer();
        if (!this.visitors.contains(player.getUniqueId())) {
            return;
        }
        RaceRoom raceRoom = this.membership.get(player.getUniqueId());
        if (raceRoom != null && raceRoom.state != RaceRoom.State.WAITING) {
            return;
        }
        if (playerMoveEvent.getTo() != null && playerMoveEvent.getTo().getY() < 55.0 && playerMoveEvent.getTo().getWorld() == Bukkit.getWorld(this.map.worldName)) {
            this.teleportInternal(player, this.map.lobby.at(Bukkit.getWorld(this.map.worldName)));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent entityDamageEvent) {
        Player player;
        Entity entity = entityDamageEvent.getEntity();
        if (entity instanceof Player && (this.visitors.contains((player = (Player)entity).getUniqueId())||protectedWorld(entity.getWorld()))) {
            entityDamageEvent.setCancelled(true);
        } else if (this.isRaceMount(entityDamageEvent.getEntity())) {
            entityDamageEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDamage(VehicleDestroyEvent vehicleDestroyEvent) {
        if (this.isRaceMount(vehicleDestroyEvent.getVehicle())) {
            vehicleDestroyEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent entityDismountEvent) {
        Object object = entityDismountEvent.getEntity();
        if (!(object instanceof Player)) {
            return;
        }
        Player player = (Player)object;if(internalTeleports.contains(player.getUniqueId()))return;
        object = this.membership.get(player.getUniqueId());
        if (object == null || ((RaceRoom)object).state != RaceRoom.State.COUNTDOWN && ((RaceRoom)object).state != RaceRoom.State.RACING) {
            return;
        }
        RaceRoom.Racer racer = ((RaceRoom)object).racers.get(player.getUniqueId());
        if (racer != null && racer.active && !racer.dnf && racer.finishMillis < 0L && racer.mount == entityDismountEvent.getDismounted()) {
            entityDismountEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onMount(EntityMountEvent entityMountEvent) {
        Entity entity = entityMountEvent.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        for (RaceRoom raceRoom : this.rooms.values()) {
            if (raceRoom.state != RaceRoom.State.COUNTDOWN && raceRoom.state != RaceRoom.State.RACING) continue;
            for (RaceRoom.Racer racer : raceRoom.racers.values()) {
                if (racer.mount != entityMountEvent.getMount() || racer.id.equals(player.getUniqueId())) continue;
                entityMountEvent.setCancelled(true);
                return;
            }
        }
    }

    private boolean protectedWorld(World w){return map!=null&&(w.getName().equals(map.worldName)||w.getName().startsWith("yaris_room_"));}

    private boolean isRaceMount(Entity entity) {
        for (RaceRoom raceRoom : this.rooms.values()) {
            for (RaceRoom.Racer racer : raceRoom.racers.values()) {
                if (racer.mount != entity) continue;
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent foodLevelChangeEvent) {
        Player player;
        HumanEntity humanEntity = foodLevelChangeEvent.getEntity();
        if (humanEntity instanceof Player && this.visitors.contains((player = (Player)humanEntity).getUniqueId())) {
            foodLevelChangeEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent blockBreakEvent) {
        if (protectedWorld(blockBreakEvent.getBlock().getWorld())||this.visitors.contains(blockBreakEvent.getPlayer().getUniqueId())) {
            blockBreakEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent blockPlaceEvent) {
        if (protectedWorld(blockPlaceEvent.getBlock().getWorld())||this.visitors.contains(blockPlaceEvent.getPlayer().getUniqueId())) {
            blockPlaceEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent playerDropItemEvent) {
        if (this.visitors.contains(playerDropItemEvent.getPlayer().getUniqueId())) {
            playerDropItemEvent.setCancelled(true);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST) public void onQuit(PlayerQuitEvent e){UUID id=e.getPlayer().getUniqueId();quitting.add(id);leave(id);}
    @EventHandler public void onJoin(PlayerJoinEvent e){quitting.remove(e.getPlayer().getUniqueId());}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(tr.kasaba.auth.api.PlayerAuthenticatedEvent e){recover(e.getPlayer());}
    private void recover(Player p){if(!authenticated(p)||visitors.contains(p.getUniqueId()))return;try{PlayerBackup.restore(getDataFolder(),p,true);}catch(RuntimeException e){getLogger().log(java.util.logging.Level.SEVERE,"Yarış eşya yedeği korundu.",e);p.kick(net.kyori.adventure.text.Component.text("Yarış eşyaların kurtarılamadı; yöneticiye haber ver."));}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void externalTeleport(org.bukkit.event.player.PlayerTeleportEvent e){if(visitors.contains(e.getPlayer().getUniqueId())&&!internalTeleports.contains(e.getPlayer().getUniqueId()))e.setCancelled(true);}
    @EventHandler public void multiverse(org.bukkit.event.server.PluginEnableEvent e){if(e.getPlugin().getName().equals("Multiverse-Core"))Bukkit.getScheduler().runTask(this,()->new MultiverseBridge(this).register(Bukkit.getWorld(map.worldName),map.lobby.at(Bukkit.getWorld(map.worldName))));}
    @Override public org.bukkit.generator.ChunkGenerator getDefaultWorldGenerator(String name,String id){return new EmptyWorldGenerator();}
    @Override public boolean lobby(UUID id){RaceRoom room=membership.get(id);return visitors.contains(id)&&(room==null||room.state==RaceRoom.State.WAITING);}
    private boolean authenticated(Player p){var auth=Bukkit.getServicesManager().load(tr.kasaba.auth.api.AuthState.class);return auth!=null&&auth.authenticated(p.getUniqueId());}
    void ready(Player p){if(!authenticated(p)||!visitors.contains(p.getUniqueId()))throw new IllegalArgumentException("Önce yarış lobisine gir.");}
    private static String norm(String s){return s.toLowerCase(Locale.ROOT).replace('ı','i').replace('ş','s').replace('ç','c').replace('ğ','g').replace('ü','u').replace('ö','o');}
    private List<Player> team(Player leader){List<UUID> ids=entryTeams.getOrDefault(leader.getUniqueId(),List.of(leader.getUniqueId()));List<Player> players=ids.stream().filter(visitors::contains).map(Bukkit::getPlayer).toList();if(players.isEmpty()||players.stream().anyMatch(java.util.Objects::isNull))throw new IllegalArgumentException("Gruptaki biri ayrıldı.");if(!players.getFirst().equals(leader))throw new IllegalArgumentException("Odayı grup lideri seçmeli.");for(Player p:players)if(membership.containsKey(p.getUniqueId()))throw new IllegalArgumentException("Gruptaki herkes önce /yaris ayrıl ile odasından çıksın.");return players;}
    void moveMount(Entity mount,Location target){for(Entity rider:mount.getPassengers())internalTeleports.add(rider.getUniqueId());try{if(!mount.teleport(target,org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN,io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS))throw new IllegalStateException("Binek taşınamadı.");}finally{for(Entity rider:mount.getPassengers())internalTeleports.remove(rider.getUniqueId());}}
    static void tell(Player player, String string) {
        player.sendMessage("§b[Kasaba Yarış] §f" + string);
    }

    private static final class Menu
    implements InventoryHolder {
        final boolean roomList;
        final Map<Integer, Integer> slotRooms = new HashMap<Integer, Integer>();
        Inventory inventory;

        Menu(boolean bl) {
            this.roomList = bl;
        }

        @Override
        public Inventory getInventory() {
            return this.inventory;
        }
    }
}
