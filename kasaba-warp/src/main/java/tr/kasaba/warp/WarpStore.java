package tr.kasaba.warp;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class WarpStore {
    private final Path path;
    WarpStore(Path path){this.path=path;}
    Map<String,Warp> load() throws IOException {
        if(!Files.exists(path))return Map.of();
        YamlConfiguration yaml=new YamlConfiguration();
        try{yaml.load(path.toFile());}
        catch(InvalidConfigurationException e){throw new IOException("Warp dosyası bozuk.",e);}
        ConfigurationSection section=yaml.getConfigurationSection("warps");
        if(section==null)throw new IOException("warps alanı bir nokta tablosu olmalı.");
        Map<String,Warp> result=new LinkedHashMap<>();
        try {
            for(String key:section.getKeys(false)) {
                if(!Warp.id(key).equals(key))throw new IllegalArgumentException("Dosyadaki warp kimliği standart biçimde olmalı: "+key);
                ConfigurationSection item=section.getConfigurationSection(key);
                if(item==null)throw new IllegalArgumentException("Geçersiz warp: "+key);
                for(String number:new String[]{"x","y","z","yaw","pitch"})
                    if(!(item.get(number) instanceof Number))throw new IllegalArgumentException("Konum alanı eksik: "+key+"."+number);
                if(item.contains("ozel")&&!item.isBoolean("ozel"))throw new IllegalArgumentException("ozel true/false olmalı: "+key);
                Warp warp=new Warp(key,item.getString("isim",key),item.getString("aciklama",""),item.getBoolean("ozel",false),
                        UUID.fromString(item.getString("dunya-kimligi","")),item.getString("dunya"),
                        item.getDouble("x"),item.getDouble("y"),item.getDouble("z"),
                        (float)item.getDouble("yaw"),(float)item.getDouble("pitch"));
                result.put(key,warp);
            }
        }catch(IllegalArgumentException e){throw new IOException("Warp verisi geçersiz: "+e.getMessage(),e);}
        return Collections.unmodifiableMap(result);
    }
    void save(Map<String,Warp> warps) throws IOException {
        YamlConfiguration yaml=new YamlConfiguration();ConfigurationSection section=yaml.createSection("warps");
        for(Warp warp:warps.values()) {
            ConfigurationSection item=section.createSection(warp.id());
            item.set("isim",warp.name());item.set("aciklama",warp.description());
            item.set("ozel",warp.restricted());
            item.set("dunya-kimligi",warp.world().toString());item.set("dunya",warp.worldName());
            item.set("x",warp.x());item.set("y",warp.y());item.set("z",warp.z());
            item.set("yaw",warp.yaw());item.set("pitch",warp.pitch());
        }
        Files.createDirectories(path.getParent());
        Path temporary=Files.createTempFile(path.getParent(),".warps-",".tmp");
        try {
            Files.writeString(temporary,yaml.saveToString(),StandardCharsets.UTF_8);
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
}
