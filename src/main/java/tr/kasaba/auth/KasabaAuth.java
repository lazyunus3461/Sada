package tr.kasaba.auth;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class KasabaAuth extends JavaPlugin implements Listener {
    private static final String WELCOME = "Kasaba’ya Hoş Geldin";
    private static final String PLACEHOLDER = "Şifreni yaz";
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<String, Failures> failures = new HashMap<>();
    private final PasswordHasher hasher = new PasswordHasher();
    private AccountStore accounts;
    private ThreadPoolExecutor workers;
    private int timeout, maxAttempts, lockout, minLength, maxLength;
    private enum Step { MENU, REGISTER, CONFIRM, LOGIN }
    private static final class Failures { int count; long until; }
    private static final class Session {
        final long deadline;
        final BossBar bar = BossBar.bossBar(Component.text(WELCOME, NamedTextColor.GOLD), 1,
                BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        Inventory inventory;
        Step step = Step.MENU;
        char[] first;
        boolean busy;
        Session(int timeout) { deadline = System.currentTimeMillis() + timeout * 1000L; }
        void clear() { if (first != null) { Arrays.fill(first, '\0'); first = null; } }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        timeout = Math.clamp(getConfig().getInt("auth-timeout-seconds", 120), 30, 600);
        maxAttempts = Math.clamp(getConfig().getInt("max-attempts", 5), 1, 20);
        lockout = Math.clamp(getConfig().getInt("lockout-seconds", 300), 30, 3600);
        minLength = Math.clamp(getConfig().getInt("minimum-password-length", 8), 8, 32);
        maxLength = Math.clamp(getConfig().getInt("maximum-password-length", 64), minLength, 64);
        try { accounts = new AccountStore(getDataFolder().toPath().resolve("accounts")); }
        catch (IOException e) { throw new IllegalStateException("Hesap dizini açılamadı; eklenti başlatılamıyor", e); }
        workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(32));
        getServer().getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::tick, 20, 20);
        for (Player p : Bukkit.getOnlinePlayers()) begin(p);
    }

    @Override public void onDisable() {
        // Kimlik doğrulama eklentisini devre dışı bırakıp oyuncuları açıkta bırakma.
        for (Player p : Bukkit.getOnlinePlayers()) {
            Session s = sessions.get(p.getUniqueId());
            if (s != null) { s.clear(); p.hideBossBar(s.bar); }
            p.kick(Component.text("Kayıt sistemi durduruldu. Lütfen yeniden bağlan.", NamedTextColor.RED));
        }
        if (workers != null) workers.shutdown();
        sessions.clear(); authenticated.clear();
    }

    private boolean locked(Player p) { return !authenticated.contains(p.getUniqueId()); }
    private String key(Player p) { return p.getName().toLowerCase(Locale.ROOT); }
    private boolean current(Player p, Session s) { return p.isOnline() && sessions.get(p.getUniqueId()) == s; }
    private void tell(Player p, String text) { p.sendMessage(Component.text(text, NamedTextColor.YELLOW)); }
    private boolean registered(Player p) {
        try { return accounts.load(p.getName()) != null; }
        catch (IOException e) { storageError(p); return true; }
    }
    private void storageError(Player p) {
        getLogger().severe("Hesap dosyası okunamadı/yazılamadı: " + p.getUniqueId());
        p.kick(Component.text("Hesap verisine erişilemiyor. Yöneticiye haber ver.", NamedTextColor.RED));
    }

    private void begin(Player p) {
        if (!p.getName().matches("[a-zA-Z0-9_]{1,16}")) {
            p.kick(Component.text("Bu oyuncu adı kayıt sistemi tarafından desteklenmiyor.")); return;
        }
        authenticated.remove(p.getUniqueId());
        Failures f = failures.get(key(p));
        if (f != null && f.until > System.currentTimeMillis()) {
            p.kick(Component.text("Çok fazla hatalı giriş. Birkaç dakika sonra tekrar dene.", NamedTextColor.RED));
            return;
        }
        Session s = new Session(timeout);
        sessions.put(p.getUniqueId(), s);
        p.showBossBar(s.bar);
        p.showTitle(Title.title(Component.text(WELCOME, NamedTextColor.GOLD),
                Component.text("Devam etmek için kayıt ol veya giriş yap", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(4), Duration.ofMillis(300))));
        Bukkit.getScheduler().runTaskLater(this, () -> { if (current(p, s)) menu(p, s); }, 2);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        failures.entrySet().removeIf(e -> e.getValue().until < now);
        for (Player p : Bukkit.getOnlinePlayers()) {
            Session s = sessions.get(p.getUniqueId());
            if (s == null) continue;
            long remaining = (s.deadline - now + 999) / 1000;
            if (remaining <= 0) {
                p.kick(Component.text("Giriş süresi doldu. Yeniden bağlanabilirsin.", NamedTextColor.RED));
                continue;
            }
            s.bar.name(Component.text(WELCOME + " • " + remaining + " saniye", NamedTextColor.GOLD));
            s.bar.progress(Math.clamp((float) remaining / timeout, 0, 1));
            p.sendActionBar(Component.text(s.busy ? "Hesabın doğrulanıyor…" : "Devam etmek için menüyü kullan • /kasaba", NamedTextColor.YELLOW));
        }
    }

    private ItemStack item(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD));
        meta.lore(Arrays.stream(lore).map(t -> Component.text(t, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private void menu(Player p, Session s) {
        if (s.busy) return;
        s.clear(); s.step = Step.MENU;
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(WELCOME));
        for (int i = 0; i < 27; i++) inv.setItem(i, item(Material.GRAY_STAINED_GLASS_PANE, " "));
        boolean exists = registered(p);
        inv.setItem(11, item(exists ? Material.LIME_DYE : Material.GRAY_DYE, "Giriş Yap", exists ? "Şifreni yazmak için tıkla" : "Önce kayıt olmalısın"));
        inv.setItem(15, item(exists ? Material.GRAY_DYE : Material.WRITABLE_BOOK, "Kayıt Ol", exists ? "Bu isim zaten kayıtlı" : "Yeni hesabını oluştur"));
        inv.setItem(22, item(Material.BARRIER, "Sunucudan Ayrıl"));
        s.inventory = inv;
        p.openInventory(inv);
    }

    private void input(Player p, Session s, Step step) {
        if (s.busy) return;
        s.step = step;
        // Paper'ın gerçek örs görünümü kullanılır; kaynak paketi ve istemci modu gerekmez.
        InventoryView opened = p.openAnvil(null, true);
        if (!(opened instanceof AnvilView view)) {
            p.kick(Component.text("Şifre ekranı açılamadı. Yöneticiye haber ver.")); return;
        }
        s.inventory = view.getTopInventory();
        view.setTitle(step == Step.LOGIN ? "Kasaba • Giriş Yap" : step == Step.CONFIRM ? "Kasaba • Şifreni Tekrarla" : "Kasaba • Kayıt Ol");
        s.inventory.setItem(0, item(Material.PAPER, PLACEHOLDER));
        view.setRepairCost(0);
        view.setRepairItemCountCost(0);
        view.setMaximumRepairCost(40);
        tell(p, "Üstteki yazı alanını temizle, şifreni yaz ve sağdaki Onayla kâğıdına tıkla.");
        tell(p, "Şifren kendi ekranında görünür. Sohbete yazma.");
    }

    @EventHandler public void prepare(PrepareAnvilEvent e) {
        if (!(e.getView().getPlayer() instanceof Player p)) return;
        Session s = sessions.get(p.getUniqueId());
        if (s == null || !s.inventory.equals(e.getInventory()) || s.step == Step.MENU) return;
        e.getView().setRepairCost(0);
        e.getView().setRepairItemCountCost(0);
        e.setResult(item(Material.PAPER, "Onayla", "Şifreni göndermek için tıkla"));
    }

    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !locked(p)) return;
        e.setCancelled(true);
        Session s = sessions.get(p.getUniqueId());
        if (s == null || s.busy || e.getView().getTopInventory() != s.inventory) return;
        if (s.step == Step.MENU) {
            int slot = e.getRawSlot();
            Bukkit.getScheduler().runTask(this, () -> {
                if (!current(p, s) || s.busy) return;
                if (slot == 22) p.kick(Component.text("Kasaba’da tekrar görüşmek üzere!"));
                else if (slot == 11 && registered(p)) input(p, s, Step.LOGIN);
                else if (slot == 15 && !registered(p)) input(p, s, Step.REGISTER);
            });
        } else if (e.getRawSlot() == 2 && e.getView() instanceof AnvilView view) {
            String text = view.getRenameText();
            if (text == null || text.equals(PLACEHOLDER) || text.length() < minLength || text.length() > maxLength || text.isBlank()) {
                tell(p, "Şifren " + minLength + "–" + maxLength + " karakter olmalı. Yazı alanını değiştir."); return;
            }
            char[] password = text.toCharArray();
            Step step = s.step;
            if (step == Step.REGISTER) {
                s.clear(); s.first = password;
                Bukkit.getScheduler().runTask(this, () -> { if (current(p, s)) input(p, s, Step.CONFIRM); });
            } else if (step == Step.CONFIRM) {
                boolean equal = Arrays.equals(s.first, password);
                s.clear();
                if (!equal) {
                    Arrays.fill(password, '\0');
                    tell(p, "Şifreler eşleşmedi. Baştan deneyelim.");
                    Bukkit.getScheduler().runTask(this, () -> { if (current(p, s)) input(p, s, Step.REGISTER); });
                } else submit(p, s, password, true);
            } else submit(p, s, password, false);
        }
    }

    private void submit(Player p, Session s, char[] password, boolean register) {
        s.busy = true;
        String name = p.getName();
        try {
            workers.execute(() -> {
                boolean ok = false, error = false;
                try {
                    if (register) { accounts.create(name, hasher.hash(password)); ok = true; }
                    else { String hash = accounts.load(name); ok = hash != null && hasher.verify(password, hash); }
                } catch (Exception ex) { error = true; }
                finally { Arrays.fill(password, '\0'); }
                boolean accepted = ok, failed = error;
                if (!isEnabled()) return;
                Bukkit.getScheduler().runTask(this, () -> {
                    if (!current(p, s)) return;
                    s.busy = false;
                    if (failed) { storageError(p); return; }
                    if (accepted) complete(p, s, register);
                    else {
                        Failures f = failures.computeIfAbsent(key(p), k -> new Failures());
                        f.until = System.currentTimeMillis() + lockout * 1000L;
                        if (++f.count >= maxAttempts) {
                            p.kick(Component.text("Çok fazla hatalı şifre. " + lockout + " saniye sonra tekrar dene.", NamedTextColor.RED));
                        } else {
                            tell(p, "Şifre yanlış. Kalan deneme: " + (maxAttempts - f.count));
                            input(p, s, Step.LOGIN);
                        }
                    }
                });
            });
        } catch (RejectedExecutionException ex) {
            Arrays.fill(password, '\0'); s.busy = false;
            tell(p, "Giriş sistemi yoğun. Biraz sonra tekrar Onayla’ya tıkla.");
        }
    }

    private void complete(Player p, Session s, boolean register) {
        if (s.inventory != null && s.step != Step.MENU) s.inventory.clear();
        authenticated.add(p.getUniqueId()); sessions.remove(p.getUniqueId()); failures.remove(key(p));
        s.clear(); p.hideBossBar(s.bar); p.closeInventory();
        p.showTitle(Title.title(Component.text(WELCOME, NamedTextColor.GOLD),
                Component.text(register ? "Kaydın tamamlandı!" : "Giriş başarılı!", NamedTextColor.GREEN)));
        tell(p, "Artık Kasaba’yı keşfedebilirsin!");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Bu komut oyun içinden kullanılır."); return true; }
        if (args.length > 0) { tell(p, "Şifreyi komuta yazma. Sadece /kasaba kullan."); return true; }
        Session s = sessions.get(p.getUniqueId());
        if (s != null) menu(p, s); else tell(p, "Zaten giriş yaptın.");
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST) public void join(PlayerJoinEvent e) { begin(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        authenticated.remove(e.getPlayer().getUniqueId());
        Session s = sessions.remove(e.getPlayer().getUniqueId());
        if (s != null) { s.clear(); e.getPlayer().hideBossBar(s.bar); }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        if (locked(e.getPlayer()) && e.hasChangedPosition()) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) public void teleport(PlayerTeleportEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void chat(AsyncChatEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void command(PlayerCommandPreprocessEvent e) {
        if (!locked(e.getPlayer())) return;
        String text = e.getMessage().toLowerCase(Locale.ROOT);
        if (!Set.of("/kasaba", "/giris", "/kayit", "/login", "/register", "/kasabaauth:kasaba", "/kasabaauth:giris", "/kasabaauth:kayit", "/kasabaauth:login", "/kasabaauth:register").contains(text)) {
            e.setCancelled(true); tell(e.getPlayer(), "Önce giriş yap. Menüyü açmak için /kasaba yaz.");
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void interact(PlayerInteractEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void blockBreak(BlockBreakEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void blockPlace(BlockPlaceEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void interactAtEntity(PlayerInteractAtEntityEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void interactEntity(PlayerInteractEntityEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void damage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && locked(p)) e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent hit && hit.getDamager() instanceof Player p && locked(p)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && locked(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void drag(InventoryDragEvent e) { if (e.getWhoClicked() instanceof Player p && locked(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void food(FoodLevelChangeEvent e) { if (e.getEntity() instanceof Player p && locked(p)) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void book(PlayerEditBookEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler(priority = EventPriority.HIGHEST) public void consume(PlayerItemConsumeEvent e) { if (locked(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void close(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        Session s = sessions.get(p.getUniqueId());
        if (s == null || !s.inventory.equals(e.getInventory())) return;
        Inventory closed = s.inventory;
        // Örs kâğıdı kapanışta oyuncunun envanterine dönmesin.
        if (s.step != Step.MENU) closed.clear();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (current(p, s) && s.inventory == closed && !s.busy) menu(p, s);
        }, 2);
    }
}
