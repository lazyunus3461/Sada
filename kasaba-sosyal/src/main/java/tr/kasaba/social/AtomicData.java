package tr.kasaba.social;

import java.io.*;
import java.nio.file.*;
import java.util.function.Consumer;
import org.bukkit.configuration.file.YamlConfiguration;

final class AtomicData {
 private final Path file;private YamlConfiguration data;
 AtomicData(Path file)throws IOException{this.file=file;data=new YamlConfiguration();if(Files.exists(file))try{data.load(file.toFile());}catch(org.bukkit.configuration.InvalidConfigurationException e){throw new IOException("Sosyal veriler bozuk; dosya korundu.",e);}}
 YamlConfiguration get(){return data;}
 void update(Consumer<YamlConfiguration> change){try{var next=new YamlConfiguration();next.loadFromString(data.saveToString());change.accept(next);Files.createDirectories(file.toAbsolutePath().getParent());Path temp=Files.createTempFile(file.toAbsolutePath().getParent(),"social-",".tmp");try{Files.writeString(temp,next.saveToString());Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}finally{Files.deleteIfExists(temp);}data=next;}catch(Exception e){if(e instanceof IllegalArgumentException a)throw a;throw new IllegalStateException("Sosyal kayıt kaydedilemedi; işlem uygulanmadı.",e);}}
}
