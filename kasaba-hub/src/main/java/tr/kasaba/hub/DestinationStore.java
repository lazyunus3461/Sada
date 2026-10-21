package tr.kasaba.hub;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import tr.kasaba.hub.api.GameInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Administrator-created game entrances; never stores arbitrary executable commands. */
final class DestinationStore {
    record Destination(String id, String name, LobbyStore.Point point) {
        Destination {
            new GameInfo(id, name, "Oyun giriş noktası", 1, 20);
            Objects.requireNonNull(point);
        }
    }
    private final Path path;
    DestinationStore(Path path) { this.path = path; }

    Map<String, Destination> load() throws IOException {
        if (!Files.exists(path)) return Map.of();
        YamlConfiguration yaml = new YamlConfiguration();
        try { yaml.load(path.toFile()); }
        catch (InvalidConfigurationException e) { throw new IOException("Oyun girişleri dosyası bozuk.", e); }
        ConfigurationSection section = yaml.getConfigurationSection("oyunlar");
        if (section == null) throw new IOException("oyunlar bölümü eksik.");
        Map<String, Destination> entries = new TreeMap<>();
        try {
            for (String id : section.getKeys(false)) {
                ConfigurationSection value = section.getConfigurationSection(id);
                if (value == null || !value.isString("ad") || !value.isString("dunya") || !value.isString("dunya-kimligi"))
                    throw new IllegalArgumentException("Eksik oyun bilgisi: " + id);
                for (String key : List.of("x", "y", "z", "yaw", "pitch"))
                    if (!(value.get(key) instanceof Number)) throw new IllegalArgumentException("Eksik konum: " + id);
                var point = new LobbyStore.Point(UUID.fromString(value.getString("dunya-kimligi")), value.getString("dunya"),
                        value.getDouble("x"), value.getDouble("y"), value.getDouble("z"),
                        (float)value.getDouble("yaw"), (float)value.getDouble("pitch"));
                entries.put(id, new Destination(id, value.getString("ad"), point));
            }
        } catch (IllegalArgumentException e) { throw new IOException("Geçersiz oyun girişi.", e); }
        return Map.copyOf(entries);
    }

    void save(Map<String, Destination> entries) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection section = yaml.createSection("oyunlar");
        for (var entry : new TreeMap<>(entries).entrySet()) {
            Destination d = entry.getValue();
            if (!entry.getKey().equals(d.id())) throw new IllegalArgumentException("Oyun kimliği uyuşmuyor.");
            var value = section.createSection(d.id()); var p = d.point();
            value.set("ad", d.name()); value.set("dunya", p.worldName()); value.set("dunya-kimligi", p.world().toString());
            value.set("x", p.x()); value.set("y", p.y()); value.set("z", p.z()); value.set("yaw", p.yaw()); value.set("pitch", p.pitch());
        }
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".oyunlar-", ".tmp");
        try {
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
