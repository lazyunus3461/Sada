package tr.kasabatr.vampir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tr.kasabatr.vampir.KasabaVampirPlugin;
import tr.kasabatr.vampir.MapLayout;

final class GameRoom {
    final KasabaVampirPlugin plugin;
    final int id;
    UUID owner;
    final LinkedHashSet<UUID> members = new LinkedHashSet<>();
    final Set<UUID> alive = new HashSet<UUID>();
    final Map<UUID, Role> roles = new HashMap<UUID, Role>();
    final Map<UUID, List<Task>> tasks = new HashMap<UUID, List<Task>>();
    final Map<UUID, UUID> votes = new HashMap<UUID, UUID>();
    final Map<UUID, UUID> nightTargets = new HashMap<UUID, UUID>();
    final Map<UUID, Location> returnPositions = new HashMap<UUID, Location>();
    final Map<UUID, Corpse> corpses = new HashMap<UUID, Corpse>();
    final Map<UUID, Integer> dayKills = new HashMap<UUID, Integer>();
    final Map<UUID, Long> lastDayKill = new HashMap<UUID, Long>();
    final Map<UUID, Integer> hunterShots = new HashMap<UUID, Integer>();
    final Map<UUID, Long> publicTaskCooldown = new HashMap<UUID, Long>();
    final Set<UUID> emergencyUsed = new HashSet<UUID>();
    private final Random random = new Random();
    private final BossBar bar;
    Phase phase = Phase.WAITING;
    World world;
    RoomGuide guide;
    String meetingReason = "Henüz toplantı yapılmadı.";
    int vampireCount = 1;
    boolean demo;
    boolean hunter = true;
    boolean seer = true;
    boolean doctor = true;
    int seconds;
    int dayRemaining;
    int speakerIndex;
    int taskDone;
    int taskGoal;
    int startingPlayers;
    boolean firstNightDone;
    boolean nightMeeting;
    private List<UUID> speakingOrder = List.of();
    private UUID protectedPlayer;
    private UUID seerTarget;
    private long lastEmergency;

    GameRoom(KasabaVampirPlugin kasabaVampirPlugin, int n, UUID uUID) {
        this.plugin = kasabaVampirPlugin;
        this.id = n;
        this.owner = uUID;
        this.members.add(uUID);
        this.bar = Bukkit.createBossBar("§5Vampir Köylü §7• §fOda " + n, BarColor.PURPLE, BarStyle.SOLID, new BarFlag[0]);
    }

    String ownerName() {
        Player player = Bukkit.getPlayer(this.owner);
        return player == null ? "?" : player.getName();
    }

    boolean canStart() {
        int n = this.demo ? 2 : Math.clamp(this.plugin.getConfig().getInt("game.minimum-players", 6),4,20);
        int n2 = Math.min(20, Math.max(n, this.plugin.getConfig().getInt("game.maximum-players", 20)));
        return this.phase == Phase.WAITING && this.members.size() >= n && this.members.size() <= n2 && this.members.size() >= this.vampireCount + (this.hunter ? 1 : 0) + (this.seer ? 1 : 0) + (this.doctor ? 1 : 0) + 1;
    }

    boolean add(Player player) {
        if (this.phase != Phase.WAITING || this.members.size() >= Math.clamp(this.plugin.getConfig().getInt("game.maximum-players", 20),6,20)) {
            return false;
        }
        this.members.add(player.getUniqueId());
        this.announce(player.getName() + " odaya katıldı. (" + this.members.size() + "/20)");
        return true;
    }

    void leave(UUID uUID) {
        boolean wasSpeaker = isSpeaker(uUID);
        if (!this.members.remove(uUID)) {
            return;
        }
        votes.remove(uUID);nightTargets.remove(uUID);nightTargets.values().removeIf(uUID::equals);
        Player player = Bukkit.getPlayer(uUID);
        if (player != null) {
            this.bar.removePlayer(player);
        }
        if (guide != null) guide.forget(uUID);
        if (this.phase != Phase.WAITING && this.phase != Phase.FINISHED) {
            boolean bl = this.alive.remove(uUID);
            if (bl) {
                this.announce(this.name(uUID) + " oyundan ayrıldı.");
            }
            this.checkWinner();
            if (wasSpeaker && phase == Phase.SPEECH) nextSpeaker();
        } else if (this.phase == Phase.WAITING) {
            if (uUID.equals(this.owner) && !this.members.isEmpty()) {
                this.owner = this.members.iterator().next();
            }
            this.announce(this.name(uUID) + " odadan ayrıldı.");
        }
        if (this.members.isEmpty()) {
            this.finish(null, "Oda boşaldı.");
        }
    }

