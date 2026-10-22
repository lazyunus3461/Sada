package tr.kasabatr.islands;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import java.util.List;
final class OceanBiomeProvider extends BiomeProvider {
    @Override public Biome getBiome(WorldInfo world,int x,int y,int z){return Biome.OCEAN;}
    @Override public List<Biome> getBiomes(WorldInfo world){return List.of(Biome.OCEAN);}
}
