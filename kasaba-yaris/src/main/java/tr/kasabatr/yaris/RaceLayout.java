package tr.kasabatr.yaris;
import java.io.*;
import java.nio.file.*;
import org.bukkit.configuration.file.YamlConfiguration;

/** Switch only the managed default layout. Old world folders remain intact as backups. */
final class RaceLayout {
    static void prepare(KasabaYarisPlugin plugin)throws IOException{
        String old=plugin.getConfig().getString("template-world","");Path folder=plugin.getDataFolder().toPath(),mapFile=folder.resolve("race-map.yml");
        boolean migrate=plugin.getConfig().getInt("layout-version",0)<3&&(old.equals("KasabaTR-Yaris-Arenasi-v01")||old.equals("KasabaTR-Yaris-Arenasi-v02"));
        if(!migrate&&Files.exists(mapFile))return;
        if(!migrate&&!old.startsWith("KasabaTR-Yaris-Arenasi-v02"))return;
        String target="KasabaTR-Yaris-Arenasi-v02";Path world=org.bukkit.Bukkit.getWorldContainer().toPath().resolve(target);
        if(Files.exists(world)&&!Files.isRegularFile(world.resolve("race-layout.properties")))target+="_"+Long.toString(System.currentTimeMillis(),36);
        EmbeddedWorld.install(plugin,target);
        if(migrate)for(String name:new String[]{"config.yml","race-map.yml"}){Path source=folder.resolve(name),backup=folder.resolve(name+".before-spaced-v2");if(Files.exists(source)&&!Files.exists(backup))Files.copy(source,backup);}
        YamlConfiguration map;
        try(var input=new InputStreamReader(plugin.getResource("race-map-v2.yml"),java.nio.charset.StandardCharsets.UTF_8)){map=YamlConfiguration.loadConfiguration(input);}
        map.set("world",target);Path temp=folder.resolve("race-map.yml.new");map.save(temp.toFile());
        try{Files.move(temp,mapFile,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,mapFile,StandardCopyOption.REPLACE_EXISTING);}
        plugin.getConfig().set("template-world",target);plugin.getConfig().set("layout-version",3);plugin.getConfig().set("obstacles-version",3);plugin.saveConfig();
    }
}
