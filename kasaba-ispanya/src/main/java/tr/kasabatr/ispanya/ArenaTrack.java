package tr.kasabatr.ispanya;

import java.io.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;

final class ArenaTrack {
    private final List<Tile> tiles;
    private final Map<Integer,Integer> shortcuts;
    ArenaTrack(File file){
        var yaml=YamlConfiguration.loadConfiguration(file);var path=new ArrayList<Tile>();
        for(String row:yaml.getStringList("tiles")){
            String[] parts=row.split(",",4);if(parts.length!=4)throw new IllegalArgumentException("Bozuk arena karesi: "+row);
            double x=Double.parseDouble(parts[0]),y=Double.parseDouble(parts[1]),z=Double.parseDouble(parts[2]);
            if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z))throw new IllegalArgumentException("Geçersiz kare konumu.");
            path.add(new Tile(x,y,z,parts[3].trim()));
        }
        if(path.size()<10||!path.getFirst().kind.equals("start")||!path.getLast().kind.equals("finish"))throw new IllegalArgumentException("Başlangıç/bitiş karesi eksik.");
        tiles=List.copyOf(path);var links=new HashMap<Integer,Integer>();
        for(String row:yaml.getStringList("shortcuts")){
            String[] parts=row.split(",",2);if(parts.length!=2)throw new IllegalArgumentException("Bozuk kestirme: "+row);
            int from=Integer.parseInt(parts[0]),to=Integer.parseInt(parts[1]);
            if(from<0||to<=from||to>=tiles.size()||!tiles.get(from).kind.equals("yellow")||links.putIfAbsent(from,to)!=null)throw new IllegalArgumentException("Geçersiz kestirme: "+row);
        }
        for(int i=0;i<tiles.size();i++)if(tiles.get(i).kind.equals("yellow")&&!links.containsKey(i))throw new IllegalArgumentException("Hedefsiz sarı kare: "+i);
        shortcuts=Map.copyOf(links);
    }
    int size(){return tiles.size();}
    Tile tile(int index){return tiles.get(Math.clamp(index,0,tiles.size()-1));}
    int shortcutDestination(int index){return shortcuts.getOrDefault(index,-1);}
    List<Tile> tiles(){return tiles;}
    Map<Integer,Integer> shortcuts(){return shortcuts;}
    record Tile(double x,double y,double z,String kind){Location location(World world){return new Location(world,x,y,z);}}
}
