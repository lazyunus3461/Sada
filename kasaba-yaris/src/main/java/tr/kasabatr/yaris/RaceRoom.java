/*
 * Decompiled with CFR 0.152.
 */
package tr.kasabatr.yaris;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import tr.kasabatr.yaris.KasabaYarisPlugin;
import tr.kasabatr.yaris.LapProgress;
import tr.kasabatr.yaris.Mode;
import tr.kasabatr.yaris.RaceMap;

final class RaceRoom {
    final KasabaYarisPlugin plugin;
    final int id;
    final Mode mode;
    final Map<UUID, Racer> racers = new LinkedHashMap<UUID, Racer>();
    UUID owner;
    State state = State.WAITING;
    World world;
    BossBar bar;
    private long tick;
    private int countdown;
    private int resultTicks;
    private long raceStartMillis;
    private final Map<String,Long> bumps=new java.util.HashMap<>();

    RaceRoom(KasabaYarisPlugin kasabaYarisPlugin, int n, Mode mode, Player player) {
        this.plugin = kasabaYarisPlugin;
        this.id = n;
        this.mode = mode;
        this.owner = player.getUniqueId();
        this.racers.put(player.getUniqueId(), new Racer(player));
    }

    boolean add(Player player) {
        if (this.state != State.WAITING || this.racers.size() >= this.plugin.maximum() || this.racers.containsKey(player.getUniqueId())) {
            return false;
        }
        this.racers.put(player.getUniqueId(), new Racer(player));
        this.announce("§e" + player.getName() + " §fkatıldı. §7(" + this.racers.size() + "/" + this.plugin.maximum() + ")");
        return true;
    }

    void remove(UUID uUID) {
        Racer racer = this.racers.get(uUID);
        if (racer == null) {
            return;
        }
        racer.active = false;
        if (racer.finishMillis < 0L) {
            racer.dnf = true;
        }
        if (racer.mount != null) {
            racer.mount.eject();
            racer.mount.remove();
            racer.mount = null;
        }
        Player player = Bukkit.getPlayer(uUID);
        if (this.bar != null && player != null) {
            this.bar.removePlayer(player);
        }
        if (this.state == State.WAITING) {
            this.racers.remove(uUID);
            if (this.owner.equals(uUID) && !this.racers.isEmpty()) {
                this.owner = this.racers.keySet().iterator().next();
            }
            this.announce("§e" + racer.name + " §fayrıldı. Oda sahibi: §e" + this.ownerName());
        } else if (this.state == State.RACING || this.state == State.COUNTDOWN) {
            this.announce("§e" + racer.name + " §7yarıştan ayrıldı.");
            if(activeCount()==1){winByForfeit();return;}
            if (this.state == State.RACING && this.activePending() == 0) {
                this.showResults();
            }
        }
        if (this.state == State.COUNTDOWN && this.activeCount() < this.plugin.minimum()) {
            this.announce("§cBaşlangıç iptal edildi: en az " + this.plugin.minimum() + " yarışçı gerekli.");
            this.plugin.closeRoom(this);
            return;
        }
        if (this.activeCount() == 0) {
            this.plugin.closeRoom(this);
        }
    }

    void start() throws IOException {
        if (this.state != State.WAITING || this.racers.size() < this.plugin.minimum()) {
            throw new IllegalStateException("En az " + this.plugin.minimum() + " oyuncu gerekli");
        }
        this.world = this.plugin.instances().create(this.id, this.plugin.map());
        RaceObstacles.build(this.world,this.mode);
        RaceMap.Course course = this.plugin.map().course(this.mode);
        int n = 0;
        try {
            Player player;
            for (Racer racer : this.racers.values()) {
                player = Bukkit.getPlayer(racer.id);
                if (player == null || !player.isOnline()) {
                    throw new IOException("Oyuncu çevrimdışı: " + racer.name);
                }
                Location location = course.startLanes().get(n++).at(this.world);
                racer.lastSafe = location.clone();
                racer.lastX = location.getX();
                racer.progress = new LapProgress(course.checkpoints().size(), this.plugin.laps());
                this.plugin.prepareRacer(player, location);
                racer.mount = this.spawnMount(player, location);
            }
            this.state = State.COUNTDOWN;
            this.countdown = this.plugin.getConfig().getInt("countdown-seconds", 5) * 20;
            this.bar = Bukkit.createBossBar("§b" + this.mode.title + " §7• başlangıç", BarColor.BLUE, BarStyle.SOLID, new BarFlag[0]);
            for (Racer racer : this.racers.values()) {
                player = Bukkit.getPlayer(racer.id);
                if (player == null) continue;
                this.bar.addPlayer(player);
            }
            this.announce("§e" + this.mode.title + " §fbaşlıyor. §7" + this.plugin.laps() + " tur, " + this.racers.size() + " oyuncu.");
        }
        catch (Exception exception) {
            for (Racer racer : this.racers.values()) {
                if (racer.mount == null) continue;
                racer.mount.eject();
                racer.mount.remove();
                racer.mount = null;
            }
            if (exception instanceof IOException) {
                IOException iOException = (IOException)exception;
                throw iOException;
            }
            throw new IOException("Yarış başlatılamadı: " + exception.getMessage(), exception);
        }
    }

