package tr.kasaba.hub;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import java.io.IOException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class LobbyStore {
    record Point(UUID world,String worldName,double x,double y,double z,float yaw,float pitch){
        Point{if(world==null||worldName==null||worldName.isBlank()||!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||!Float.isFinite(yaw)||!Float.isFinite(pitch)||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000||Math.abs(pitch)>90)throw new IllegalArgumentException("Geçersiz lobi konumu.");}
        boolean contains(UUID world,double x,double z,int radius){return this.world.equals(world)&&Math.abs(this.x-x)<=radius&&Math.abs(this.z-z)<=radius;}
    }
    private final Path path;
    LobbyStore(Path path){this.path=path;}
    Point load() throws IOException{
        if(!Files.exists(path))return null;YamlConfiguration y=new YamlConfiguration();
        try{y.load(path.toFile());}catch(InvalidConfigurationException e){throw new IOException("Lobi dosyası bozuk.",e);}
        for(String key:new String[]{"x","y","z","yaw","pitch"})if(!(y.get(key) instanceof Number))throw new IOException("Lobi sayısı eksik: "+key);
        try{return new Point(UUID.fromString(y.getString("dunya-kimligi","")),y.getString("dunya"),y.getDouble("x"),y.getDouble("y"),y.getDouble("z"),(float)y.getDouble("yaw"),(float)y.getDouble("pitch"));}catch(IllegalArgumentException e){throw new IOException("Lobi verisi geçersiz.",e);}
    }
    void save(Point p) throws IOException{
        YamlConfiguration y=new YamlConfiguration();y.set("dunya-kimligi",p.world().toString());y.set("dunya",p.worldName());y.set("x",p.x());y.set("y",p.y());y.set("z",p.z());y.set("yaw",p.yaw());y.set("pitch",p.pitch());
        Files.createDirectories(path.getParent());Path tmp=Files.createTempFile(path.getParent(),".lobi-",".tmp");
        try{Files.writeString(tmp,y.saveToString(),StandardCharsets.UTF_8);try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(tmp);}
    }
}