    void start() throws IOException {
        if (!this.canStart()) {
            throw new IOException("En az oyuncu veya rol dağılımı uygun değil.");
        }
        for (UUID uUID : this.members) {
            if (Bukkit.getPlayer(uUID) != null) continue;
            throw new IOException("Çevrimdışı oyuncu var.");
        }
        this.world = this.plugin.instances().create(this.id);
        try {
            this.startingPlayers = this.members.size();
            ArrayList<UUID> arrayList = new ArrayList<UUID>(this.members);
            Collections.shuffle(arrayList, this.random);
            int n = 0;
            for (int i = 0; i < this.vampireCount; ++i) {
                this.roles.put(arrayList.get(n++), Role.VAMPIRE);
            }
            if (this.hunter) {
                this.roles.put(arrayList.get(n++), Role.HUNTER);
            }
            if (this.seer) {
                this.roles.put(arrayList.get(n++), Role.SEER);
            }
            if (this.doctor) {
                this.roles.put(arrayList.get(n++), Role.DOCTOR);
            }
            Iterator<UUID> iterator = arrayList.iterator();
            while (iterator.hasNext()) {
                UUID object = iterator.next();
                this.roles.putIfAbsent(object, Role.VILLAGER);
            }
            this.alive.addAll(this.members);
            this.taskGoal = (this.startingPlayers - this.vampireCount) * 3;
            for (UUID uUID : this.members) {
                ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
                for (int i = 0; i < MapLayout.STATIONS.size(); ++i) {
                    arrayList2.add(i);
                }
                Collections.shuffle(arrayList2, this.random);
                this.tasks.put(uUID, arrayList2.subList(0, 4).stream().map(Task::new).toList());
            }
            this.guide = new RoomGuide(this);
            int n2 = 0;
            for (UUID uUID : this.members) {
                Player player = Bukkit.getPlayer(uUID);
                this.plugin.prepareMatchPlayer(player, this, MapLayout.seat(this.world, n2++));
                this.bar.addPlayer(player);
                this.guide.attach(player);
                this.plugin.tell(player, "Rolün: §d" + this.roles.get((Object)uUID).label + "§f. Başkalarına söylemek zorunda değilsin.");
                this.plugin.tell(player, GameText.purpose(this.roles.get(uUID)));
                this.plugin.tell(player, "7. kutudaki harita görevlerini gösterir. Haritaya/deftere sağ tıklayıp hedef seç. /vk rehber ile kuralları oku.");
                if (this.roles.get(uUID) != Role.VAMPIRE) continue;
                this.plugin.tell(player, "Gündüz yakın dövüşle avlan; gece menüden hedef seç. /v gizli sohbet.");
            }
            this.phase = Phase.DAY;
            this.dayRemaining = Math.max(20, this.plugin.getConfig().getInt("game.day-seconds", 180));
            this.world.setTime(1000L);
            this.announce("Gündüz başladı: " + this.dayRemaining + " saniye. Görevleri yapın, ipuçlarını izleyin.");
            this.refreshBar();
            this.guide.tick();
        }
        catch (RuntimeException runtimeException) {
            this.finish(null, "Maç başlatılamadı.");
            throw new IOException("Oyuncular haritaya yerleştirilemedi", runtimeException);
        }
    }

    void tick() {
        if (this.phase == Phase.WAITING || this.phase == Phase.FINISHED) {
            return;
        }
        if (this.phase == Phase.DAY) {
            if (--this.dayRemaining <= 0) {
                this.beginMeeting(true, null);
            }
        } else if (--this.seconds <= 0) {
            switch (this.phase.ordinal()) {
                case 2: {
                    this.nextSpeaker();
                    break;
                }
                case 3: {
                    this.closeVote();
                    break;
                }
                case 4: {
                    this.finishNight();
                    break;
                }
            }
        }
        this.refreshBar();
        if (this.guide != null && phase != Phase.FINISHED) this.guide.tick();
    }

    void beginMeeting(boolean bl, Corpse corpse) {
        beginMeeting(bl, corpse, null);
    }