    private Entity spawnMount(Player player, Location location) {
        if (this.mode == Mode.BOAT) {
            Boat boat = (Boat)this.world.spawnEntity(location, EntityType.OAK_BOAT);
            boat.setInvulnerable(true);
            if (!boat.addPassenger(player)) {
                throw new IllegalStateException("Oyuncu tekneye bindirilemedi");
            }
            return boat;
        }
        Horse horse = (Horse)this.world.spawnEntity(location, EntityType.HORSE);
        horse.setAdult();horse.setCollidable(true);
        horse.setTamed(true);
        horse.setOwner(player);
        horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        horse.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.225);
        horse.getAttribute(Attribute.JUMP_STRENGTH).setBaseValue(0.55);
        horse.setInvulnerable(true);
        horse.setRemoveWhenFarAway(false);
        if (!horse.addPassenger(player)) {
            throw new IllegalStateException("Oyuncu ata bindirilemedi");
        }
        return horse;
    }

    void tick() {
        if (this.state == State.CLOSED || this.state == State.WAITING) {
            return;
        }
        ++this.tick;
        if (this.state == State.COUNTDOWN) {
            if (this.tick % 20L == 1L) {
                int n = Math.max(1, (this.countdown + 19) / 20);
                this.titleAll("§e" + n, "§fBaşlamaya hazırlan");
            }
            for (Racer racer2 : this.racers.values()) {
                if (!racer2.active || racer2.mount == null || !racer2.mount.isValid()) continue;
                this.plugin.moveMount(racer2.mount,racer2.lastSafe);racer2.mount.setVelocity(new Vector(0, 0, 0));
            }
            if (--this.countdown <= 0) {
                this.state = State.RACING;
                this.raceStartMillis = System.currentTimeMillis();
                this.titleAll("§aBAŞLA!", "§fKontrol noktalarını sırayla geç");
                this.bar.setTitle("§b" + this.mode.title + " §7• " + this.plugin.laps() + " tur");
            }
            return;
        }
        if (this.state == State.RESULTS) {
            if (--this.resultTicks <= 0) {
                this.plugin.closeRoom(this);
            }
            return;
        }
        if (System.currentTimeMillis() - this.raceStartMillis >= (long)this.plugin.timeLimitSeconds() * 1000L) {
            for (Racer racer3 : this.racers.values()) {
                if (!racer3.active || racer3.finishMillis >= 0L) continue;
                racer3.dnf = true;
            }
            this.showResults();
            return;
        }
        RaceMap.Course course = this.plugin.map().course(this.mode);
        for (Racer racer4 : this.racers.values()) {
            if (!racer4.active || racer4.finishMillis >= 0L || racer4.dnf) continue;
            Player player = Bukkit.getPlayer(racer4.id);
            if (player == null || !player.isOnline()) {
                racer4.dnf = true;
                continue;
            }
            Entity entity = racer4.mount;
            if (entity == null || !entity.isValid() || entity.getWorld() != this.world) {
                racer4.dnf = true;
                RaceRoom.tell(player, "§cBineğin kayboldu. /yaris ayril ile lobiye dönebilirsin.");
                continue;
            }
            if (!entity.getPassengers().contains(player)) {
                if (this.tick % 20L != 0L) continue;
                entity.addPassenger(player);
                continue;
            }
            Location location = entity.getLocation();
            CourseGeometry.Position track=CourseGeometry.at(mode==Mode.HORSE?190:-190,location.getX(),location.getZ());
            if(Math.abs(track.lateral())>12.5){plugin.moveMount(entity,racer4.lastSafe);racer4.lastX=racer4.lastSafe.getX();if(tick%20==0)tell(player,"§eParkurun dışına çıktın; kontrol noktasına döndün.");continue;}
            double edge=System.currentTimeMillis()-raceStartMillis<3000||Math.abs(track.lateral())<7?1:0.6;
            if(mode==Mode.HORSE)((Horse)entity).getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(0.225*track.speedScale()*edge);
            else {Vector velocity=entity.getVelocity();double cap=0.36*track.speedScale()*edge,speed=Math.hypot(velocity.getX(),velocity.getZ());if(speed>cap){velocity.setX(velocity.getX()*cap/speed);velocity.setZ(velocity.getZ()*cap/speed);entity.setVelocity(velocity);}}
            if (location.getY() < 61.0 || location.getY() > 95.0) {
                this.plugin.moveMount(entity,racer4.lastSafe);
                racer4.lastX = racer4.lastSafe.getX();
                RaceRoom.tell(player, "§eParkura geri alındın.");
                continue;
            }
            boolean bl = location.getX() > racer4.lastX + 0.02;
            racer4.lastX = location.getX();
            if (this.tick - racer4.lastGateTick < 20L) continue;
            if (racer4.progress.nextCheckpoint() < course.checkpoints().size()) {
                int n = racer4.progress.nextCheckpoint();
                RaceMap.Gate gate = course.checkpoints().get(n);
                if (!gate.contains(location)) continue;
                racer4.progress.recordCheckpoint(n);
                racer4.lastGateTick = this.tick;
                racer4.lastSafe = location.clone();
                RaceRoom.tell(player, "§aKontrol noktası §f" + racer4.progress.nextCheckpoint() + "/" + course.checkpoints().size() + " §7• Tur " + racer4.progress.lap() + "/" + this.plugin.laps());
                continue;
            }
            if (!bl || !(location.getX() >= course.finish().x()) || !course.finish().contains(location)) continue;
            racer4.lastGateTick = this.tick;
            LapProgress.Finish finish = racer4.progress.recordFinish();
            if (finish == LapProgress.Finish.RACE_FINISHED) {
                racer4.finishMillis = System.currentTimeMillis() - this.raceStartMillis;
                int n = (int)this.racers.values().stream().filter(racer -> racer.finishMillis >= 0L).count();
                RaceRoom.title(player, "§6" + n + ". BİTİRDİN!", "§fSüre: " + RaceRoom.format(racer4.finishMillis));
                this.announce("§6" + n + ". §e" + racer4.name + " §7• " + RaceRoom.format(racer4.finishMillis));
                if (racer4.mount != null) {
                    racer4.mount.eject();
                    racer4.mount.remove();
                    racer4.mount = null;
                }
                player.setGameMode(GameMode.SPECTATOR);
                this.plugin.teleportInternal(player, course.spectator().at(this.world));
                continue;
            }
            if (finish != LapProgress.Finish.NEXT_LAP) continue;
            racer4.lastSafe = location.clone();
            RaceRoom.title(player, "§aTUR " + racer4.progress.lap(), "§fSonraki kontrol noktalarına git");
        }
        if(mode==Mode.HORSE)bumpHorses();
        if (this.tick % 20L == 0L) {
            long l = (long)this.plugin.timeLimitSeconds() - (System.currentTimeMillis() - this.raceStartMillis) / 1000L;
            this.bar.setProgress(Math.max(0.0, Math.min(1.0, (double)l / (double)this.plugin.timeLimitSeconds())));
            this.bar.setTitle("§b" + this.mode.title + " §7• Kalan süre " + l + " sn");
        }
        if (this.activePending() == 0) {
            this.showResults();
        }
    }

    private void bumpHorses(){
        List<Racer> active=racers.values().stream().filter(r->r.active&&!r.dnf&&r.finishMillis<0&&r.mount!=null&&r.mount.isValid()).toList();
        for(int i=0;i<active.size();i++)for(int j=i+1;j<active.size();j++){
            Racer a=active.get(i),b=active.get(j);String key=a.id+"/"+b.id;
            if(tick-bumps.getOrDefault(key,-100L)<8)continue;
            Vector delta=b.mount.getLocation().toVector().subtract(a.mount.getLocation().toVector());
            if(Math.abs(delta.getY())>1.5)continue;delta.setY(0);double distance=delta.length();if(distance>=1.6)continue;
            if(distance<0.01)delta=new Vector(0,0,1);else delta.multiply(1/distance);
            Vector force=delta.multiply(0.11);a.mount.setVelocity(a.mount.getVelocity().subtract(force));b.mount.setVelocity(b.mount.getVelocity().add(force));bumps.put(key,tick);
        }
    }

    private int activePending() {
        int n = 0;
        for (Racer racer : this.racers.values()) {
            if (!racer.active || racer.dnf || racer.finishMillis >= 0L) continue;
            ++n;
        }
        return n;
    }

    private int activeCount() {
        int n = 0;
        for (Racer racer : this.racers.values()) {
            if (!racer.active) continue;
            ++n;
        }
        return n;
    }

    private void winByForfeit(){for(Racer racer:racers.values())if(racer.active){racer.dnf=false;if(racer.finishMillis<0)racer.finishMillis=raceStartMillis==0?0:Math.max(0,System.currentTimeMillis()-raceStartMillis);Player p=Bukkit.getPlayer(racer.id);if(p!=null){title(p,"§a§lKAZANDIN!","§eDiğer yarışçılar ayrıldı • Lobiye 3 sn");tell(p,"§aKazandın! §e3 saniye sonra yarış lobisine döneceksin.");}}showResults();resultTicks=60;}

    private void showResults() {
        if (this.state == State.RESULTS || this.state == State.CLOSED) {
            return;
        }
        this.state = State.RESULTS;
        this.resultTicks = this.plugin.resultsSeconds() * 20;
        if (this.bar != null) {
            this.bar.setColor(BarColor.GREEN);
            this.bar.setProgress(1.0);
            this.bar.setTitle("§a" + this.mode.title + " §7• Sonuçlar");
        }
        this.announce("§6§l" + this.mode.title + " sonuçları §7(oda " + this.id + ")");
        int n = 1;
        for (Racer racer : this.ranking()) {
            this.announce("§e" + n++ + ". §f" + racer.name + " §7• " + (racer.finishMillis >= 0L ? RaceRoom.format(racer.finishMillis) : "Bitiremedi"));
        }
        this.announce("§7" + this.plugin.resultsSeconds() + " saniye sonra yarış lobisine dönülecek.");
    }

    List<Racer> ranking() {
        ArrayList<Racer> arrayList = new ArrayList<Racer>(this.racers.values());
        arrayList.sort(Comparator.<Racer,Boolean>comparing(racer -> racer.finishMillis < 0L).thenComparingLong(racer -> racer.finishMillis < 0L ? Long.MAX_VALUE : racer.finishMillis));
        return arrayList;
    }

    void close() {
        this.state = State.CLOSED;
        if (this.bar != null) {
            this.bar.removeAll();
            this.bar = null;
        }
        for (Racer racer : this.racers.values()) {
            if (racer.mount == null) continue;
            racer.mount.eject();
            racer.mount.remove();
            racer.mount = null;
        }
    }

    String ownerName() {
        Racer racer = this.racers.get(this.owner);
        return racer == null ? "-" : racer.name;
    }

    String summary() {
        return "§e#" + this.id + " §f" + this.mode.title + " §7" + this.racers.size() + "/" + this.plugin.maximum() + " • " + this.ownerName();
    }

    void announce(String string) {
        for (Racer racer : this.racers.values()) {
            Player player = Bukkit.getPlayer(racer.id);
            if (player == null || !player.isOnline()) continue;
            RaceRoom.tell(player, string);
        }
    }

    private void titleAll(String string, String string2) {
        for (Racer racer : this.racers.values()) {
            Player player = Bukkit.getPlayer(racer.id);
            if (player == null || !player.isOnline()) continue;
            RaceRoom.title(player, string, string2);
        }
    }

    private static void title(Player player, String string, String string2) {
        player.sendTitle(string, string2, 0, 30, 5);
    }

    private static void tell(Player player, String string) {
        player.sendMessage("§b[Kasaba Yarış] §f" + string);
    }

    private static String format(long l) {
        long l2 = l / 1000L;
        return String.format("%02d:%02d.%03d", l2 / 60L, l2 % 60L, l % 1000L);
    }

    static enum State {
        WAITING,
        COUNTDOWN,
        RACING,
        RESULTS,
        CLOSED;

    }

    static final class Racer {
        final UUID id;
        final String name;
        Entity mount;
        Location lastSafe;
        double lastX;
        LapProgress progress;
        long lastGateTick;
        long finishMillis = -1L;
        boolean active = true;
        boolean dnf;

        Racer(Player player) {
            this.id = player.getUniqueId();
            this.name = player.getName();
        }
    }
}
