package tr.kasaba.roles;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

final class RoleFiles {
    record Configuration(RoleCatalog catalog, boolean manageTab, YamlConfiguration yaml) {}
    static YamlConfiguration read(Path path) throws IOException {
        YamlConfiguration yaml=new YamlConfiguration();
        try { yaml.load(path.toFile()); }
        catch(InvalidConfigurationException e) { throw new IOException("YAML okunamadı: "+path.getFileName(),e); }
        return yaml;
    }
    static Configuration configuration(YamlConfiguration yaml) {
        if(yaml.contains("manage-tab")&&!yaml.isBoolean("manage-tab"))throw new IllegalArgumentException("manage-tab true/false olmalı");
        ConfigurationSection section=yaml.getConfigurationSection("roles");
        if(section==null || section.getKeys(false).isEmpty())throw new IllegalArgumentException("En az bir rol tanımlanmalı");
        if(section.getKeys(false).size()>100)throw new IllegalArgumentException("En fazla 100 rol tanımlanabilir");
        Map<String,Role> roles=new LinkedHashMap<>();
        for(String key:section.getKeys(false)) {
            String id=Role.normalizeId(key);
            if(!id.equals(key))throw new IllegalArgumentException("Rol kimliklerini küçük harfle yaz: "+key);
            ConfigurationSection r=section.getConfigurationSection(key);
            if(r==null)throw new IllegalArgumentException("Geçersiz rol: "+key);
            Map<String,Boolean> permissions=new LinkedHashMap<>();
            ConfigurationSection p=r.getConfigurationSection("permissions");
            if(r.contains("permissions")&&p==null)throw new IllegalArgumentException("Yetkiler bir liste değil, düğüm/değer tablosu olmalı: "+key);
            if(p!=null)for(String node:p.getKeys(true)) {
                if(p.isConfigurationSection(node))continue;
                if(!p.isBoolean(node))throw new IllegalArgumentException("Yetki true/false olmalı: "+node);
                permissions.put(Role.permissionNode(node),p.getBoolean(node));
            }
            String color=r.getString("color","gray"), nameColor=r.getString("name-color","white");
            KasabaRoles.color(color);KasabaRoles.color(nameColor);
            if(r.contains("inherits")&&(!r.isList("inherits")||r.getList("inherits").stream().anyMatch(v->!(v instanceof String))))
                throw new IllegalArgumentException("inherits rol kimliklerinden oluşan bir liste olmalı: "+key);
            roles.put(id,new Role(id,r.getString("label",id),color,nameColor,r.getInt("priority",1),r.getStringList("inherits"),permissions));
        }
        return new Configuration(new RoleCatalog(roles,yaml.getString("default-role","oyuncu")),yaml.getBoolean("manage-tab",true),yaml);
    }
    static Map<String,String> assignments(YamlConfiguration yaml, RoleCatalog roles) {
        Map<String,String> result=new LinkedHashMap<>();
        ConfigurationSection players=yaml.getConfigurationSection("players");
        if(yaml.contains("players")&&players==null)throw new IllegalArgumentException("players bir oyuncu/rol tablosu olmalı");
        if(players!=null)for(String name:players.getKeys(false)) {
            if(!players.isString(name))throw new IllegalArgumentException("Oyuncu rolü metin olmalı: "+name);
            String role=Role.normalizeId(players.getString(name));
            if(roles.get(role)==null)throw new IllegalArgumentException("Oyuncuya atanmış rol bulunamadı: "+role);
            if(result.put(Role.playerKey(name),role)!=null)throw new IllegalArgumentException("Aynı oyuncu iki kez yazılmış: "+name);
        }
        return result;
    }
    static void writeAssignments(Path path,Map<String,String> assignments) throws IOException {
        YamlConfiguration yaml=new YamlConfiguration();
        yaml.createSection("players",assignments);
        write(path,yaml.saveToString());
    }
    static void write(Path path,String text) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp=Files.createTempFile(path.getParent(),".roles-",".tmp");
        try {
            Files.writeString(temp,text,StandardCharsets.UTF_8);
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
