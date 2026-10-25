package tr.kasabatr.boya;
import java.io.*;import java.nio.file.*;import java.util.*;import org.bukkit.*;
final class PaintMap {
 record Tile(int x,int y,int z){}
 record Point(double x,double y,double z,float yaw){Location at(World world){return new Location(world,x,y,z,yaw,0);}}
 final String worldName;final Point lobby=new Point(.5,80,.5,180);final Set<Tile> paintable;
 PaintMap(String worldName,Path file)throws IOException{
  if(!worldName.matches("[A-Za-z0-9_-]{1,80}")||worldName.startsWith("boya_room_"))throw new IOException("Geçersiz arena adı.");this.worldName=worldName;Set<Tile> sites=new LinkedHashSet<>();
  try{for(String line:Files.readAllLines(file)){String[] a=line.split(",",-1);if(a.length!=3)throw new IOException("Boyama satırı bozuk.");Tile t=new Tile(Integer.parseInt(a[0]),Integer.parseInt(a[1]),Integer.parseInt(a[2]));if(Math.abs(t.x)>42||Math.abs(t.z)>30||(t.y!=79&&t.y!=83)||!sites.add(t))throw new IOException("Boyama noktası geçersiz: "+t);}}catch(NumberFormatException e){throw new IOException("Boyama koordinatı sayısal değil.",e);}
  if(sites.size()<1000||sites.size()>10000)throw new IOException("Boyama bloğu sayısı geçersiz.");paintable=Set.copyOf(sites);
 }
 Location spawn(World world,PaintTeam team,int index){return new Location(world,team==PaintTeam.RED?-38.5:39.5,80,(index%3-1)*3+.5,team==PaintTeam.RED?-90:90,0);}
 boolean contains(Location at){return Math.abs(at.getX())<43&&Math.abs(at.getZ())<31&&at.getY()>=79.5&&at.getY()<98;}
}