    void beginMeeting(boolean bl, Corpse corpse, UUID firstSpeaker) {
        if (this.phase != Phase.DAY) {
            return;
        }
        this.nightMeeting = bl;
        this.meetingReason = bl ? "Gündüz bitti; gece öncesi şüphelileri konuşmak için toplandınız."
            : corpse != null ? this.name(corpse.victim) + " oyuncusunun cesedi " + corpse.zone + " bölgesinde bulundu."
            : "Acil toplantı çanı çalındı; şüpheli davranışları değerlendireceksiniz.";
        this.phase = Phase.SPEECH;
        this.speakingOrder = MeetingOrder.create(this.members, this.alive, firstSpeaker);
        this.speakerIndex = -1;
        this.returnPositions.clear();
        int n = 0;
        for (UUID uUID : this.speakingOrder) {
            Player player = Bukkit.getPlayer(uUID);
            if (player == null) continue;
            this.returnPositions.put(uUID, player.getLocation().clone());
            this.plugin.closePage(player);
            this.plugin.teleportInternal(player,MapLayout.seat(this.world, n++));
        }
        if (bl) {
            this.world.setTime(18000L);
            this.announce("§9Gece çöktü. Herkes meydanda toplandı; sırayla konuşulacak.");
        } else if (corpse != null) {
            this.announce("§eCeset bulundu: " + this.name(corpse.victim) + " • " + corpse.zone + " • yaklaşık " + Math.max(0L, (System.currentTimeMillis() - corpse.time) / 1000L) + " sn önce.");
        } else {
            this.announce("§eMeydan çanı çaldı. Herkes toplantıya çağrıldı.");
        }
        this.announce("Amaç: aranızdaki vampiri bulmak. Sıran gelince kimi, nerede gördüğünü anlat; ardından şüpheli kişiye oy vereceksin.");
        this.nextSpeaker();
    }

    void nextSpeaker() {
        if (this.phase != Phase.SPEECH) {
            return;
        }
        do {
            ++this.speakerIndex;
        } while (this.speakerIndex < this.speakingOrder.size() && !this.alive.contains(this.speakingOrder.get(this.speakerIndex)));
        if (this.speakerIndex >= this.speakingOrder.size()) {
            this.beginVote();
            return;
        }
        this.seconds = Math.max(5, this.plugin.getConfig().getInt("game.speaking-seconds-per-player", 20));
        this.announce("Söz sırası: §a" + this.name(this.speakingOrder.get(this.speakerIndex)) + "§f (" + this.seconds + " sn). /vk bitir ile geçebilir.");
        this.refreshBar();
    }

    boolean isSpeaker(UUID uUID) {
        return this.phase == Phase.SPEECH && this.speakerIndex >= 0 && this.speakerIndex < this.speakingOrder.size() && this.speakingOrder.get(this.speakerIndex).equals(uUID);
    }

    private void beginVote() {
        this.phase = Phase.VOTE;
        this.votes.clear();
        this.seconds = Math.max(10, this.plugin.getConfig().getInt("game.vote-seconds", 30));
        this.announce("§eOylama başladı. Menünden bir oyuncu seç veya pas geç. Eşitlikte kimse elenmez.");
        this.announce("Vampir olduğundan şüphelendiğin kişiyi seç. En çok oyu tek başına alan elenir; yanlış bir köylüyü elemek takımını zayıflatır. Emin değilsen Pas.");
        for (UUID uUID : this.alive) {
            Player player = Bukkit.getPlayer(uUID);
            if (player == null) continue;
            this.plugin.openTargetMenu(player, this, "vote");
        }
        this.refreshBar();
    }

    void vote(UUID uUID, UUID uUID2) {
        if (this.phase != Phase.VOTE || !this.alive.contains(uUID) || uUID2 != null && !this.alive.contains(uUID2)) {
            return;
        }
        this.votes.put(uUID, uUID2);
        Player player = Bukkit.getPlayer(uUID);
        if (player != null) {
            this.plugin.tell(player, (String)(uUID2 == null ? "Pas oyu verildi." : this.name(uUID2) + " için oy verildi."));
        }
        if (this.votes.size() >= this.alive.size()) {
            this.closeVote();
        }
    }

