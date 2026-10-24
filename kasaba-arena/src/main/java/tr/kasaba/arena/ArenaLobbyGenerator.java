package tr.kasaba.arena;
import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;
import java.util.Random;

/** Fixed spawn avoids searching thousands of empty chunks for a natural surface. */
public final class ArenaLobbyGenerator extends ChunkGenerator {
    @Override public Location getFixedSpawnLocation(World world,Random random){return new Location(world,0.5,80,0.5);}
    @Override public boolean shouldGenerateNoise(){return false;}
    @Override public boolean shouldGenerateSurface(){return false;}
    @Override public boolean shouldGenerateBedrock(){return false;}
    @Override public boolean shouldGenerateCaves(){return false;}
    @Override public boolean shouldGenerateDecorations(){return false;}
    @Override public boolean shouldGenerateMobs(){return false;}
    @Override public boolean shouldGenerateStructures(){return false;}
}
