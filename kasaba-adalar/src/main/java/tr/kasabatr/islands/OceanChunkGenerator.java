package tr.kasabatr.islands;

import org.bukkit.Material;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import java.util.Random;

/** Continuous ocean floor. No air cavity between the bedrock and seabed. */
final class OceanChunkGenerator extends ChunkGenerator {
    @Override public void generateNoise(WorldInfo world,Random random,int x,int z,ChunkData data){int bottom=world.getMinHeight();data.setRegion(0,bottom,0,16,bottom+1,16,Material.BEDROCK);data.setRegion(0,bottom+1,0,16,58,16,Material.STONE);data.setRegion(0,58,0,16,59,16,Material.GRAVEL);data.setRegion(0,59,0,16,64,16,Material.WATER);}
    @Override public boolean shouldGenerateNoise(){return false;}
    @Override public boolean shouldGenerateSurface(){return false;}
    @Override public boolean shouldGenerateBedrock(){return false;}
    @Override public boolean shouldGenerateCaves(){return false;}
    @Override public boolean shouldGenerateDecorations(){return false;}
    @Override public boolean shouldGenerateStructures(){return false;}
}
