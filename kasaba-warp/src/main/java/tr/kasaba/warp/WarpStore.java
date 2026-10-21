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
                if(item.contains("simge")&&!item.isString("simge"))throw new IllegalArgumentException("simge kaydedilmiş eşya verisi olmalı: "+key);
                int slot=item.contains("menu-yeri")?MenuLayout.parse(item.getString("menu-yeri")):-1;
                Warp warp=new Warp(key,item.getString("isim",key),item.getString("aciklama",""),item.getBoolean("ozel",false),
                        UUID.fromString(item.getString("dunya-kimligi","")),item.getString("dunya"),
                        item.getDouble("x"),item.getDouble("y"),item.getDouble("z"),
                        (float)item.getDouble("yaw"),(float)item.getDouble("pitch"),item.getString("simge",""),slot,extraPoints(item));
                result.put(key,warp);
            }
            MenuLayout.validate(result.values());
        }catch(IllegalArgumentException e){throw new IOException("Warp verisi geçersiz: "+e.getMessage(),e);}
        return Collections.unmodifiableMap(result);
    }
    void save(Map<String,Warp> warps) throws IOException {
        MenuLayout.validate(warps.values());
        YamlConfiguration yaml=new YamlConfiguration();ConfigurationSection section=yaml.createSection("warps");
        for(Warp warp:warps.values()) {
            ConfigurationSection item=section.createSection(warp.id());
            item.set("isim",warp.name());item.set("aciklama",warp.description());
            item.set("ozel",warp.restricted());
            if(!warp.icon().isEmpty())item.set("simge",warp.icon());
            if(warp.menuSlot()>=0)item.set("menu-yeri",MenuLayout.label(warp.menuSlot()));
            item.set("dunya-kimligi",warp.world().toString());item.set("dunya",warp.worldName());
            item.set("x",warp.x());item.set("y",warp.y());item.set("z",warp.z());
            item.set("yaw",warp.yaw());item.set("pitch",warp.pitch());
            if(!warp.extraPoints().isEmpty())item.set("ek-noktalar",warp.extraPoints().stream().map(p->Map.of("x",p.x(),"y",p.y(),"z",p.z(),"yaw",p.yaw(),"pitch",p.pitch())).toList());
        }
        Files.createDirectories(path.getParent());
        Path temporary=Files.createTempFile(path.getParent(),".warps-",".tmp");
        try {
            Files.writeString(temporary,yaml.saveToString(),StandardCharsets.UTF_8);
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
    private List<WarpPoint> extraPoints(ConfigurationSection item){
        if(!item.contains("ek-noktalar"))return List.of();
        if(!(item.get("ek-noktalar") instanceof List<?> list))throw new IllegalArgumentException("ek-noktalar bir konum listesi olmalı.");
        List<WarpPoint> points=new ArrayList<>();
        for(Object value:list){
            if(!(value instanceof Map<?,?> map))throw new IllegalArgumentException("Ek nokta bir konum tablosu olmalı.");
            for(String key:List.of("x","y","z","yaw","pitch"))if(!(map.get(key) instanceof Number))throw new IllegalArgumentException("Ek nokta alanı eksik: "+key);
            points.add(new WarpPoint(((Number)map.get("x")).doubleValue(),((Number)map.get("y")).doubleValue(),((Number)map.get("z")).doubleValue(),((Number)map.get("yaw")).floatValue(),((Number)map.get("pitch")).floatValue()));
        }
        return points;
    }
}
