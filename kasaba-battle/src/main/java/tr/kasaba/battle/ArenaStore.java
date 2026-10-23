package tr.kasaba.battle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

final class ArenaStore {
    static final class Definition {
        final int id;BattleRules.Area area;Location center;
        final List<Location> spawns=new ArrayList<>(),candidates=new ArrayList<>();
        Definition(int id){this.id=id;}
        boolean ready(){return area!=null&&center!=null&&center.getWorld()!=null&&area.contains(center.getWorld().getUID(),center.getX(),center.getZ())&&area.radius(center.getX(),center.getZ())>=8&&spawns.size()>=2&&candidates.size()>=4;}
    }
    final Map<Integer,Definition> rooms=new TreeMap<>();
    Location lobby;BattleRules.Area lobbyArea;
    private final Path file,journal;private final Map<String,Location> temporary=new LinkedHashMap<>();
    ArenaStore(Path folder)throws IOException{file=folder.resolve("arenalar.yml");journal=folder.resolve("gecici-sandiklar.yml");load();recoverChests();}
    private static BattleRules.Area area(YamlConfiguration d,String path){if(!d.contains(path))return null;return new BattleRules.Area(UUID.fromString(d.getString(path+".world")),d.getInt(path+".x1"),d.getInt(path+".x2"),d.getInt(path+".z1"),d.getInt(path+".z2"));}
    private static void area(YamlConfiguration d,String path,BattleRules.Area a){if(a==null)return;d.set(path+".world",a.world().toString());d.set(path+".x1",a.minX());d.set(path+".x2",a.maxX());d.set(path+".z1",a.minZ());d.set(path+".z2",a.maxZ());}
    void load()throws IOException{YamlConfiguration d=AtomicYaml.read(file);try{lobby=d.getLocation("lobi.konum");lobbyArea=area(d,"lobi.bolge");rooms.clear();for(int id=1;id<=3;id++){Definition r=new Definition(id);String k="arenalar."+id;r.area=area(d,k+".bolge");r.center=d.getLocation(k+".merkez");for(Object value:d.getList(k+".dogmalar",List.of())){if(!(value instanceof Location l))throw new IOException("Doğma kaydı bozuk.");r.spawns.add(l);}for(Object value:d.getList(k+".sandik-adaylari",List.of())){if(!(value instanceof Location l))throw new IOException("Sandık kaydı bozuk.");r.candidates.add(l);}rooms.put(id,r);}}catch(RuntimeException e){throw new IOException("Arena kaydı okunamadı; dosya korundu.",e);}}
    void save()throws IOException{YamlConfiguration d=new YamlConfiguration();d.set("lobi.konum",lobby);area(d,"lobi.bolge",lobbyArea);for(Definition r:rooms.values()){String k="arenalar."+r.id;area(d,k+".bolge",r.area);d.set(k+".merkez",r.center);d.set(k+".dogmalar",r.spawns);d.set(k+".sandik-adaylari",r.candidates);}try{AtomicYaml.write(file,d);}catch(IOException e){load();throw e;}}
    boolean lobbyReady(){return lobby!=null&&lobby.getWorld()!=null&&lobbyArea!=null&&in(lobbyArea,lobby);}
    boolean importProfile(World world)throws IOException{
        if(lobby!=null||rooms.values().stream().anyMatch(r->r.area!=null||r.center!=null))return false;
        Path path=world.getWorldFolder().toPath().resolve("kasaba-br-map.yml");if(!java.nio.file.Files.exists(path))return false;
        YamlConfiguration d=AtomicYaml.read(path);if(d.getInt("schema")!=1)throw new IOException("Harita profili sürümü desteklenmiyor.");
        try{lobby=position(world,d.getString("lobby.position"));lobbyArea=profileArea(world,d.getString("lobby.area"));
            for(Definition r:rooms.values()){String k="arenas."+r.id;r.area=profileArea(world,d.getString(k+".area"));r.center=position(world,d.getString(k+".center"));for(String s:d.getStringList(k+".spawns"))r.spawns.add(position(world,s));for(String s:d.getStringList(k+".candidates"))r.candidates.add(position(world,s));if(!r.ready())throw new IllegalArgumentException("Arena profili eksik: "+r.id);}
            validateArea(0,lobbyArea);for(Definition r:rooms.values())validateArea(r.id,r.area);save();return true;
        }catch(RuntimeException|IOException e){load();throw new IOException("Harita profili uygulanamadı; mevcut ayarlar korundu.",e);}
    }
    private static Location position(World w,String s){String[] a=s.split("\\|");if(a.length!=3)throw new IllegalArgumentException("Konum bozuk.");return new Location(w,Double.parseDouble(a[0]),Double.parseDouble(a[1]),Double.parseDouble(a[2]));}
    private static BattleRules.Area profileArea(World w,String s){String[] a=s.split("\\|");if(a.length!=4)throw new IllegalArgumentException("Bölge bozuk.");return new BattleRules.Area(w.getUID(),Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]));}
    static boolean in(BattleRules.Area a,Location l){return a!=null&&l!=null&&l.getWorld()!=null&&a.contains(l.getWorld().getUID(),l.getX(),l.getZ());}
    void validateArea(int id,BattleRules.Area next){
        if(lobby!=null&&!next.world().equals(lobby.getWorld().getUID()))throw new IllegalArgumentException("BR lobisi ve üç arena aynı dünyada olmalı.");
        if(id!=0&&lobbyArea!=null&&next.overlaps(lobbyArea))throw new IllegalArgumentException("Arena lobi bölgesiyle çakışamaz.");
        for(Definition r:rooms.values())if(r.id!=id&&r.area!=null&&next.overlaps(r.area))throw new IllegalArgumentException("Bölge arena "+r.id+" ile çakışıyor.");
        if(id==0&&lobby!=null&&!in(next,lobby))throw new IllegalArgumentException("Kaydettiğin lobi konumu seçilen bölgede değil.");
        Definition r=rooms.get(id);if(r!=null){if(r.center!=null&&!in(next,r.center))throw new IllegalArgumentException("Arena merkezi seçilen bölgede kalmalı.");for(Location l:r.spawns)if(!in(next,l))throw new IllegalArgumentException("Kayıtlı doğma noktaları bölge dışında kalıyor.");for(Location l:r.candidates)if(!in(next,l))throw new IllegalArgumentException("Sandık adayları bölge dışında kalıyor.");}
    }
    static boolean safe(Location feet){if(feet.getWorld()==null)return false;Block b=feet.getBlock(),floor=b.getRelative(0,-1,0);if(!b.isEmpty()||!b.getRelative(0,1,0).isEmpty()||!floor.getType().isSolid())return false;return !Set.of(Material.MAGMA_BLOCK,Material.CACTUS,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.POWDER_SNOW,Material.FIRE,Material.SOUL_FIRE,Material.TNT).contains(floor.getType());}
    List<Location> scan(Definition r,int count,Random random){if(r.area==null||r.center==null)throw new IllegalArgumentException("Önce bölgeyi ve arena merkezini ayarla.");List<Location> out=new ArrayList<>();World w=r.center.getWorld();double radius=r.area.radius(r.center.getX(),r.center.getZ())*0.9;for(int i=0;i<Math.min(10000,count*100)&&out.size()<count;i++){int x=r.area.minX()+random.nextInt(r.area.maxX()-r.area.minX()+1),z=r.area.minZ()+random.nextInt(r.area.maxZ()-r.area.minZ()+1);if(Math.hypot(x+0.5-r.center.getX(),z+0.5-r.center.getZ())>radius||!w.isChunkGenerated(x>>4,z>>4))continue;Location l=new Location(w,x+0.5,w.getHighestBlockYAt(x,z,HeightMap.MOTION_BLOCKING_NO_LEAVES)+1,z+0.5);if(safe(l)&&out.stream().noneMatch(a->a.distanceSquared(l)<16))out.add(l);}if(out.size()<count)throw new IllegalArgumentException("Yeterli güvenli zemin bulunamadı ("+out.size()+"/"+count+"). Haritayı yükle veya daha az nokta iste.");return out;}
    /** Open sky on natural ground: roofs never consume the guaranteed outdoor quota. */
    static boolean outdoors(Location l){
        if(l.getWorld()==null||l.getWorld().getHighestBlockYAt(l,HeightMap.MOTION_BLOCKING_NO_LEAVES)>l.getBlockY())return false;
        return Set.of(Material.GRASS_BLOCK,Material.DIRT,Material.COARSE_DIRT,Material.ROOTED_DIRT,Material.DIRT_PATH,Material.SAND,Material.RED_SAND,Material.GRAVEL,Material.STONE,Material.ANDESITE,Material.DIORITE,Material.GRANITE,Material.MOSS_BLOCK,Material.PODZOL,Material.MYCELIUM).contains(l.getBlock().getRelative(0,-1,0).getType());
    }
    static String key(Location l){return l.getWorld().getUID()+"|"+l.getBlockX()+"|"+l.getBlockY()+"|"+l.getBlockZ();}
    private void journal()throws IOException{YamlConfiguration d=new YamlConfiguration();d.set("blocks",new ArrayList<>(temporary.values()));AtomicYaml.write(journal,d);}
    void recoverChests()throws IOException{YamlConfiguration d=AtomicYaml.read(journal);temporary.clear();for(Object value:d.getList("blocks",List.of())){if(!(value instanceof Location l)||l.getWorld()==null)throw new IOException("Geçici sandık dünyası yüklenemedi; dünya yükleyicisini kontrol et.");if(l.getBlock().getType()==Material.CHEST)l.getBlock().setType(Material.AIR,false);}journal();}
    void placeChests(List<Location> locations)throws IOException{
        for(Location l:locations)if(!safe(l)||temporary.containsKey(key(l)))throw new IllegalArgumentException("Sandık noktası dolu veya güvensiz: "+l.getBlockX()+","+l.getBlockZ());
        List<String> added=new ArrayList<>();for(Location l:locations){String k=key(l);temporary.put(k,l.clone());added.add(k);}try{journal();}catch(IOException e){added.forEach(temporary::remove);throw e;}for(Location l:locations)l.getBlock().setType(Material.CHEST,false);
    }
    void removeChests(Collection<Location> locations)throws IOException{for(Location l:locations){String k=key(l);if(!temporary.containsKey(k))continue;if(l.getBlock().getType()==Material.CHEST)l.getBlock().setType(Material.AIR,false);temporary.remove(k);}journal();}
}
