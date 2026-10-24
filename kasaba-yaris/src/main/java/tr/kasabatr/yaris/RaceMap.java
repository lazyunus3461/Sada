/*
 * Decompiled with CFR 0.152.
 */
package tr.kasabatr.yaris;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import tr.kasabatr.yaris.Mode;

final class RaceMap {
    final String worldName;
    final Point lobby;
    final Course boat;
    final Course horse;

    RaceMap(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("race-map.yml yok: " + String.valueOf(file));
        }
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        this.worldName = yamlConfiguration.getString("world", "");
        if (!this.worldName.matches("[A-Za-z0-9_-]{1,80}")) {
            throw new IllegalArgumentException("Harita dünya adı geçersiz");
        }
        this.lobby = RaceMap.point(RaceMap.section(yamlConfiguration, "lobby"));
        this.boat = RaceMap.course(RaceMap.section(yamlConfiguration, "boat"));
        this.horse = RaceMap.course(RaceMap.section(yamlConfiguration, "horse"));
    }

    Course course(Mode mode) {
        return mode == Mode.BOAT ? this.boat : this.horse;
    }

    private static Course course(ConfigurationSection configurationSection) {
        ArrayList<Point> arrayList = new ArrayList<Point>();
        for (Map<?, ?> object22 : configurationSection.getMapList("start_lanes")) {
            arrayList.add(RaceMap.point(object22));
        }
        ArrayList arrayList2 = new ArrayList();
        for (Map<?, ?> map : configurationSection.getMapList("checkpoints")) {
            arrayList2.add(RaceMap.gate(map));
        }
        Gate gate = RaceMap.gate(RaceMap.section(configurationSection, "finish"));
        Point point = configurationSection.isConfigurationSection("spectator") ? RaceMap.point(RaceMap.section(configurationSection, "spectator")) : (Point)arrayList.getFirst();
        return new Course(arrayList, arrayList2, gate, point);
    }

    private static ConfigurationSection section(ConfigurationSection configurationSection, String string) {
        ConfigurationSection configurationSection2 = configurationSection.getConfigurationSection(string);
        if (configurationSection2 == null) {
            throw new IllegalArgumentException("Harita alanı yok: " + configurationSection.getCurrentPath() + "." + string);
        }
        return configurationSection2;
    }

    private static Point point(ConfigurationSection configurationSection) {
        return new Point(configurationSection.getDouble("x"), configurationSection.getDouble("y"), configurationSection.getDouble("z"), (float)configurationSection.getDouble("yaw"), (float)configurationSection.getDouble("pitch"));
    }

    private static Point point(Map<?, ?> map) {
        return new Point(RaceMap.number(map, "x"), RaceMap.number(map, "y"), RaceMap.number(map, "z"), RaceMap.number(map, "yaw"), RaceMap.number(map, "pitch"));
    }

    private static Gate gate(ConfigurationSection configurationSection) {
        return new Gate(configurationSection.getDouble("x"), configurationSection.getDouble("y"), configurationSection.getDouble("z"), Math.max(1.0, configurationSection.getDouble("radius", 8.0)));
    }

    private static Gate gate(Map<?, ?> map) {
        return new Gate(RaceMap.number(map, "x"), RaceMap.number(map, "y"), RaceMap.number(map, "z"), Math.max(1.0f, RaceMap.number(map, "radius")));
    }

    private static float number(Map<?, ?> map, String string) {
        Object obj = map.get(string);
        if (!(obj instanceof Number)) {
            throw new IllegalArgumentException("Harita sayısı yok: " + string);
        }
        Number number = (Number)obj;
        return number.floatValue();
    }

    record Point(double x, double y, double z, float yaw, float pitch) {
        Location at(World world) {
            return new Location(world, this.x, this.y, this.z, this.yaw, this.pitch);
        }
    }

    record Course(List<Point> startLanes, List<Gate> checkpoints, Gate finish, Point spectator) {
        Course {
            startLanes = List.copyOf(startLanes);
            checkpoints = List.copyOf(checkpoints);
            if (startLanes.size() != 5 || checkpoints.size() < 3) {
                throw new IllegalArgumentException("Her parkurda 5 başlangıç şeridi ve en az 3 kontrol noktası olmalı");
            }
        }
    }

    record Gate(double x, double y, double z, double radius) {
        boolean contains(Location location) {
            double d = location.getX() - this.x;
            double d2 = location.getZ() - this.z;
            return Math.abs(location.getY() - this.y) <= 7.0 && d * d + d2 * d2 <= this.radius * this.radius;
        }
    }
}