    private void closeVote() {
        if (this.phase != Phase.VOTE) {
            return;
        }
        for(UUID id:members){Player player=Bukkit.getPlayer(id);if(player!=null)plugin.closePage(player);}
        HashMap<UUID, Integer> hashMap = new HashMap<UUID, Integer>();
        for (UUID object2 : this.votes.values()) {
            hashMap.merge(object2, 1, Integer::sum);
        }
        int n = hashMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<UUID> list = hashMap.entrySet().stream().filter(entry -> entry.getValue() == n).map(Map.Entry::getKey).toList();
        if (n > 0 && list.size() == 1 && list.get(0) != null) {
            UUID uUID = list.get(0);
            this.announce("Karar: " + this.name(uUID) + " — " + n + " oy. Açıklanan rol: §d" + this.roles.get(uUID).label);
            this.eliminate(uUID, null, false);
            if (this.phase != Phase.FINISHED) {
                this.announce("Oylama sonucu " + this.name(uUID) + " elendi. Rol: " + this.roles.get((Object)uUID).label);
            }
        } else {
            this.announce("Oylama eşit/pas: kimse elenmedi.");
        }
        if (this.phase == Phase.FINISHED) {
            return;
        }
        if (this.nightMeeting) {
            this.beginNightRoles();
        } else {
            this.resumeDay();
        }
    }

    private void beginNightRoles() {
        this.phase = Phase.ROLE;
        this.seconds = Math.max(5, this.plugin.getConfig().getInt("game.night-choice-seconds", 40));
        this.nightTargets.clear();
        this.protectedPlayer = null;
        this.seerTarget = null;
        this.announce("§9Gece rolleri için " + this.seconds + " saniye. Kahin, doktor ve vampirler listeden seçsin.");
        for (UUID uUID : this.alive) {
            Player player = Bukkit.getPlayer(uUID);
            if (player == null || this.roles.get(uUID) != Role.VAMPIRE && this.roles.get(uUID) != Role.SEER && this.roles.get(uUID) != Role.DOCTOR) continue;
            Bukkit.getScheduler().runTaskLater(plugin,()->{if(phase==Phase.ROLE&&alive.contains(player.getUniqueId())&&player.isOnline()&&!plugin.hasPage(player))plugin.openTargetMenu(player,this,"role");},2);
        }
    }

    void chooseNight(UUID uUID, UUID uUID2) {
        if (this.phase != Phase.ROLE || !this.alive.contains(uUID) || !this.alive.contains(uUID2)) {
            return;
        }
        Role role = this.roles.get(uUID);
        if (uUID.equals(uUID2) && role != Role.DOCTOR) {
            return;
        }
        Player player = Bukkit.getPlayer(uUID);
        if (role == Role.SEER) {
            if (this.seerTarget != null) {
                this.plugin.tell(player, "Bu gece zaten baktın.");
                return;
            }
            this.seerTarget = uUID2;
            this.plugin.tell(player, this.name(uUID2) + " rolü: §d" + this.roles.get((Object)uUID2).label);
        } else if (role == Role.DOCTOR) {
            if (this.protectedPlayer != null) {
                this.plugin.tell(player, "Bu gece zaten koruma seçtin.");
                return;
            }
            this.protectedPlayer = uUID2;
            this.plugin.tell(player, this.name(uUID2) + " koruma altında.");
        } else if (role == Role.VAMPIRE) {
            int n;
            if (this.nightTargets.containsKey(uUID)) {
                this.plugin.tell(player, "Bu geceki hedefin seçildi.");
                return;
            }
            if (this.roles.get(uUID2) == Role.VAMPIRE) {
                this.plugin.tell(player, "Başka vampiri hedefleyemezsin.");
                return;
            }
            int n2 = n = this.startingPlayers >= this.plugin.getConfig().getInt("game.high-player-threshold", 15) ? this.vampireCount * Math.max(1, this.plugin.getConfig().getInt("game.high-player-night-kills-per-vampire", 1)) : Math.max(1, this.plugin.getConfig().getInt("game.low-player-night-team-kills", 1));
            if (this.nightTargets.size() >= n) {
                this.plugin.tell(player, "Takımın gece hakkı doldu.");
                return;
            }
            if (this.nightTargets.containsValue(uUID2)) {
                this.plugin.tell(player, "Bu kişi zaten hedeflenmiş.");
                return;
            }
            this.nightTargets.put(uUID, uUID2);
            this.plugin.tell(player, this.name(uUID2) + " gece hedefi seçildi.");
        }
    }

