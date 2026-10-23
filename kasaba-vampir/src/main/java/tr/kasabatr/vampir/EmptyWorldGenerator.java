package tr.kasabatr.vampir;

import org.bukkit.generator.ChunkGenerator;

/** Every new chunk outside the supplied village is empty, including structures. */
public final class EmptyWorldGenerator extends ChunkGenerator {
    @Override public boolean shouldGenerateNoise(){return false;}
    @Override public boolean shouldGenerateSurface(){return false;}
    @Override public boolean shouldGenerateBedrock(){return false;}
    @Override public boolean shouldGenerateCaves(){return false;}
    @Override public boolean shouldGenerateDecorations(){return false;}
    @Override public boolean shouldGenerateMobs(){return false;}
    @Override public boolean shouldGenerateStructures(){return false;}
}
