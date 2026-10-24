package tr.kasabatr.esya;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

/** Validated contract shared by the Anvil generator and the game. */
final class ToyMap {
    record Point(double x, double y, double z, float yaw) {
        Location at(World world) { return new Location(world, x, y, z, yaw, 0); }
    }
    record Pos(int x, int y, int z) { }
    record Decoy(Pos pos, Material material) { }
    final String worldName;
    final Point lobby, seekerWait, seekerRelease;
    final List<Point> hiderSpawns;
    final List<Material> materials;
    final List<Decoy> decoys;
    final Set<Pos> decoyPositions;
    final List<Pos> decoyCandidates;
    final int minX, maxX, minY, maxY, minZ, maxZ, hideSeconds, matchSeconds;

    ToyMap(Path file) throws IOException {
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            worldName = root.get("world").getAsString();
            if (!worldName.matches("[A-Za-z0-9_-]{1,80}")) throw new IllegalArgumentException("Dünya adı geçersiz");
            lobby = point(root.getAsJsonObject("lobby"));
            seekerWait = point(root.getAsJsonObject("seeker_wait"));
            seekerRelease = point(root.getAsJsonObject("seeker_release"));
            hiderSpawns = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("hider_spawns")) hiderSpawns.add(point(element.getAsJsonObject()));
            if (hiderSpawns.size() < 11) throw new IllegalArgumentException("En az 11 saklanan doğma noktası gerekli");
            materials = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("prop_materials")) {
                Material material = Material.matchMaterial(element.getAsString());
                if (material == null || !material.isBlock() || material.isAir() || materials.contains(material))
                    throw new IllegalArgumentException("Kılık materyali geçersiz");
                materials.add(material);
            }
            if (materials.size() < 4 || materials.size()>12) throw new IllegalArgumentException("En az 4 kılık gerekli");
            decoys = new ArrayList<>();
            decoyPositions = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray("decoys")) {
                JsonObject item = element.getAsJsonObject();
                Pos pos = new Pos(item.get("x").getAsInt(), item.get("y").getAsInt(), item.get("z").getAsInt());
                Material material = Material.matchMaterial(item.get("material").getAsString());
                if (material == null || !materials.contains(material) || !decoyPositions.add(pos))
                    throw new IllegalArgumentException("Yinelenen/geçersiz yem: " + pos);
                decoys.add(new Decoy(pos, material));
            }
            if (decoys.size() < 40) throw new IllegalArgumentException("Yem sayısı yetersiz");
            decoyCandidates = new ArrayList<>();
            Set<Pos> sites = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray("decoy_candidates")) {
                JsonObject item = element.getAsJsonObject();
                Pos pos = new Pos(item.get("x").getAsInt(), item.get("y").getAsInt(), item.get("z").getAsInt());
                if (!sites.add(pos)) throw new IllegalArgumentException("Yinelenen yem adayı: " + pos);
                decoyCandidates.add(pos);
            }
            if (decoyCandidates.size() < decoys.size() * 2 || !sites.containsAll(decoyPositions))
                throw new IllegalArgumentException("Yem adayları eksik");
            JsonObject b = root.getAsJsonObject("bounds");
            minX = b.get("min_x").getAsInt(); maxX = b.get("max_x").getAsInt();
            minY = b.get("min_y").getAsInt(); maxY = b.get("max_y").getAsInt();
            minZ = b.get("min_z").getAsInt(); maxZ = b.get("max_z").getAsInt();
            hideSeconds = root.get("hide_seconds").getAsInt();
            matchSeconds = root.get("match_seconds").getAsInt();
            if (minX >= maxX || minY >= maxY || minZ >= maxZ || hideSeconds < 5 || hideSeconds>120 || matchSeconds < 60 || matchSeconds>1800)
                throw new IllegalArgumentException("Harita sınırı veya süre geçersiz");
            for(Point point:hiderSpawns)if(!contains(point))throw new IllegalArgumentException("Doğma noktası sınır dışında.");
            if(!contains(lobby)||!contains(seekerWait)||!contains(seekerRelease))throw new IllegalArgumentException("Lobi/ebe noktası sınır dışında.");
            for(Pos pos:decoyCandidates)if(pos.x<minX||pos.x>maxX||pos.y<minY||pos.y>maxY||pos.z<minZ||pos.z>maxZ)throw new IllegalArgumentException("Yem adayı sınır dışında.");
        } catch (Exception error) { throw new IOException("prop-map.json okunamadı: " + error.getMessage(), error); }
    }

    private boolean contains(Point p){return p.x>=minX&&p.x<maxX+1&&p.y>=minY&&p.y<maxY+2&&p.z>=minZ&&p.z<maxZ+1;}
    boolean contains(Location at) {
        return at.getX() >= minX && at.getX() < maxX + 1 && at.getY() >= minY && at.getY() < maxY + 2
            && at.getZ() >= minZ && at.getZ() < maxZ + 1;
    }
    static Pos pos(Location at) { return new Pos(at.getBlockX(), at.getBlockY(), at.getBlockZ()); }
    private static Point point(JsonObject o) {
        return new Point(o.get("x").getAsDouble(), o.get("y").getAsDouble(),
            o.get("z").getAsDouble(), o.get("yaw").getAsFloat());
    }
}
