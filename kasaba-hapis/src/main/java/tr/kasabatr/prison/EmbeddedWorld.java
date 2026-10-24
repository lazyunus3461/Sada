package tr.kasabatr.prison;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Installs only into a missing world directory; the upload is embedded unchanged. */
final class EmbeddedWorld {
    static void install(Plugin plugin,String name)throws IOException{
        if(!name.matches("[A-Za-z0-9_-]{1,80}"))throw new IOException("Geçersiz dünya adı.");
        Path target=Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize().resolve(name);
        if(Files.exists(target)){if(!Files.isRegularFile(target.resolve("level.dat")))throw new IOException("Mevcut dünya eksik; üzerine yazılmadı: "+name);return;}
        Path stage=target.resolveSibling(".kasaba-install-"+UUID.randomUUID());
        try(InputStream manifest=plugin.getResource("world-assets.txt")){
            if(manifest==null)throw new IOException("Dünya listesi eksik.");for(String row:new String(manifest.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R")){
                if(row.isBlank())continue;Path file=stage.resolve(row).normalize();if(!file.startsWith(stage)||Path.of(row).isAbsolute())throw new IOException("Geçersiz harita dosyası.");Files.createDirectories(file.getParent());try(InputStream in=plugin.getResource("map-template/"+row)){if(in==null)throw new IOException("Harita eksik: "+row);Files.copy(in,file);}
            }
            try{Files.move(stage,target,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(stage,target);}
        }finally{if(Files.exists(stage))try(var stream=Files.walk(stage)){for(Path file:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(file);}}
    }
}