    private void finishNight() {
        UUID uUID;
        if (this.phase != Phase.ROLE) {
            return;
        }
        if (this.seerTarget == null) {
            for (UUID object2 : this.alive) {
                if (this.roles.get(object2) != Role.SEER || (uUID = this.randomOther(object2)) == null) continue;
                this.seerTarget = uUID;
                this.plugin.tell(Bukkit.getPlayer(object2), "Rastgele bakılan " + this.name(uUID) + ": §d" + this.roles.get((Object)uUID).label);
            }
        }
        if (this.protectedPlayer == null) {
            for (UUID uUID2 : this.alive) {
                if (this.roles.get(uUID2) != Role.DOCTOR) continue;
                this.protectedPlayer = this.randomAlive();
                if (this.protectedPlayer == null) continue;
                this.plugin.tell(Bukkit.getPlayer(uUID2), "Rastgele korunan kişi: " + this.name(this.protectedPlayer));
            }
        }
        ArrayList<UUID> arrayList = new ArrayList<UUID>(this.nightTargets.values());
        Iterator<UUID> iterator = arrayList.iterator();
        while (iterator.hasNext()) {
            uUID = iterator.next();
            if (this.phase == Phase.FINISHED) break;
            if (uUID.equals(this.protectedPlayer)) {
                this.announce("§aDoktor bir gece saldırısını engelledi.");
                continue;
            }
            if (!this.alive.contains(uUID)) continue;
            this.eliminate(uUID, null, false);
            this.announce(this.name(uUID) + " gece öldürüldü.");
        }
        this.firstNightDone = true;
        if (this.phase == Phase.FINISHED) {
            return;
        }
        if (this.taskDone >= this.taskGoal) {
            this.finish(false, "Köylüler görevleri tamamladı!");
            return;
        }
        this.startDay();
    }

    private void startDay() {
        this.phase = Phase.DAY;
        this.dayRemaining = Math.max(20, this.plugin.getConfig().getInt("game.day-seconds", 180));
        this.dayKills.clear();
        this.world.setTime(1000L);
        int n = 0;
        for (UUID uUID : this.members) {
            Player player;
            if (!this.alive.contains(uUID) || (player = Bukkit.getPlayer(uUID)) == null) continue;
            this.plugin.closePage(player);
            this.plugin.teleportInternal(player,MapLayout.seat(this.world, n++));
        }
        this.announce("§eYeni gündüz başladı. " + this.dayRemaining + " saniye.");
    }

    private void resumeDay() {
        this.phase = Phase.DAY;
        for (UUID uUID : this.alive) {
            Player player = Bukkit.getPlayer(uUID);
            Location location = this.returnPositions.get(uUID);
            if (player == null || location == null) continue;
            this.plugin.closePage(player);
            this.plugin.teleportInternal(player,location);
        }
        this.announce("Toplantı bitti. Gündüz kaldığı yerden sürüyor (" + this.dayRemaining + " sn).");
    }

    boolean dayKill(Player player, Player player2) {
        UUID uUID = player.getUniqueId();
        UUID uUID2 = player2.getUniqueId();
        if (this.phase != Phase.DAY || !this.alive.contains(uUID) || !this.alive.contains(uUID2) || this.roles.get(uUID) != Role.VAMPIRE || this.roles.get(uUID2) == Role.VAMPIRE || player.getLocation().distanceSquared(player2.getLocation()) > 12.25) {
            return false;
        }
        int n = Math.max(1, this.plugin.getConfig().getInt("game.day-vampire-kills-per-player", 1));
        long l = 1000L * (long)Math.max(0, this.plugin.getConfig().getInt("game.day-vampire-cooldown-seconds", 45));
        if (this.dayKills.getOrDefault(uUID, 0) >= n || System.currentTimeMillis() - this.lastDayKill.getOrDefault(uUID, 0L) < l) {
            this.plugin.tell(player, "Gündüz av hakkın veya bekleme süren doldu.");
            return false;
        }
        this.dayKills.merge(uUID, 1, Integer::sum);
        this.lastDayKill.put(uUID, System.currentTimeMillis());
        this.eliminate(uUID2, player2.getLocation().clone(), false);
        this.plugin.tell(player, "Av başarılı. Ceset bildirilene kadar kimliğin gizli.");
        return true;
    }

