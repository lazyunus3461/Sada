package tr.kasaba.auth;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class KasabaAuth extends JavaPlugin implements Listener {
    private static final String WELCOME = "Kasaba’ya Hoş Geldin";
    private static final Component WELCOME_HEADER = Component.empty()
            .append(Component.text("Kasaba’ya ", TextColor.color(0xFFB52E)))
            .append(Component.text("Hoş ", TextColor.color(0x35D9FF)))
            .append(Component.text("Geldin", TextColor.color(0x70F09A)))
            .decorate(TextDecoration.BOLD);
    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<String, Failures> failures = new HashMap<>();
    private final PasswordHasher hasher = new PasswordHasher();
    private AccountStore accounts;
    private ThreadPoolExecutor workers;
    private int timeout, maxAttempts, lockout, minLength, maxLength;
    private static final class Failures { int count; long until; }
    private static final class Session {
        final long deadline;
        final BossBar bar = BossBar.bossBar(WELCOME_HEADER, 1,
                BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        long formVersion;
        boolean busy;
        Session(int timeout) { deadline = System.currentTimeMillis() + timeout * 1000L; }
    }

    @Override public void onEnable() {
        saveDefaultConfig();
        // Bir defalık 1.1.0 -> 1.1.1 geçişi: eski 8 karakter ayarını 6 yap.
        if (!getConfig().contains("config-version", true)) {
            getConfig().set("minimum-password-length", 6);
            getConfig().set("config-version", 2);
            saveConfig();
        }
        timeout = Math.clamp(getConfig().getInt("auth-timeout-seconds", 120), 30, 600);
        maxAttempts = Math.clamp(getConfig().getInt("max-attempts", 5), 1, 20);
        lockout = Math.clamp(getConfig().getInt("lockout-seconds", 300), 30, 3600);
        minLength = Math.clamp(getConfig().getInt("minimum-password-length", 6), 6, 32);
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
            if (s != null) { p.closeDialog(); p.hideBossBar(s.bar); }
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
        p.showTitle(Title.title(WELCOME_HEADER,
                Component.text("Devam etmek için kayıt ol veya giriş yap", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1400), Duration.ofMillis(300))));
        Bukkit.getScheduler().runTaskLater(this, () -> { if (current(p, s)) form(p, s, null); }, 40);
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
            s.bar.name(WELCOME_HEADER.append(Component.text(" • " + remaining + " saniye", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, false)));
            s.bar.progress(Math.clamp((float) remaining / timeout, 0, 1));
            p.sendActionBar(Component.text(s.busy ? "Hesabın doğrulanıyor…" : "Devam etmek için kayıt / giriş formunu doldur", NamedTextColor.YELLOW));
        }
    }

    private void form(Player p, Session s, String error) {
        if (!current(p, s) || s.busy) return;
        boolean register = !registered(p);
        if (!current(p, s)) return;
        long version = ++s.formVersion;
        List<DialogInput> inputs = new ArrayList<>();
        inputs.add(DialogInput.text("password", Component.text("Şifre"))
                .width(280).maxLength(maxLength).initial("").build());
        if (register) inputs.add(DialogInput.text("confirm", Component.text("Şifreyi tekrar yaz"))
                .width(280).maxLength(maxLength).initial("").build());
        List<DialogBody> body = new ArrayList<>();
        body.add(DialogBody.plainMessage(Component.text(register ? "HESAP OLUŞTUR" : "HESABINA GİRİŞ YAP", NamedTextColor.YELLOW), 300));
        body.add(DialogBody.plainMessage(Component.text("Oyuncu: " + p.getName(), NamedTextColor.WHITE), 300));
        if (error != null) body.add(DialogBody.plainMessage(Component.text(error, NamedTextColor.RED), 300));
        body.add(DialogBody.plainMessage(Component.text(register
                ? "Kasaba’ya katılmak için aşağıdaki alanları doldur."
                : "Devam etmek için kayıtlı şifreni yaz.", NamedTextColor.GRAY), 300));
        body.add(DialogBody.plainMessage(Component.text("Şifre: " + minLength + "–" + maxLength
                + " karakter. Yazdığın şifre kendi ekranında görünür.", NamedTextColor.DARK_GRAY), 300));
        ClickCallback.Options options = ClickCallback.Options.builder()
                .uses(1).lifetime(Duration.ofSeconds(timeout + 5L)).build();
        DialogAction send = DialogAction.customClick((response, audience) -> {
            if (!(audience instanceof Player actor) || !actor.getUniqueId().equals(p.getUniqueId())) return;
            String password = response.getText("password");
            String confirmation = response.getText("confirm");
            Bukkit.getScheduler().runTask(this, () -> {
                if (!current(p, s) || s.busy || s.formVersion != version) return;
                if (System.currentTimeMillis() >= s.deadline) {
                    p.kick(Component.text("Giriş süresi doldu. Yeniden bağlanabilirsin.")); return;
                }
                if (password == null || password.length() < minLength || password.length() > maxLength || password.isBlank()) {
                    form(p, s, "Şifren " + minLength + "–" + maxLength + " karakter olmalı."); return;
                }
                if (register && !password.equals(confirmation)) {
                    form(p, s, "Şifreler eşleşmedi. İki alana da aynı şifreyi yaz."); return;
                }
                submit(p, s, password.toCharArray(), register);
            });
        }, options);
        DialogAction leave = DialogAction.customClick((response, audience) -> {
            if (!(audience instanceof Player actor) || !actor.getUniqueId().equals(p.getUniqueId())) return;
            Bukkit.getScheduler().runTask(this, () -> {
                if (current(p, s) && s.formVersion == version)
                    p.kick(Component.text("Kasaba’da tekrar görüşmek üzere!"));
            });
        }, options);
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(WELCOME_HEADER)
                        .canCloseWithEscape(false).pause(false)
                        // Form açık kalır; doğrulama başarılı olunca sunucu doğrudan kapatır.
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(body).inputs(inputs).build())
                .type(DialogType.multiAction(List.of(
                        ActionButton.create(Component.text(register ? "Kayıt Ol" : "Giriş Yap", NamedTextColor.GREEN), null, 140, send),
                        ActionButton.create(Component.text("Sunucudan Ayrıl", NamedTextColor.RED), null, 140, leave)
                )).columns(2).build()));
        p.showDialog(dialog);
    }

    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && locked(p)) e.setCancelled(true);
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
                            form(p, s, "Şifre yanlış. Kalan deneme: " + (maxAttempts - f.count));
                        }
                    }
                });
            });
        } catch (RejectedExecutionException ex) {
            Arrays.fill(password, '\0'); s.busy = false;
            form(p, s, "Giriş sistemi yoğun. Biraz sonra tekrar dene.");
        }
    }

    private void complete(Player p, Session s, boolean register) {
        authenticated.add(p.getUniqueId()); sessions.remove(p.getUniqueId()); failures.remove(key(p));
        p.closeDialog(); p.hideBossBar(s.bar);
        p.showTitle(Title.title(WELCOME_HEADER,
                Component.text(register ? "Kaydın tamamlandı!" : "Giriş başarılı!", NamedTextColor.GREEN)));
        tell(p, "Artık Kasaba’yı keşfedebilirsin!");
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Bu komut oyun içinden kullanılır."); return true; }
        if (args.length > 0) { tell(p, "Şifreyi komuta yazma. Sadece /kasaba kullan."); return true; }
        Session s = sessions.get(p.getUniqueId());
        if (s != null) form(p, s, null); else tell(p, "Zaten giriş yaptın.");
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST) public void join(PlayerJoinEvent e) { begin(e.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent e) {
        authenticated.remove(e.getPlayer().getUniqueId());
        Session s = sessions.remove(e.getPlayer().getUniqueId());
        if (s != null) { e.getPlayer().hideBossBar(s.bar); }
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
            e.setCancelled(true); tell(e.getPlayer(), "Önce giriş yap. Formu açmak için /kasaba yaz.");
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
}
