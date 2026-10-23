package tr.kasabatr.vampir;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.Plugin;
import tr.kasabatr.vampir.MapLayout;

final class WorldInstances {
    private static final String PREFIX = "vk_room_";
    private static final String MARKER = ".kasabavampir-instance";
    private final Plugin plugin;

    WorldInstances(Plugin plugin) {
        this.plugin = plugin;
    }

    boolean templateReady() {
        try {
            return Files.isRegularFile(this.template().resolve("level.dat"), new LinkOption[0]);
        }
        catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
    }

    World create(int n) throws IOException {
        final Path path = this.template();
        if (!Files.isRegularFile(path.resolve("level.dat"), new LinkOption[0])) {
            throw new IOException("Harita şablonu yok: " + String.valueOf(path));
        }
        World source=Bukkit.getWorld(path.getFileName().toString());if(source!=null)source.save();
        String string = PREFIX + n + "_" + UUID.randomUUID().toString().substring(0, 8);
        final Path path2 = this.path(string);
        Files.createDirectory(path2, new FileAttribute[0]);
        try {
            Files.walkFileTree(path, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

                @Override
                public FileVisitResult preVisitDirectory(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                    Path path22 = path.relativize(path3);
                    if (!path22.toString().isEmpty() && (path22.getName(0).toString().equals("playerdata") || path22.getName(0).toString().equals("stats") || path22.getName(0).toString().equals("advancements"))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Files.createDirectories(path2.resolve(path22), new FileAttribute[0]);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path3, BasicFileAttributes basicFileAttributes) throws IOException {
                    String string = path3.getFileName().toString();
                    if (!(string.equals("uid.dat") || string.equals("session.lock") || string.equals(WorldInstances.MARKER))) {
                        Files.copy(path3, path2.resolve(path.relativize(path3)), new CopyOption[0]);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            Files.writeString(path2.resolve(MARKER), (CharSequence)("KasabaVampirKoylu " + n), new OpenOption[0]);
            World world = Bukkit.createWorld(new WorldCreator(string));
            if (world == null) {
                throw new IOException("Paper dünya kopyasını açamadı: " + string);
            }
            world.setAutoSave(false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);
            world.setGameRule(GameRule.LOCATOR_BAR,false);
            world.setStorm(false);world.setThundering(false);
            world.setTime(1000L);
            world.getWorldBorder().setCenter(0.5, 0.5);
            world.getWorldBorder().setSize(137.0);
            world.setSpawnLocation(MapLayout.seat(world, 0));
            return world;
        }
        catch (Exception exception) {
            if (Bukkit.getWorld(string) != null) {
                Bukkit.unloadWorld(string, false);
            }
            this.deleteTree(path2);
            if (exception instanceof IOException) {
                IOException iOException = (IOException)exception;
                throw iOException;
            }
            throw new IOException("Dünya açılamadı: " + string, exception);
        }
    }

    void close(World world) {
        if (world == null || !world.getName().startsWith(PREFIX)) {
            return;
        }
        String string = world.getName();
        if (!Bukkit.unloadWorld(world, false)) {
            this.plugin.getLogger().warning("Dünya kullanımda; silinmedi: " + string);
            return;
        }
        try {
            Path path = this.path(string);
            if (Files.isRegularFile(path.resolve(MARKER), new LinkOption[0])) {
                this.deleteTree(path);
            }
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Geçici dünya silinemedi: " + string + " " + iOException.getMessage());
        }
    }

    void clearStaleCopies() {
        Path path2 = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        try (Stream<Path> stream = Files.list(path2);){
            for (Path path3 : stream.filter(path -> Files.isDirectory(path, new LinkOption[0])).toList()) {
                String string = path3.getFileName().toString();
                if (!string.startsWith(PREFIX) || Bukkit.getWorld(string) != null || !Files.isRegularFile(path3.resolve(MARKER), new LinkOption[0])) continue;
                try {
                    this.deleteTree(path3);
                }
                catch (IOException iOException) {
                    this.plugin.getLogger().warning("Eski oda silinemedi: " + string);
                }
            }
        }
        catch (IOException iOException) {
            this.plugin.getLogger().warning("Eski odalar denetlenemedi: " + iOException.getMessage());
        }
    }

    private Path template() {
        String string = this.plugin.getConfig().getString("template-world", "");
        if (!string.matches("[A-Za-z0-9_-]{1,80}") || string.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Geçersiz template-world adı");
        }
        return this.path(string);
    }

    private Path path(String string) {
        Path path = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path path2 = path.resolve(string).normalize();
        if (!path2.getParent().equals(path)) {
            throw new IllegalArgumentException("Dünya yolu dışarı taşamaz");
        }
        return path2;
    }

    private void deleteTree(Path path) throws IOException {
        Path path2 = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path path3 = path.toAbsolutePath().normalize();
        if (!path3.getParent().equals(path2) || !path3.getFileName().toString().startsWith(PREFIX)) {
            throw new IOException("Geçici dünya sınırı dışındaki dizin silinemez: " + String.valueOf(path3));
        }
        Files.walkFileTree(path3, (FileVisitor<? super Path>)new SimpleFileVisitor<Path>(){

            @Override
            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path path, IOException iOException) throws IOException {
                if (iOException != null) {
                    throw iOException;
                }
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
