package tr.kasabatr.vampir;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/** Installs the enclosed village separately. Old worlds remain untouched; no old terrain or structures are imported. */
final class CleanWorldInstaller {
    private static final List<String> SEEDS=List.of("r.-1.-1.mca","r.-1.0.mca","r.0.-1.mca","r.0.0.mca","r.1.-1.mca","r.1.0.mca");
    private CleanWorldInstaller(){}
    static void ensure(Plugin plugin,Path worlds,FileConfiguration config)throws IOException{
        Path target=worlds.resolve(WorldNames.VILLAGE);
        if(Files.isRegularFile(target.resolve("level.dat")))return;
        if(Files.exists(target))throw new IOException("Yeni köy klasörü dolu fakat level.dat yok: "+target);
        Set<String> regions=new LinkedHashSet<>(SEEDS);
        Path staging=Files.createTempDirectory(worlds,".vk-village-");
        try{
            Files.createDirectories(staging.resolve("region"));Files.createDirectories(staging.resolve("data"));
            Files.write(staging.resolve("level.dat"),resource(plugin,"level.dat"));Files.write(staging.resolve("data/world_border.dat"),resource(plugin,"data/world_border.dat"));
            for(String name:regions){
                Files.write(staging.resolve("region").resolve(name),resource(plugin,"region/"+name));
            }
            Files.writeString(staging.resolve(".kasabavampir-three-villages"),"KasabaVampirKoylu 1.2.0\nonly-distributed-buildings=true\n");
            try{Files.move(staging,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(staging,target);}
            plugin.getLogger().info("Üç ayrı köy ve tek ortak lobi hazır: "+WorldNames.VILLAGE+". Önceki dünya dosyaları korundu.");
        }finally{if(Files.exists(staging))try(var files=Files.walk(staging)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    private static byte[] resource(Plugin plugin,String relative)throws IOException{try(InputStream in=plugin.getResource("map-template/"+relative)){if(in==null)throw new IOException("JAR içindeki köy haritası eksik: "+relative);return in.readAllBytes();}}
}
