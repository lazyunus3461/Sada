package tr.kasabatr.tac;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;

final class CrownMap {
    record Point(double x,double y,double z){Location at(World w){return new Location(w,x,y,z);}}
    record Cell(int x,int y,int z){}
    final String worldName;final Point lobby;final List<Point> spawns=new ArrayList<>();
    final Map<String,List<Cell>> gates=new LinkedHashMap<>();final int minX,maxX,minZ,maxZ;
    CrownMap(String name,Path file)throws IOException{
        try{var root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();worldName=name;
            if(!name.matches("[A-Za-z0-9_-]{1,80}")||!root.get("world").getAsString().equals(name))throw new IllegalArgumentException("Şato dünya adı eşleşmiyor.");
            lobby=point(root.getAsJsonObject("lobby"));var b=root.getAsJsonArray("arena_bounds");minX=b.get(0).getAsInt();maxX=b.get(1).getAsInt();minZ=b.get(2).getAsInt();maxZ=b.get(3).getAsInt();
            if(minX>=maxX||minZ>=maxZ||maxX-minX>256||maxZ-minZ>256)throw new IllegalArgumentException("Şato sınırı geçersiz.");
            Set<Point> unique=new HashSet<>();for(var e:root.getAsJsonArray("spawns")){Point p=point(e.getAsJsonObject());if(!inside(p.x,p.y,p.z)||!unique.add(p))throw new IllegalArgumentException("Doğma noktası geçersiz.");spawns.add(p);}if(spawns.size()!=16)throw new IllegalArgumentException("16 doğma noktası gerekli.");
            Set<Cell> used=new HashSet<>();for(var e:root.getAsJsonArray("gates")){var g=e.getAsJsonObject();String id=g.get("id").getAsString();List<Cell> cells=new ArrayList<>();for(var p:g.getAsJsonArray("blocks")){var a=p.getAsJsonArray();Cell c=new Cell(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());if(!inside(c.x,c.y,c.z)||!used.add(c))throw new IllegalArgumentException("Kapı geçersiz.");cells.add(c);}if(cells.size()!=15||gates.put(id,List.copyOf(cells))!=null)throw new IllegalArgumentException("Kapı tekrarı / hücre sayısı.");}
            if(!gates.keySet().equals(Set.of("KUZEY","DOGU")))throw new IllegalArgumentException("Kuzey/doğu kapısı gerekli.");
        }catch(Exception e){throw new IOException("crown-map.json okunamadı: "+e.getMessage(),e);}
    }
    boolean inside(double x,double y,double z){return x>=minX&&x<maxX+1&&z>=minZ&&z<maxZ+1&&y>=80.75&&y<92;}
    boolean contains(Location at){return inside(at.getX(),at.getY(),at.getZ());}
    private static Point point(JsonObject p){double x=p.get("x").getAsDouble(),y=p.get("y").getAsDouble(),z=p.get("z").getAsDouble();if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Sonlu konum gerekli.");return new Point(x,y,z);}
}
