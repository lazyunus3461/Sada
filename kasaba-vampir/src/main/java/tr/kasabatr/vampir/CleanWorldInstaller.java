package tr.kasabatr.vampir;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

/** Installs the enclosed village separately. Old worlds are read only; lobby edits survive. */
final class CleanWorldInstaller {
    private static final List<String> SEEDS=List.of("r.-1.-1.mca","r.-1.0.mca","r.0.-1.mca","r.0.0.mca");
    private CleanWorldInstaller(){}
    static void ensure(Plugin plugin,Path worlds,FileConfiguration config)throws IOException{
        Path target=worlds.resolve(WorldNames.VILLAGE);
        if(Files.isRegularFile(target.resolve("level.dat")))return;
        if(Files.exists(target))throw new IOException("Yeni köy klasörü dolu fakat level.dat yok: "+target);
        Path source=null;
        if(WorldNames.separateLobby(config)&&WorldNames.managed(config.getString("lobby.world"))){
            LinkedHashSet<String> names=new LinkedHashSet<>();names.add(config.getString("lobby.world"));names.addAll(List.of(WorldNames.CLEAN,WorldNames.CURRENT,WorldNames.LEGACY));
            for(String name:names)if(!WorldNames.VILLAGE.equals(name)&&Files.isRegularFile(worlds.resolve(name).resolve("level.dat"))){source=worlds.resolve(name);break;}
        }
        double x=config.getDouble("lobby.x",144.5),z=config.getDouble("lobby.z",.5);int radius=Math.clamp(config.getInt("lobby.radius",31),1,128)+16;
        int minX=(int)Math.floor((x-radius)/16),maxX=(int)Math.floor((x+radius)/16),minZ=(int)Math.floor((z-radius)/16),maxZ=(int)Math.floor((z+radius)/16);
        Set<String> regions=new LinkedHashSet<>(SEEDS);if(source!=null)for(int rx=minX>>5;rx<=(maxX>>5);rx++)for(int rz=minZ>>5;rz<=(maxZ>>5);rz++)regions.add("r."+rx+"."+rz+".mca");
        Path staging=Files.createTempDirectory(worlds,".vk-village-");
        try{
            Files.createDirectories(staging.resolve("region"));Files.createDirectories(staging.resolve("data"));
            Files.write(staging.resolve("level.dat"),resource(plugin,"level.dat"));Files.write(staging.resolve("data/world_border.dat"),resource(plugin,"data/world_border.dat"));
            for(String name:regions){
                String[] parts=name.split("\\.");int rx=Integer.parseInt(parts[1]),rz=Integer.parseInt(parts[2]);
                byte[] seed=SEEDS.contains(name)?resource(plugin,"region/"+name):new byte[8192];Path old=source==null?null:source.resolve("region").resolve(name);
                byte[] existing=old!=null&&Files.isRegularFile(old)?Files.readAllBytes(old):null;
                Files.write(staging.resolve("region").resolve(name),AnvilRegion.village(seed,existing,index->{int cx=rx*32+index%32,cz=rz*32+index/32;return cx>=minX&&cx<=maxX&&cz>=minZ&&cz<=maxZ&&(cx< -5||cx>4||cz< -5||cz>4);}));
            }
            Files.writeString(staging.resolve(".kasabavampir-village"),"KasabaVampirKoylu 1.1.0\nprevious-world="+(source==null?"none":source.getFileName())+"\n");
            try{Files.move(staging,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(staging,target);}
            plugin.getLogger().info("Dar sokaklı köy hazır: "+WorldNames.VILLAGE+". Önceki dünya dosyaları korundu.");
        }finally{if(Files.exists(staging))try(var files=Files.walk(staging)){for(Path p:files.sorted(Comparator.reverseOrder()).toList())Files.delete(p);}}
    }
    private static byte[] resource(Plugin plugin,String relative)throws IOException{try(InputStream in=plugin.getResource("map-template/"+relative)){if(in==null)throw new IOException("JAR içindeki köy haritası eksik: "+relative);return in.readAllBytes();}}
}
