package tr.kasaba.auth;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

/** Separate from account data and world spawn; updates replace the file atomically. */
final class AuthSpawnStore {
    record Point(UUID world, String worldName, double x, double y, double z, float yaw, float pitch) {
        Point {
            if (world == null || worldName == null || worldName.isBlank()
                    || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)
                    || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Geçersiz kayıt yeri");
        }
    }

    private final Path file;
    AuthSpawnStore(Path file) { this.file = file; }

    Point load() throws IOException {
        if (!Files.exists(file)) return null;
        YamlConfiguration yaml = new YamlConfiguration();
        try { yaml.load(file.toFile()); }
        catch (InvalidConfigurationException e) { throw new IOException("Kayıt yeri dosyası bozuk", e); }
        if (!yaml.isBoolean("enabled")) throw new IOException("Kayıt yeri enabled alanı true/false olmalı");
        if (!yaml.getBoolean("enabled")) return null;
        for (String key : new String[]{"x", "y", "z", "yaw", "pitch"})
            if (!(yaml.get(key) instanceof Number)) throw new IOException("Kayıt yeri sayısal alanı eksik: " + key);
        try {
            return new Point(UUID.fromString(yaml.getString("world-uuid", "")), yaml.getString("world-name"),
                    yaml.getDouble("x"), yaml.getDouble("y"), yaml.getDouble("z"),
                    (float) yaml.getDouble("yaw"), (float) yaml.getDouble("pitch"));
        } catch (IllegalArgumentException e) { throw new IOException("Geçersiz kayıt yeri", e); }
    }

    void save(Point point) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("enabled", point != null);
        if (point != null) {
            yaml.set("world-uuid", point.world().toString()); yaml.set("world-name", point.worldName());
            yaml.set("x", point.x()); yaml.set("y", point.y()); yaml.set("z", point.z());
            yaml.set("yaw", point.yaw()); yaml.set("pitch", point.pitch());
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), ".kayit-yeri-", ".tmp");
        try {
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
