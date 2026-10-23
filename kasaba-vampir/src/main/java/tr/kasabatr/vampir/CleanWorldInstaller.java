package tr.kasabatr.vampir;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.plugin.Plugin;

/** Creates a separate clean world; the original world is never overwritten. */
final class CleanWorldInstaller {
    private static final String[] REGIONS={"r.-1.-1.mca","r.-1.0.mca","r.0.-1.mca","r.0.0.mca"};
    private CleanWorldInstaller(){}
    static void ensure(Plugin plugin,Path worlds,String sourceName)throws IOException{
        Path target=worlds.resolve(WorldNames.CLEAN);
        if(Files.isRegularFile(target.resolve("level.dat")))return;
        if(Files.exists(target))throw new IOException("Temiz dünya klasörü dolu fakat level.dat yok: "+target);
        Path source=WorldNames.managed(sourceName)&&!WorldNames.CLEAN.equals(sourceName)?worlds.resolve(sourceName):null;
        boolean copy=source!=null&&Files.isRegularFile(source.resolve("level.dat"));
        Path staging=Files.createTempDirectory(worlds,".vk-clean-");
        try{
            Files.createDirectories(staging.resolve("region"));Files.createDirectories(staging.resolve("data"));
            Files.write(staging.resolve("level.dat"),copy?Files.readAllBytes(source.resolve("level.dat")):resource(plugin,"level.dat"));
            Path border=copy?source.resolve("data/world_border.dat"):null;Files.write(staging.resolve("data/world_border.dat"),border!=null&&Files.isRegularFile(border)?Files.readAllBytes(border):resource(plugin,"data/world_border.dat"));
            int preserved=0;
            for(String name:REGIONS){byte[] seed=resource(plugin,"region/"+name);Path old=copy?source.resolve("region").resolve(name):null;byte[] existing=old!=null&&Files.isRegularFile(old)?Files.readAllBytes(old):null;if(existing!=null)preserved++;Files.write(staging.resolve("region").resolve(name),AnvilRegion.retain(seed,existing));}
            Files.writeString(staging.resolve(".kasabavampir-clean"),"KasabaVampirKoylu 1.0.3\nsource="+sourceName+"\n");
            try{Files.move(staging,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(staging,target);}
            plugin.getLogger().info("Boş dünya hazır: "+WorldNames.CLEAN+". Harita bölgesi korundu ("+preserved+" bölge); eski dünya klasörü yerinde.");
        }finally{if(Files.exists(staging))try(var files=Files.walk(staging)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    private static byte[] resource(Plugin plugin,String relative)throws IOException{try(InputStream in=plugin.getResource("map-template/"+relative)){if(in==null)throw new IOException("JAR içindeki harita eksik: "+relative);return in.readAllBytes();}}
}