    boolean hunterShot(Player player, Player player2) {
        UUID uUID = player.getUniqueId();
        UUID uUID2 = player2.getUniqueId();
        if (this.phase != Phase.DAY || !this.alive.contains(uUID) || !this.alive.contains(uUID2) || this.roles.get(uUID) != Role.HUNTER) {
            return false;
        }
        this.eliminate(uUID2, player2.getLocation().clone(), true);
        this.announce("§cAvcı " + this.name(uUID2) + " kişisini vurdu. Rolü: §f" + this.roles.get((Object)uUID2).label);
        return true;
    }

    private void eliminate(UUID uUID, Location location, boolean bl) {
        if (!this.alive.remove(uUID)) {
            return;
        }
        Player player = Bukkit.getPlayer(uUID);
        if (location != null && this.world != null) {
            ArmorStand armorStand2 = this.world.spawn(location, ArmorStand.class, armorStand -> {
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setMarker(true);
                armorStand.setCustomName("§c☠ " + this.name(uUID) + " cesedi");
                armorStand.setCustomNameVisible(false);
                armorStand.getEquipment().setHelmet(new ItemStack(Material.PLAYER_HEAD));
            });
            this.corpses.put(armorStand2.getUniqueId(), new Corpse(uUID, armorStand2, System.currentTimeMillis(), MapLayout.zone(location)));
        }
        if (player != null) {
            this.plugin.closePage(player);
            player.setHealth(player.getMaxHealth());
            player.setGameMode(GameMode.SPECTATOR);
            this.plugin.tell(player, "Elendin. Maç bitene kadar sohbet/oy kullanamazsın.");
        }
        this.checkWinner();
    }

    private void checkWinner() {
        if (this.phase == Phase.FINISHED || this.phase == Phase.WAITING) {
            return;
        }
        int n = 0;
        int n2 = 0;
        for (UUID uUID : this.alive) {
            if (this.roles.get(uUID) == Role.VAMPIRE) {
                ++n;
                continue;
            }
            ++n2;
        }
        if (n == 0) {
            this.finish(false, "Bütün vampirler elendi. Köylüler kazandı!");
        } else if (n >= n2) {
            this.finish(true, "Vampirler çoğunluğu ele geçirdi!");
        }
    }

    void reportNearest(Player player) {
        if (this.phase != Phase.DAY || !this.alive.contains(player.getUniqueId())) {
            return;
        }
        Corpse corpse = null;
        double d = 16.0;
        for (Corpse corpse2 : this.corpses.values()) {
            double d2;
            if (corpse2.stand.isDead() || !((d2 = corpse2.stand.getLocation().distanceSquared(player.getLocation())) < d)) continue;
            d = d2;
            corpse = corpse2;
        }
        if (corpse == null) {
            this.plugin.tell(player, "Cesedi bildirmek için 4 blok yakınında ol.");
            return;
        }
        this.corpses.remove(corpse.stand.getUniqueId());
        corpse.stand.remove();
        this.beginMeeting(false, corpse, player.getUniqueId());
    }

    void ringEmergency(Player player) {
        if (this.phase != Phase.DAY || !this.alive.contains(player.getUniqueId())) {
            return;
        }
        if (this.emergencyUsed.contains(player.getUniqueId())) {
            this.plugin.tell(player, "Bu maçta acil çan hakkını kullandın.");
            return;
        }
        long l = (long)Math.max(0, this.plugin.getConfig().getInt("game.emergency-bell-cooldown-seconds", 45)) * 1000L;
        if (System.currentTimeMillis() - this.lastEmergency < l) {
            this.plugin.tell(player, "Çan bekleme süresinde.");
            return;
        }
        this.emergencyUsed.add(player.getUniqueId());
        this.lastEmergency = System.currentTimeMillis();
        this.beginMeeting(false, null, player.getUniqueId());
    }

    Task taskAt(UUID uUID, int n) {
        List<Task> list = this.tasks.get(uUID);
        if (list == null) {
            return null;
        }
        for (Task task : list) {
            if (task.completed || task.station != n && (task.kind != 2 || !task.carrying || task.destination() != n)) continue;
            return task;
        }
        return null;
    }

