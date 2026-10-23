package tr.kasabatr.vampir;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;

/** Binds the compass and match source to a real, populated village world. */
final class VillageWorlds {
    private final KasabaVampirPlugin plugin;
    final MultiverseBridge multiverse;
    VillageWorlds(KasabaVampirPlugin plugin){this.plugin=plugin;multiverse=new MultiverseBridge(plugin);}

    static boolean safe(Location point){
        return point!=null&&point.getWorld()!=null&&point.getY()>=point.getWorld().getMinHeight()+1
            &&point.getY()+1<point.getWorld().getMaxHeight()&&point.getWorld().getWorldBorder().isInside(point)
            &&point.getBlock().isPassable()&&point.clone().add(0,1,0).getBlock().isPassable()
            &&point.clone().add(0,-1,0).getBlock().getType().isSolid();
    }

    static boolean populated(World world){
        if(world==null||world.getName().startsWith("vk_room_"))return false;
        for(int origin:new int[]{0,320,640}){
            if(world.getBlockAt(origin,70,0).getType()!=Material.BELL)return false;
            for(int[] offset:new int[][]{{8,0},{0,8},{-7,0},{0,-7}})
                if(!world.getBlockAt(origin+offset[0],68,offset[1]).getType().isSolid())return false;
        }
        return safe(sharedSpawn(world));
    }

    static Location sharedSpawn(World world){return new Location(world,144.5,69,.5,180,0);}

    void startup()throws IOException{
        var config=plugin.getConfig();String name=config.getString("template-world","");
        World selected=Bukkit.getWorld(name);
        if(!populated(selected)){
            List<World> candidates=Bukkit.getWorlds().stream().filter(VillageWorlds::populated).toList();
            if(candidates.size()==1)selected=candidates.getFirst();
            else if(WorldNames.managed(name)||name.startsWith(WorldNames.VILLAGE+"_onarim")){
                // Do not overwrite or unload an existing world, even when its lobby looks empty.
                String repair=WorldNames.VILLAGE+"_onarim";int suffix=2;
                Path container=Bukkit.getWorldContainer().toPath();
                while(Files.exists(container.resolve(repair))||Bukkit.getWorld(repair)!=null)repair=WorldNames.VILLAGE+"_onarim_"+suffix++;
                CleanWorldInstaller.install(plugin,container,repair);
                selected=Bukkit.createWorld(new WorldCreator(repair).generator(new EmptyWorldGenerator()).generateStructures(false));
                if(!populated(selected))throw new IOException("Hazır köy zemini doğrulanamadı.");
            }else return;
            bind(selected);
            plugin.getLogger().info("Pusula ve maç haritası dolu dünyaya bağlandı: "+selected.getName());
        }else if(!safe(plugin.lobbySpawn()))bind(selected);
        multiverse.register(selected,plugin.lobbySpawn());
        Location lobby=plugin.lobbySpawn();if(lobby!=null&&lobby.getWorld()!=selected)multiverse.register(lobby.getWorld(),lobby);
    }

    void bind(World world)throws IOException{
        if(!populated(world))throw new IllegalArgumentException("Bu dünyada üç köy ve ortak lobi bulunamadı. Dolu üç köylü haritada kullan.");
        var config=plugin.getConfig();Path file=plugin.getDataFolder().toPath().resolve("config.yml");
        Path backup=plugin.getDataFolder().toPath().resolve("config-before-world-link-1.2.1.yml");
        if(Files.isRegularFile(file)&&!Files.exists(backup))Files.copy(file,backup);
        config.set("template-world",world.getName());config.set("lobby.world",world.getName());config.set("world-link.world",world.getName());
        config.set("lobby.x",144.5);config.set("lobby.y",69.0);config.set("lobby.z",.5);config.set("lobby.yaw",180.0);config.set("lobby.pitch",0.0);
        config.save(file.toFile());world.setSpawnLocation(sharedSpawn(world));world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);world.setTime(1000);world.setStorm(false);
        multiverse.register(world,sharedSpawn(world));
    }
}
