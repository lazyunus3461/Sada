package tr.kasabatr.tac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.*;

/** Permanent waiting pavilion in its own void world, never copied into a match. */
final class LobbyWorld {
    static final String NAME = "KasabaTR-Tac-Lobi-v01";
    private static final String MARKER = ".kasaba-tac-lobi";
    private static final String READY = "KasabaTac separate lobby v1 ready\n";
    private LobbyWorld() {}

    static World open() throws IOException {
        Path folder = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize().resolve(NAME);
        Path marker = folder.resolve(MARKER);
        if (Files.exists(folder) && !Files.isRegularFile(marker))
            throw new IOException("Mevcut işaretsiz lobi dünyası korunuyor: " + NAME);
        if (!Files.exists(folder)) {
            Files.createDirectory(folder);
            Files.writeString(marker, "KasabaTac separate lobby v1 building\n");
        }
        String state = Files.readString(marker);
        if (!state.equals(READY) && !state.equals("KasabaTac separate lobby v1 building\n"))
            throw new IOException("Lobi işareti okunamadı; mevcut dünya korunuyor.");
        World world = Bukkit.getWorld(NAME);
        if (world == null) world = Bukkit.createWorld(new WorldCreator(NAME)
            .type(WorldType.FLAT)
            .generatorSettings("{\"layers\":[],\"biome\":\"minecraft:the_void\",\"structure_overrides\":[]}")
            .generator(new EmptyWorldGenerator()).generateStructures(false));
        if (world == null) throw new IOException("Ayrı lobi dünyası açılamadı.");
        if (!state.equals(READY)) {
            build(world);
            world.save(true);
            Files.writeString(marker, READY);
        }
        world.setSpawnLocation(spawn(world));
        if (!world.getBlockAt(0, 80, 10).getType().isSolid()
            || !world.getBlockAt(0, 81, 10).isPassable()
            || !world.getBlockAt(0, 82, 10).isPassable())
            throw new IOException("Ayrı lobinin doğma noktası kapalı; dünya korunuyor.");
        return world;
    }

    static Location spawn(World world) { return new Location(world, .5, 81, 10.5, 180, 0); }

    static boolean contains(Location at, World world) {
        return at.getWorld() == world && at.getX() >= -19 && at.getX() < 20
            && at.getZ() >= -15 && at.getZ() < 16 && at.getY() >= 80.75 && at.getY() < 89;
    }

    private static void put(World world, int x, int y, int z, Material type) {
        world.getBlockAt(x, y, z).setType(type, false);
    }

    private static void build(World world) {
        // A solid base and continuous windowed walls keep the waiting area safe.
        for (int x = -20; x <= 20; x++) for (int z = -16; z <= 16; z++) {
            boolean edge = Math.abs(x) == 20 || Math.abs(z) == 16;
            put(world, x, 79, z, Material.STONE_BRICKS);
            Material floor = Math.abs(x) <= 2 ? Material.PURPLE_TERRACOTTA : Material.POLISHED_ANDESITE;
            if ((x + z) % 8 == 0 && !edge) floor = Material.SEA_LANTERN;
            put(world, x, 80, z, edge ? Material.DEEPSLATE_TILES : floor);
            if (edge) for (int y = 81; y <= 88; y++) {
                boolean pillar = Math.abs(x) == 20 && z % 8 == 0 || Math.abs(z) == 16 && x % 5 == 0;
                put(world, x, y, z, y == 81 || y == 88 || pillar ? Material.STONE_BRICKS : Material.PURPLE_STAINED_GLASS);
            }
            put(world, x, 89, z, edge ? Material.DEEPSLATE_TILES : Material.TINTED_GLASS);
        }
        // Crown throne colours suggest the game without exposing any playable layout.
        for (int x : new int[]{-15, 15}) for (int z : new int[]{-10, 0, 10}) {
            put(world, x, 81, z, Material.CHISELED_STONE_BRICKS);
            put(world, x, 82, z, z == 0 ? Material.GOLD_BLOCK : x < 0 ? Material.PURPLE_WOOL : Material.YELLOW_WOOL);
            put(world, x, 83, z, Material.LANTERN);
        }
        for (int x : new int[]{-10, 10}) {
            for (int z = 5; z <= 10; z++) put(world, x, 81, z, Material.OAK_SLAB);
            put(world, x, 81, -10, Material.FLOWERING_AZALEA);
            put(world, x, 82, -10, Material.LANTERN);
        }
    }
}