    void clickStation(Player player, int n) {
        if (this.phase != Phase.DAY || !this.alive.contains(player.getUniqueId())) {
            return;
        }
        if(n<0||n>=MapLayout.STATIONS.size()||player.getWorld()!=world||player.getLocation().distanceSquared(MapLayout.STATIONS.get(n).location(world))>25)return;
        Task task = this.taskAt(player.getUniqueId(), n);
        if (task == null) {
            if (this.firstNightDone && this.roles.get(player.getUniqueId()) != Role.VAMPIRE && this.taskDone < this.taskGoal) {
                long l = this.publicTaskCooldown.getOrDefault(player.getUniqueId(), 0L);
                if (System.currentTimeMillis() - l >= 30000L) {
                    this.plugin.openTaskMenu(player, this, n, null);
                    return;
                }
            }
            this.plugin.tell(player, "Bu istasyonda aktif görevin yok. Görev defterine bak.");
            return;
        }
        if (task.kind == 2) {
            if (!task.carrying && n == task.station) {
                task.carrying = true;
                this.plugin.tell(player, "Paketi aldın. " + MapLayout.STATIONS.get(task.destination()).name() + " noktasına götür.");
            } else if (task.carrying && n == task.destination()) {
                this.completeTask(player, task, false);
            }
        } else {
            this.plugin.openTaskMenu(player, this, n, task);
        }
    }

    void clickTaskGui(Player player, int n, Task task, int n2) {
        int[] nArray;
        if (this.phase != Phase.DAY || !this.alive.contains(player.getUniqueId())) {
            return;
        }
        if (n<0||n>=MapLayout.STATIONS.size()||task!=null&&(!tasks.getOrDefault(player.getUniqueId(),List.of()).contains(task)||task.station!=n)||player.getWorld() != this.world || player.getLocation().distanceSquared(MapLayout.STATIONS.get(n).location(this.world)) > 25.0) {
            return;
        }
        if (task == null) {
            if (n2 == 13 && this.firstNightDone && this.roles.get(player.getUniqueId()) != Role.VAMPIRE && System.currentTimeMillis() - this.publicTaskCooldown.getOrDefault(player.getUniqueId(), 0L) >= 30000L) {
                this.publicTaskCooldown.put(player.getUniqueId(), System.currentTimeMillis());
                ++this.taskDone;
                this.plugin.tell(player, "Ortak bakım görevi tamamlandı.");
                this.refreshBar();
                if (this.taskDone >= this.taskGoal) {
                    this.finish(false, "Köylüler bütün görevleri tamamladı!");
                }
            }
            this.plugin.closePage(player);
            return;
        }
        if (task.completed || task.kind == 2) {
            return;
        }
        if (task.kind == 0) {
            int[] nArray2 = new int[3];
            nArray2[0] = 10;
            nArray2[1] = 13;
            nArray = nArray2;
            nArray2[2] = 16;
        } else {
            int[] nArray3 = new int[3];
            nArray3[0] = 16;
            nArray3[1] = 10;
            nArray = nArray3;
            nArray3[2] = 13;
        }
        int[] nArray4 = nArray;
        if (n2 != nArray4[task.step]) {
            task.step = 0;
            this.plugin.tell(player, "Sıra karıştı; baştan dene.");
            return;
        }
        ++task.step;
        if (task.step == nArray4.length) {
            this.completeTask(player, task, false);
        } else {
            this.plugin.tell(player, "Doğru: " + task.step + "/3");
        }
    }

    private void completeTask(Player player, Task task, boolean bl) {
        if (task.completed) {
            return;
        }
        task.completed = true;
        task.carrying = false;
        this.plugin.closePage(player);
        if (this.roles.get(player.getUniqueId()) == Role.VAMPIRE) {
            this.plugin.tell(player, "Sahte görev tamamlandı. Gerçek ilerleme değişmedi.");
        } else {
            ++this.taskDone;
            this.plugin.tell(player, "Görev tamamlandı! Takım ilerlemesi: " + this.taskDone + "/" + this.taskGoal);
            this.refreshBar();
            if (this.firstNightDone && this.taskDone >= this.taskGoal) {
                this.finish(false, "Köylüler bütün görevleri tamamladı!");
            }
        }
    }

