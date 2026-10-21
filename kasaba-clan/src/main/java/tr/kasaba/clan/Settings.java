package tr.kasaba.clan;

import net.kyori.adventure.text.format.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Locale;

record Settings(int members,int area,int claims,int side,int invitation,TextColor color) {
    static Settings read(Path path) throws IOException {
        YamlConfiguration yaml=new YamlConfiguration();
        try{yaml.load(path.toFile());}catch(InvalidConfigurationException e){throw new IOException("Klan config.yml bozuk.",e);}
        try{
            String key=yaml.getString("etiket-rengi","aqua").toLowerCase(Locale.ROOT);
            TextColor color=key.startsWith("#")?TextColor.fromHexString(key):NamedTextColor.NAMES.value(key);
            if(color==null)throw new IllegalArgumentException("etiket-rengi geçersiz. Örnek: aqua veya #55FFFF");
            return new Settings(integer(yaml,"uye-siniri",10,2,100),integer(yaml,"toplam-alan-siniri",5000,1,1_000_000),integer(yaml,"arazi-sayisi-siniri",20,1,200),integer(yaml,"kenar-siniri",1024,1,4096),integer(yaml,"davet-suresi-saniye",120,10,600),color);
        }catch(IllegalArgumentException e){throw new IOException("Klan ayarı geçersiz: "+e.getMessage(),e);}
    }
    private static int integer(YamlConfiguration yaml,String key,int fallback,int min,int max){
        int value=yaml.contains(key)?ClanStore.number(yaml,key):fallback;
        if(value<min||value>max)throw new IllegalArgumentException(key+" "+min+"–"+max+" arasında olmalı.");return value;
    }
}
