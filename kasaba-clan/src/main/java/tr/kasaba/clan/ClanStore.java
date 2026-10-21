package tr.kasaba.clan;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ClanStore {
    private final Path path;
    ClanStore(Path path){this.path=path;}
    ClanState load() throws IOException {
        if(!Files.exists(path))return ClanState.empty();
        if(Files.size(path)>10_485_760)throw new IOException("Klan veri dosyası 10 MiB sınırını aşıyor.");
        YamlConfiguration yaml=new YamlConfiguration();
        try{yaml.load(path.toFile());}catch(InvalidConfigurationException e){throw new IOException("Klan YAML dosyası bozuk.",e);}
        try{
            if(number(yaml,"surum")!=1)throw new IllegalArgumentException("Desteklenmeyen klan veri sürümü.");
            UUID forest=null;String forestName=null;
            if(yaml.contains("orman")){ConfigurationSection section=section(yaml,"orman");forest=UUID.fromString(string(section,"kimlik"));forestName=string(section,"isim");}
            ConfigurationSection section=section(yaml,"klanlar");Map<UUID,Clan> clans=new HashMap<>();
            for(String key:section.getKeys(false)){
                UUID id=UUID.fromString(key);ConfigurationSection data=section(section,key);
                ConfigurationSection players=section(data,"uyeler");Map<String,String> members=new HashMap<>();
                for(String player:players.getKeys(false))members.put(player,string(players,player));
                Object ranks=data.get("yardimcilar");if(!(ranks instanceof List<?> list)||list.stream().anyMatch(v->!(v instanceof String)))throw new IllegalArgumentException("yardimcilar bir isim listesi olmalı.");
                Set<String> assistants=new HashSet<>();for(Object value:list)assistants.add(Names.key((String)value));
                ConfigurationSection areas=section(data,"araziler");List<Claim> claims=new ArrayList<>();
                for(String index:areas.getKeys(false)){
                    ConfigurationSection area=section(areas,index);
                    claims.add(new Claim(Integer.parseInt(index),UUID.fromString(string(area,"dunya")),new Rect(number(area,"min-x"),number(area,"min-z"),number(area,"max-x"),number(area,"max-z"))));
                }
                clans.put(id,new Clan(id,string(data,"isim"),string(data,"etiket"),string(data,"lider"),members,assistants,claims));
            }
            return new ClanState(forest,forestName,clans);
        }catch(IllegalArgumentException e){throw new IOException("Klan verisi geçersiz: "+e.getMessage(),e);}
    }
    static String string(ConfigurationSection section,String key){if(!section.isString(key))throw new IllegalArgumentException("Metin alanı eksik: "+key);return section.getString(key);}
    static int number(ConfigurationSection section,String key){if(!section.isInt(key))throw new IllegalArgumentException("Tam sayı alanı eksik: "+key);return section.getInt(key);}
    static ConfigurationSection section(ConfigurationSection section,String key){ConfigurationSection result=section.getConfigurationSection(key);if(result==null)throw new IllegalArgumentException("Tablo alanı eksik: "+key);return result;}
    void save(ClanState state) throws IOException {
        YamlConfiguration yaml=new YamlConfiguration();yaml.set("surum",1);
        if(state.forest()!=null){yaml.set("orman.kimlik",state.forest().toString());yaml.set("orman.isim",state.forestName());}
        ConfigurationSection section=yaml.createSection("klanlar");
        for(Clan clan:state.clans().values().stream().sorted(Comparator.comparing(c->c.id().toString())).toList()){
            ConfigurationSection data=section.createSection(clan.id().toString());
            data.set("isim",clan.name());data.set("etiket",clan.tag());data.set("lider",clan.leader());
            ConfigurationSection players=data.createSection("uyeler");new TreeMap<>(clan.members()).forEach(players::set);
            data.set("yardimcilar",clan.assistants().stream().sorted().toList());ConfigurationSection areas=data.createSection("araziler");
            for(Claim claim:clan.claims().stream().sorted(Comparator.comparingInt(Claim::number)).toList()){
                ConfigurationSection area=areas.createSection(Integer.toString(claim.number()));Rect r=claim.bounds();
                area.set("dunya",claim.world().toString());area.set("min-x",r.minX());area.set("min-z",r.minZ());area.set("max-x",r.maxX());area.set("max-z",r.maxZ());
            }
        }
        Files.createDirectories(path.getParent());Path temporary=Files.createTempFile(path.getParent(),".klanlar-",".tmp");
        try{
            Files.writeString(temporary,yaml.saveToString(),StandardCharsets.UTF_8);
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temporary);}
    }
}