    void taskBook(Player player) {
        List<Task> list = this.tasks.get(player.getUniqueId());
        if (list == null) {
            return;
        }
        this.plugin.tell(player, "Görevler" + (this.roles.get(player.getUniqueId()) == Role.VAMPIRE ? " (sahte)" : "") + ":");
        for (Task task : list) {
            String string = task.kind == 2 ? " → " + MapLayout.STATIONS.get(task.destination()).name() : "";
            this.plugin.tell(player, (task.completed ? "§a✓ " : "§e• ") + MapLayout.STATIONS.get(task.station).name() + string);
        }
        if (this.firstNightDone && this.roles.get(player.getUniqueId()) != Role.VAMPIRE) {
            this.plugin.tell(player, "Ek ortak bakım işleri istasyonlarda yapılabilir.");
        }
    }

    private UUID randomAlive() {
        ArrayList<UUID> arrayList = new ArrayList<UUID>(this.alive);
        return arrayList.isEmpty() ? null : (UUID)arrayList.get(this.random.nextInt(arrayList.size()));
    }

    private UUID randomOther(UUID uUID) {
        ArrayList<UUID> arrayList = new ArrayList<UUID>(this.alive);
        arrayList.remove(uUID);
        return arrayList.isEmpty() ? null : (UUID)arrayList.get(this.random.nextInt(arrayList.size()));
    }

    void refreshBar() {
        if (phase == Phase.FINISHED) return;
        double progress = taskGoal == 0 ? 0 : Math.min(1, (double)taskDone / taskGoal);
        String title;
        switch (phase) {
            case SPEECH -> {
                String speaker = speakerIndex >= 0 && speakerIndex < speakingOrder.size() ? name(speakingOrder.get(speakerIndex)) : "Sıra değişiyor";
                title = "§eŞU AN KONUŞUYOR: §f" + speaker + " §7• §e" + Math.max(0, seconds) + " sn";
                progress = (double)Math.max(0, seconds) / Math.max(5, plugin.getConfig().getInt("game.speaking-seconds-per-player",20));
                bar.setColor(BarColor.YELLOW);
            }
            case VOTE -> { title="§cVAMPİRİ BUL §7• §fOylama " + Math.max(0,seconds) + " sn"; bar.setColor(BarColor.RED); }
            case ROLE -> { title="§9GECE SEÇİMİ §7• §f" + Math.max(0,seconds) + " sn"; bar.setColor(BarColor.BLUE); }
            default -> { title="§5Vampir Köylü §7• §fGündüz " + dayRemaining + " sn §7• §aGörev %" + (int)(progress*100); bar.setColor(BarColor.PURPLE); }
        }
        bar.setTitle(title); bar.setProgress(Math.clamp(progress,0,1));
    }

    void announce(String string) {
        for (UUID uUID : this.members) {
            Player player = Bukkit.getPlayer(uUID);
            if (player == null) continue;
            this.plugin.tell(player, string);
        }
    }

    private String name(UUID uUID) {
        if (uUID == null) {
            return "Pas";
        }
        Player player = Bukkit.getPlayer(uUID);
        return player == null ? uUID.toString().substring(0, 8) : player.getName();
    }

    void finish(Boolean bl, String string) {
        if (this.phase == Phase.FINISHED) {
            return;
        }
        this.phase = Phase.FINISHED;
        this.announce("§6Maç bitti: " + string);
        for (Corpse corpse : this.corpses.values()) {
            corpse.stand.remove();
        }
        this.corpses.clear();
        for(UUID id:members){Player p=Bukkit.getPlayer(id);if(p!=null)plugin.closePage(p);}
        this.bar.removeAll();
        if (guide != null) guide.close();
        this.plugin.roomFinished(this);
    }

    static enum Phase {
        WAITING,
        DAY,
        SPEECH,
        VOTE,
        ROLE,
        FINISHED;

    }

    static enum Role {
        VAMPIRE("Vampir"),
        VILLAGER("Köylü"),
        HUNTER("Avcı"),
        SEER("Kahin"),
        DOCTOR("Doktor");

        final String label;

        private Role(String string2) {
            this.label = string2;
        }
    }

    record Corpse(UUID victim, ArmorStand stand, long time, String zone) {
    }

    static final class Task {
        final int station;
        final int kind;
        boolean completed;
        boolean carrying;
        int step;

        Task(int n) {
            this.station = n;
            this.kind = n % 3;
        }

        int destination() {
            return (this.station + 5) % MapLayout.STATIONS.size();
        }
    }
}
