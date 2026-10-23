package tr.kasabatr.ispanya;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import org.bukkit.*;

/** Only marked match copies are disposable. The supplied public world is never replaced. */
final class WorldInstances {
    private static final String PREFIX="isp_room_",MARKER=".kasabaispanya-instance";
    private static final List<String> ASSETS=List.of("level.dat","arena.json","region/r.-1.-1.mca","region/r.-1.0.mca","region/r.0.-1.mca","region/r.0.0.mca");
    private final KasabaIspanyaPlugin plugin;
    private final Set<World> copies=new HashSet<>();
    World publicWorld;
    WorldInstances(KasabaIspanyaPlugin plugin){this.plugin=plugin;}
    void startup()throws IOException{
        String name=plugin.getConfig().getString("template-world","ispanya");Path folder=path(name);
        if(name.startsWith(PREFIX))throw new IOException("Maç kopyası ortak dünya olarak kullanılamaz.");
        if(!Files.exists(folder)){
            Path stage=folder.resolveSibling(".ispanya-install-"+UUID.randomUUID());
            try{
                for(String asset:ASSETS){Path file=stage.resolve(asset);Files.createDirectories(file.getParent());try(InputStream in=plugin.getResource("map-template/"+asset)){if(in==null)throw new IOException("Hazır harita eksik: "+asset);Files.copy(in,file);}}
                try{Files.move(stage,folder,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(stage,folder);}
            }finally{if(Files.exists(stage))deleteFiles(stage);}
        }
        if(!Files.isRegularFile(folder.resolve("level.dat")))throw new IOException("Mevcut dünya eksik; üzerine yazılmadı: "+name);
        publicWorld=Bukkit.getWorld(name);if(publicWorld==null)publicWorld=load(name,false);
        if(publicWorld==null)throw new IOException("İspanya dünyası yüklenemedi.");
        for(int i=0;i<plugin.track().size();i++)if(!safe(plugin.track().tile(i).location(publicWorld)))throw new IOException("Harita yolu ile dünya uyuşmuyor: kare "+(i+1)+". Mevcut dünya korunuyor.");
        Location lobby=lobby();if(!safe(lobby))throw new IOException("İspanya lobi zemini güvenli değil; /ispanya lobi ayarla için konumu kontrol et.");
        configure(publicWorld,false);publicWorld.setSpawnLocation(lobby);new MultiverseBridge(plugin).register(publicWorld,lobby);clearStaleCopies();
    }
    Location lobby(){return new Location(publicWorld,plugin.getConfig().getDouble("lobby.x",.5),plugin.getConfig().getDouble("lobby.y",65),plugin.getConfig().getDouble("lobby.z",34.5),(float)plugin.getConfig().getDouble("lobby.yaw",180),(float)plugin.getConfig().getDouble("lobby.pitch",0));}
    static boolean safe(Location point){return point!=null&&point.getWorld()!=null&&point.getBlock().isPassable()&&point.clone().add(0,1,0).getBlock().isPassable()&&point.clone().add(0,-1,0).getBlock().getType().isSolid();}
    boolean templateReady(){return publicWorld!=null&&Bukkit.getWorld(publicWorld.getUID())==publicWorld;}
    boolean managed(World world){return world!=null&&(world==publicWorld||copies.contains(world));}
    World create(int id)throws IOException{
        if(!templateReady())throw new IOException("Ortak İspanya dünyası hazır değil.");
        publicWorld.save(true);
        Path source=publicWorld.getWorldFolder().toPath();String name=PREFIX+id+"_"+UUID.randomUUID().toString().substring(0,8);Path target=path(name);
        Files.createDirectory(target);Files.writeString(target.resolve(MARKER),"KasabaIspanya "+id);
        try{
            Files.walkFileTree(source,new SimpleFileVisitor<>(){
                @Override public FileVisitResult preVisitDirectory(Path dir,BasicFileAttributes attrs)throws IOException{
                    Path relative=source.relativize(dir);if(!relative.toString().isEmpty()&&Set.of("playerdata","stats","advancements","entities").contains(relative.getName(0).toString()))return FileVisitResult.SKIP_SUBTREE;
                    if(Files.isSymbolicLink(dir))throw new IOException("Dünya kopyasında bağlantılı klasör desteklenmiyor.");Files.createDirectories(target.resolve(relative));return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attrs)throws IOException{
                    if(Files.isSymbolicLink(file))throw new IOException("Dünya kopyasında bağlantılı dosya desteklenmiyor.");
                    if(!Set.of("uid.dat","session.lock",MARKER,"level.dat_old").contains(file.getFileName().toString()))Files.copy(file,target.resolve(source.relativize(file)));return FileVisitResult.CONTINUE;
                }
            });
            World result=load(name,true);if(result==null)throw new IOException("Maç dünyası açılamadı.");copies.add(result);configure(result,true);return result;
        }catch(Exception e){World loaded=Bukkit.getWorld(name);if(loaded!=null&&!Bukkit.unloadWorld(loaded,false))throw new IOException("Eksik maç dünyası kullanımda; silinmedi: "+name,e);deleteMarked(target);throw new IOException("Maç dünyası oluşturulamadı.",e);}
    }
    private World load(String name,boolean match){return Bukkit.createWorld(new WorldCreator(name).generator(new EmptyWorldGenerator()).generateStructures(false));}
    private void configure(World world,boolean match){
        world.setAutoSave(!match);world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE,false);world.setGameRule(GameRule.DO_WEATHER_CYCLE,false);world.setGameRule(GameRule.DO_MOB_SPAWNING,false);world.setGameRule(GameRule.RANDOM_TICK_SPEED,0);world.setGameRule(GameRule.DO_FIRE_TICK,false);world.setGameRule(GameRule.KEEP_INVENTORY,true);world.setGameRule(GameRule.LOCATOR_BAR,false);
        world.setTime(1000);world.setStorm(false);world.setThundering(false);world.getWorldBorder().setCenter(.5,.5);world.getWorldBorder().setSize(125);world.setSpawnLocation(new Location(world,.5,65,34.5));
    }
    void close(World world){
        if(!copies.contains(world))return;
        if(!world.getPlayers().isEmpty()||!Bukkit.unloadWorld(world,false)){plugin.getLogger().warning("Maç dünyası kullanımda; silinmedi: "+world.getName());return;}
        copies.remove(world);try{deleteMarked(path(world.getName()));}catch(IOException e){plugin.getLogger().warning("Eski maç kopyası sonraki açılışta temizlenecek: "+e.getMessage());}
    }
    void clearStaleCopies()throws IOException{try(var files=Files.list(Bukkit.getWorldContainer().toPath())){for(Path p:files.toList())if(p.getFileName().toString().startsWith(PREFIX)&&Files.isRegularFile(p.resolve(MARKER))&&Bukkit.getWorld(p.getFileName().toString())==null)deleteMarked(p);}}
    private Path path(String name){if(!name.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("Geçersiz dünya adı.");Path container=Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();Path result=container.resolve(name);if(Files.isSymbolicLink(result))throw new IllegalArgumentException("Dünya klasörü sembolik bağlantı olamaz.");return result;}
    private void deleteMarked(Path folder)throws IOException{if(!folder.toAbsolutePath().normalize().getParent().equals(Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize())||!folder.getFileName().toString().startsWith(PREFIX)||!Files.isRegularFile(folder.resolve(MARKER)))throw new IOException("İşaretlenmemiş dünya silinemez.");deleteFiles(folder);}
    private static void deleteFiles(Path folder)throws IOException{Files.walkFileTree(folder,new SimpleFileVisitor<>(){@Override public FileVisitResult visitFile(Path file,BasicFileAttributes a)throws IOException{Files.delete(file);return FileVisitResult.CONTINUE;}@Override public FileVisitResult postVisitDirectory(Path dir,IOException e)throws IOException{if(e!=null)throw e;Files.delete(dir);return FileVisitResult.CONTINUE;}});}
}
