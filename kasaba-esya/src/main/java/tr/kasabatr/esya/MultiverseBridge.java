package tr.kasabatr.esya;

import java.lang.reflect.*;
import java.util.logging.Level;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;

/** Optional public-world registration. Match copies stay private and temporary. */
final class MultiverseBridge {
    private final Plugin owner;
    MultiverseBridge(Plugin owner){this.owner=owner;}

    boolean registered(World world){
        Plugin mv=Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if(mv==null||!mv.isEnabled()||world==null)return false;
        try{Object manager=manager(mv);return (Boolean)manager.getClass().getMethod("isWorld",String.class).invoke(manager,world.getName());}
        catch(ReflectiveOperationException e){return false;}
    }

    void register(World world,Location spawn){
        Plugin mv=Bukkit.getPluginManager().getPlugin("Multiverse-Core");
        if(mv==null||!mv.isEnabled()||world==null||world.getName().startsWith("esya_room_"))return;
        try{
            Object manager=manager(mv);
            if(!(Boolean)manager.getClass().getMethod("isWorld",String.class).invoke(manager,world.getName())){
                ClassLoader loader=mv.getClass().getClassLoader();
                Class<?> options=Class.forName("org.mvplugins.multiverse.core.world.options.ImportWorldOptions",true,loader);
                Object request=options.getMethod("worldName",String.class).invoke(null,world.getName());
                options.getMethod("environment",World.Environment.class).invoke(request,world.getEnvironment());
                options.getMethod("generator",String.class).invoke(request,owner.getName());
                options.getMethod("useSpawnAdjust",boolean.class).invoke(request,false);
                Method method=manager.getClass().getMethod("importWorld",options);Object result=method.invoke(manager,request);
                if(!(Boolean)method.getReturnType().getMethod("isSuccess").invoke(result))throw new IllegalStateException("Multiverse dünya kaydı başarısız: "+result);
            }
            Method get=manager.getClass().getMethod("getWorld",String.class);Object option=get.invoke(manager,world.getName());
            Object entry=get.getReturnType().getMethod("getOrNull").invoke(option);
            if(entry!=null){
                entry.getClass().getMethod("setAdjustSpawn",boolean.class).invoke(entry,false);
                entry.getClass().getMethod("setGameMode",GameMode.class).invoke(entry,GameMode.ADVENTURE);
                if(spawn!=null&&spawn.getWorld()==world){world.setSpawnLocation(spawn);entry.getClass().getMethod("setSpawnLocation",Location.class).invoke(entry,spawn);}
            }
            manager.getClass().getMethod("saveWorldsConfig").invoke(manager);
        }catch(ReflectiveOperationException|RuntimeException e){owner.getLogger().log(Level.WARNING,"Multiverse ortak dünya kaydı tamamlanamadı; /esya durum ile dünyayı kontrol et.",e);}
    }

    private Object manager(Plugin mv)throws ReflectiveOperationException{
        Class<?> api=Class.forName("org.mvplugins.multiverse.core.MultiverseCoreApi",true,mv.getClass().getClassLoader());
        Object instance=api.getMethod("get").invoke(null);return api.getMethod("getWorldManager").invoke(instance);
    }
}
