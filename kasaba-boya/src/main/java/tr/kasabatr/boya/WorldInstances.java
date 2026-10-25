package tr.kasabatr.boya;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;

/** Each live room receives a separate copy of the supplied world. */
final class WorldInstances {
    private static final String PREFIX = "boya_room_";
    private static final String MARKER = ".kasababoya-instance";
    private final Plugin plugin;

    WorldInstances(Plugin plugin) { this.plugin = plugin; }

    boolean templateReady(PaintMap map) {
        try { return Files.isRegularFile(template(map.worldName).resolve("level.dat")); }
        catch (IllegalArgumentException error) { return false; }
    }

    World create(int roomId, PaintMap map) throws IOException {
        Path source = template(map.worldName);
        World templateWorld=Bukkit.getWorld(map.worldName);if(templateWorld!=null)templateWorld.save(true);
        if (!Files.isRegularFile(source.resolve("level.dat"))) {
            throw new IOException("Harita şablonu yok: " + source);
        }
        String name = PREFIX + roomId + "_" + UUID.randomUUID().toString().substring(0, 8);
        Path destination = path(name);
        Files.createDirectory(destination);
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relative = source.relativize(dir);
                    if (!relative.toString().isEmpty() && (relative.getName(0).toString().equals("entities") || relative.getName(0).toString().equals("playerdata")
                            || relative.getName(0).toString().equals("stats")
                            || relative.getName(0).toString().equals("advancements"))) return FileVisitResult.SKIP_SUBTREE;
                    Files.createDirectories(destination.resolve(relative));
                    return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String filename = file.getFileName().toString();
                    if (!filename.equals("uid.dat") && !filename.equals("session.lock") && !filename.equals(MARKER)) {
                        Files.copy(file, destination.resolve(source.relativize(file)));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            Files.writeString(destination.resolve(MARKER), "KasabaBoya " + roomId);
            World world = Bukkit.createWorld(new WorldCreator(name).generator(new EmptyWorldGenerator()).generateStructures(false));
            if (world == null) throw new IOException("Paper dünya kopyasını açamadı: " + name);
            world.setAutoSave(false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.LOCATOR_BAR,false);world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);world.setStorm(false);world.setThundering(false);
            world.setTime(1000L);
            world.getWorldBorder().setCenter(0.5, 0.5);
            world.getWorldBorder().setSize(1200);
            world.setSpawnLocation(map.lobby.at(world));
            return world;
        } catch (Exception error) {
            if (Bukkit.getWorld(name) != null) Bukkit.unloadWorld(name, false);
            deleteTree(destination);
            if (error instanceof IOException io) throw io;
            throw new IOException("Dünya açılamadı: " + name, error);
        }
    }

    void close(World world) {
        if (world == null || !world.getName().startsWith(PREFIX)) return;
        String name = world.getName();
        if (!Bukkit.unloadWorld(world, false)) {
            plugin.getLogger().warning("Dünya kullanımda; silinmedi: " + name);
            return;
        }
        try {
            Path path = path(name);
            if (Files.isRegularFile(path.resolve(MARKER))) deleteTree(path);
        } catch (IOException error) {
            plugin.getLogger().warning("Geçici dünya silinemedi: " + name + " " + error.getMessage());
        }
    }

    void clearStaleCopies() {
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        try (var paths = Files.list(container)) {
            for (Path candidate : paths.filter(Files::isDirectory).toList()) {
                String name = candidate.getFileName().toString();
                if (!name.startsWith(PREFIX) || Bukkit.getWorld(name) != null
                        || !Files.isRegularFile(candidate.resolve(MARKER))) continue;
                try { deleteTree(candidate); }
                catch (IOException error) { plugin.getLogger().warning("Eski oda silinemedi: " + name); }
            }
        } catch (IOException error) {
            plugin.getLogger().warning("Eski odalar denetlenemedi: " + error.getMessage());
        }
    }

    private Path template(String name) {
        if (!name.matches("[A-Za-z0-9_-]{1,80}") || name.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Geçersiz template-world adı");
        }
        return path(name);
    }

    private Path path(String name) {
        Path root = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path path = root.resolve(name).normalize();
        if (!path.getParent().equals(root)) throw new IllegalArgumentException("Dünya yolu dışarı taşamaz");
        return path;
    }

    private void deleteTree(Path root) throws IOException {
        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path absolute = root.toAbsolutePath().normalize();
        if (!absolute.getParent().equals(container) || !absolute.getFileName().toString().startsWith(PREFIX)) {
            throw new IOException("Geçici dünya sınırı dışındaki dizin silinemez: " + absolute);
        }
        Files.walkFileTree(absolute, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(dir); return FileVisitResult.CONTINUE;
            }
        });
    }
}
