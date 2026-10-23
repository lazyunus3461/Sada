package tr.kasabatr.vampir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.EnumMap;
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
    final Map<UUID, Long> lastDayKill = new HashMap<UUID, Long>();
    final Map<UUID, Integer> hunterShots = new HashMap<UUID, Integer>();
    final Map<UUID, Long> publicTaskCooldown = new HashMap<UUID, Long>();
    final Map<UUID, Task> commonWork = new HashMap<>();
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
    int fakeTaskDone;
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
        this.bar = Bukkit.createBossBar("§dVampir Köylü §8• §bOda " + n, BarColor.PURPLE, BarStyle.SOLID, new BarFlag[0]);
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
            Map<Role,Integer> quotas=new EnumMap<>(Role.class);quotas.put(Role.VAMPIRE,this.vampireCount);quotas.put(Role.HUNTER,this.hunter?1:0);quotas.put(Role.SEER,this.seer?1:0);quotas.put(Role.DOCTOR,this.doctor?1:0);quotas.put(Role.VILLAGER,this.members.size()-this.vampireCount-(this.hunter?1:0)-(this.seer?1:0)-(this.doctor?1:0));
            Map<UUID,Role> preferences=new HashMap<>();for(UUID id:this.members){Player p=Bukkit.getPlayer(id);if(p!=null&&tr.kasaba.hub.api.NetworkFeatures.isVip(p)){String name=this.plugin.getConfig().getString("vip-rol-tercihleri."+id,"");try{preferences.put(id,Role.valueOf(name));}catch(IllegalArgumentException ignored){}}}
            this.roles.putAll(RoleLottery.assign(new ArrayList<>(this.members),quotas,preferences,this.random));
            this.alive.addAll(this.members);
            this.taskGoal = (this.startingPlayers - this.vampireCount) * 3;
            for (UUID uUID : this.members) {
                ArrayList<Integer> arrayList2 = new ArrayList<Integer>();
                for (int i = 0; i < MapLayout.STATIONS.size(); ++i) {
                    arrayList2.add(i);
                }
                Collections.shuffle(arrayList2, this.random);
                List<Task> assigned = new ArrayList<>();
                for(int kind=0;kind<4;kind++)assigned.add(new Task(arrayList2.get(kind),kind));
                this.tasks.put(uUID, List.copyOf(assigned));
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
                this.plugin.tell(player, "7. kutudaki görev pusulasına sağ tıkla, hedef seç ve yerdeki mavi okları izle. Okları yalnız sen görürsün. /vk rehber ile kuralları oku.");
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
        for(UUID id:List.copyOf(this.alive)){Player player=Bukkit.getPlayer(id);if(player!=null)interruptTaskWork(player,player.getLocation());}
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
        long remaining = dayKillRemainingMillis(uUID);
        if (remaining > 0) {
            this.plugin.tell(player, "Yeniden avlanmak için " + (remaining + 999) / 1000 + " saniye bekle.");
            return false;
        }
        this.lastDayKill.put(uUID, System.currentTimeMillis());
        this.eliminate(uUID2, player2.getLocation().clone(), false);
        this.plugin.tell(player, "Av başarılı. Yeni avın " + (dayKillRemainingMillis(uUID) + 999) / 1000 + " saniye sonra hazır.");
        return true;
    }

    long dayKillRemainingMillis(UUID player) {
        Long last = this.lastDayKill.get(player);
        if (last == null) return 0;
        long cooldown = 1000L * Math.max(0, this.plugin.getConfig().getInt("game.day-vampire-cooldown-seconds", 40));
        return Math.max(0, cooldown - (System.currentTimeMillis() - last));
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

    Task taskAt(UUID player, int station) {
        List<Task> assigned=this.tasks.getOrDefault(player,List.of());
        for(Task task:assigned)if(!task.completed&&task.kind==2&&task.carrying&&task.destination()==station)return task;
        for(Task task:assigned)if(!task.completed&&task.station==station)return task;
        return null;
    }

    void interruptTaskWork(Player player,Location position) {
        List<Task> work=new ArrayList<>(tasks.getOrDefault(player.getUniqueId(),List.of()));
        Task common=commonWork.get(player.getUniqueId());if(common!=null)work.add(common);
        for(Task task:work)if(!task.completed&&!task.carrying&&task.work.startedAt>=0
            &&(phase!=Phase.DAY||position.getWorld()!=world||position.distanceSquared(MapLayout.STATIONS.get(task.station).location(world))>25))task.work.interrupt();
    }

    void clickStation(Player player, int station) {
        UUID id=player.getUniqueId();
        if(phase!=Phase.DAY||!alive.contains(id)||station<0||station>=MapLayout.STATIONS.size()
            ||player.getWorld()!=world||player.getLocation().distanceSquared(MapLayout.STATIONS.get(station).location(world))>25)return;
        interruptTaskWork(player,player.getLocation());
        Task task=taskAt(id,station);
        if(task!=null&&task.kind==2&&task.carrying){
            if(station!=task.destination()){plugin.tell(player,"Paket sende. Teslim yeri: "+MapLayout.STATIONS.get(task.destination()).name());return;}
            if(task.leg++==0){plugin.tell(player,"İlk teslimat tamam. Son durak: "+MapLayout.STATIONS.get(task.destination()).name()+". Yerdeki mavi okları izle.");}
            else completeTask(player,task,false);
            return;
        }
        if(task==null){
            if(!firstNightDone||taskDone>=taskGoal){plugin.tell(player,"Bu istasyonda aktif görevin yok. Görev defterine bak.");return;}
            long wait=30000-(System.currentTimeMillis()-publicTaskCooldown.getOrDefault(id,0L));
            if(wait>0){plugin.tell(player,"Ortak bakım için "+(wait+999)/1000+" saniye bekle.");return;}
            task=commonWork.get(id);
            if(task==null||task.station!=station||task.completed){task=new Task(station,0,true);commonWork.put(id,task);}
        }
        task.work.begin(System.currentTimeMillis());
        plugin.openTaskMenu(player,this,station,task);
    }

    void clickTaskGui(Player player,int station,Task task,int action) {
        UUID id=player.getUniqueId();
        if(phase!=Phase.DAY||!alive.contains(id)||task==null||task.completed||task.carrying||task.station!=station
            ||station<0||station>=MapLayout.STATIONS.size()||player.getWorld()!=world
            ||player.getLocation().distanceSquared(MapLayout.STATIONS.get(station).location(world))>25
            ||!(tasks.getOrDefault(id,List.of()).contains(task)||commonWork.get(id)==task))return;
        if(task.maintenance&&(!firstNightDone||taskDone>=taskGoal||System.currentTimeMillis()-publicTaskCooldown.getOrDefault(id,0L)<30000))return;
        long now=System.currentTimeMillis();TaskWork.Result result=task.work.select(action,now);
        switch(result){
            case WAIT -> plugin.tell(player,"İşlem sürüyor: "+Math.max(1,(task.work.remaining(action,now)+999)/1000)+" saniye bekle.");
            case WRONG -> plugin.tell(player,"Sıra karıştı. Bu aşamayı yeniden dene.");
            case ROUND -> plugin.tell(player,"Yeni diziyi incele. Hazır olduğunda gizleyip devam et.");
            case HIDDEN -> plugin.tell(player,"Dizi gizlendi. Hatırladığın sıraya bas.");
            case STEP -> plugin.tell(player,"Doğru: "+task.work.step+"/"+task.work.sequence.length);
            case DONE -> {
                if(task.kind==2){task.carrying=true;if(guide!=null)guide.tracked.put(id,tasks.get(id).indexOf(task));plugin.closePage(player);plugin.tell(player,"Paket hazır. İlk durak: "+MapLayout.STATIONS.get(task.destination()).name()+". Yerdeki mavi okları izle.");}
                else if(task.maintenance){task.completed=true;publicTaskCooldown.put(id,now);commonWork.remove(id,task);plugin.closePage(player);recordTaskCompletion(player,"Ortak bakım görevi tamamlandı.");}
                else completeTask(player,task,false);
            }
            default -> { }
        }
    }

    private void completeTask(Player player, Task task, boolean bl) {
        if (task.completed) {
            return;
        }
        task.completed = true;
        task.carrying = false;
        this.plugin.closePage(player);
        this.recordTaskCompletion(player, "Görev tamamlandı!");
    }

    private void recordTaskCompletion(Player player, String message) {
        if (this.roles.get(player.getUniqueId()) == Role.VAMPIRE) ++this.fakeTaskDone;
        else ++this.taskDone;
        this.plugin.tell(player, message);
        this.refreshBar();
        if (this.firstNightDone && this.taskDone >= this.taskGoal) {
            this.finish(false, "Köylüler bütün görevleri tamamladı!");
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
        if (this.firstNightDone) {
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
        double progress = TaskProgress.visible(this.taskDone, this.fakeTaskDone, this.taskGoal);
        String title;
        switch (phase) {
            case SPEECH -> {
                String speaker = speakerIndex >= 0 && speakerIndex < speakingOrder.size() ? name(speakingOrder.get(speakerIndex)) : "Sıra değişiyor";
                title = "§eŞU AN KONUŞUYOR: §b" + speaker + " §8• §6" + Math.max(0, seconds) + " sn";
                progress = (double)Math.max(0, seconds) / Math.max(5, plugin.getConfig().getInt("game.speaking-seconds-per-player",20));
                bar.setColor(BarColor.YELLOW);
            }
            case VOTE -> { title="§cVAMPİRİ BUL §8• §eOylama §6" + Math.max(0,seconds) + " sn"; bar.setColor(BarColor.RED); }
            case ROLE -> { title="§bGECE SEÇİMİ §8• §6" + Math.max(0,seconds) + " sn"; bar.setColor(BarColor.BLUE); }
            default -> { title="§dVampir Köylü §8• §bGündüz §6" + dayRemaining + " sn §8• §aGörev %" + (int)(progress*100); bar.setColor(BarColor.PURPLE); }
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
        final int station,kind;
        final boolean maintenance;
        final TaskWork work;
        boolean completed,carrying;
        int leg;
        Task(int station){this(station,station%4);}
        Task(int station,int kind){this(station,kind,false);}
        Task(int station,int kind,boolean maintenance){this.station=station;this.kind=kind;this.maintenance=maintenance;this.work=new TaskWork(kind,new Random());}
        int destination(){int first=MapLayout.deliveryStop(station,-1);return leg==0?first:MapLayout.deliveryStop(first,station);}
        String status(){if(completed)return "Tamamlandı";if(carrying)return "Teslimat "+(leg+1)+"/2";return work.title()+" • "+work.difficulty()+(kind==3?" • Tur "+(work.round+1)+"/2":"");}
    }
}
