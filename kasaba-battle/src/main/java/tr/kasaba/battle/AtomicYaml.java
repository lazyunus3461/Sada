package tr.kasaba.battle;
import java.io.IOException;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;

final class AtomicYaml {
    static YamlConfiguration read(Path path)throws IOException{
        YamlConfiguration data=new YamlConfiguration();try{if(Files.exists(path))data.load(path.toFile());}catch(Exception e){throw new IOException("Dosya okunamadı; korundu: "+path.getFileName(),e);}return data;
    }
    static void write(Path path,YamlConfiguration data)throws IOException{
        Files.createDirectories(path.getParent());Path temp=Files.createTempFile(path.getParent(),"br-",".tmp");try{Files.writeString(temp,data.saveToString());try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
}
