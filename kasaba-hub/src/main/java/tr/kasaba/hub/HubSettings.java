package tr.kasaba.hub;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import java.io.IOException;
import java.nio.file.Path;

record HubSettings(int capacity,int invitation,int radius,boolean autoPage){
    static HubSettings read(Path path) throws IOException{
        YamlConfiguration y=new YamlConfiguration();try{y.load(path.toFile());}catch(InvalidConfigurationException e){throw new IOException("Hub config.yml bozuk.",e);}
        if(y.contains("otomatik-sayfa")&&!y.isBoolean("otomatik-sayfa"))throw new IOException("otomatik-sayfa true/false olmalı.");
        return new HubSettings(number(y,"grup-uye-siniri",8,2,20),number(y,"davet-suresi-saniye",60,10,600),number(y,"lobi-koruma-yaricapi",100,8,512),y.getBoolean("otomatik-sayfa",true));
    }
    private static int number(YamlConfiguration y,String key,int fallback,int min,int max) throws IOException{if(y.contains(key)&&!y.isInt(key))throw new IOException(key+" tam sayı olmalı.");int n=y.getInt(key,fallback);if(n<min||n>max)throw new IOException(key+" "+min+"–"+max+" arasında olmalı.");return n;}
}
