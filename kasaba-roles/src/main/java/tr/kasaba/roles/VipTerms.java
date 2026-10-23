package tr.kasaba.roles;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;

final class VipTerms {
    private final Path path;
    private final Map<UUID,Long> terms=new HashMap<>();
    VipTerms(Path path)throws IOException{this.path=path;if(Files.exists(path)){var yaml=RoleFiles.read(path);for(String key:yaml.getKeys(false)){try{long until=yaml.getLong(key);if(until<0)throw new IllegalArgumentException();terms.put(UUID.fromString(key),until);}catch(RuntimeException ex){throw new IOException("VIP süre dosyası bozuk; korundu.",ex);}}}}
    long until(UUID id){return terms.getOrDefault(id,0L);}
    void grant(UUID id,long until)throws IOException{if(until<=until(id))return;var next=new HashMap<>(terms);next.put(id,until);var yaml=new YamlConfiguration();next.forEach((key,value)->yaml.set(key.toString(),value));RoleFiles.write(path,yaml.saveToString());terms.clear();terms.putAll(next);}
}
