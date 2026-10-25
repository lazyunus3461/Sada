package tr.kasabatr.tac;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Install a bundled template only if its directory is absent. Never overlay a user's world. */
final class MapInstaller {
    static final List<String> DEFAULTS=List.of("KasabaTR-Lanetli-Tac-Satosu-v01");
    private static final List<String> FILES=List.of("level.dat","region/r.-1.-1.mca","region/r.-1.0.mca","region/r.0.-1.mca","region/r.0.0.mca");
    static void ensure(Plugin plugin,String name)throws IOException{
        if(!name.matches("[A-Za-z0-9_-]{1,80}")||name.startsWith("tac_room_"))throw new IOException("Geçersiz harita adı.");
        Path root=Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize(),target=root.resolve(name);
        if(Files.exists(target)){
            if(!Files.isRegularFile(target.resolve("level.dat")))throw new IOException("Mevcut dünya eksik; üzerine yazılmadı: "+name);
            return;
        }
        if(!DEFAULTS.contains(name))throw new IOException("Özel harita sunucu kökünde bulunamadı: "+name);
        Path stage=root.resolve(".kasaba-tac-install-"+UUID.randomUUID());
        try{
            List<String> files=new ArrayList<>(FILES);files.add("crown-map.json");
            for(String file:files){Path output=stage.resolve(file);Files.createDirectories(output.getParent());try(InputStream in=plugin.getResource("map-template/"+name+"/"+file)){if(in==null)throw new IOException("JAR içinde harita eksik: "+file);Files.copy(in,output);}}
            try{Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(stage,target);}
        }finally{if(Files.exists(stage))try(var paths=Files.walk(stage)){for(Path path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
    }
}
