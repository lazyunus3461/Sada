package tr.kasabatr.yaris;

import java.util.*;
import java.nio.file.*;
import org.bukkit.*;

/** Medium courses: broad jumps, alternating gates and a contained shallow water crossing. */
final class RaceObstacles {
 record Hazard(String kind,int x,int z){}
 static final List<Hazard> HORSE=List.of(new Hazard("jump",210,55),new Hazard("jump",221,55),new Hazard("slalom",192,-55),new Hazard("water",220,-55),new Hazard("jump",148,-55));
 static final List<Hazard> BOAT=List.of(new Hazard("slalom",-166,55),new Hazard("slalom",-184,-55));
 static void build(World world,Mode mode){
  if(mode==Mode.HORSE){
   jump(world,210,55);jump(world,221,55);jump(world,148,-55);
   for(int gate=0;gate<3;gate++)island(world,192-gate*15,-55,gate%2==0?-1:1,false);
   for(int dx=-5;dx<=5;dx++)for(int dz=-9;dz<=9;dz++){
    world.getBlockAt(220+dx,63,-55+dz).setType(Material.PRISMARINE,false);
    world.getBlockAt(220+dx,64,-55+dz).setType(Math.abs(dx)==5||Math.abs(dz)==9?Material.GRAVEL:Material.WATER,false);
   }
  }else{
   for(int gate=0;gate<3;gate++)island(world,-180+gate*15,55,gate%2==0?-1:1,true);
   for(int gate=0;gate<4;gate++)island(world,-158-gate*19,-55,gate%2==0?1:-1,true);
  }
 }
 private static void jump(World w,int x,int z){
  for(int dz=-9;dz<=9;dz++)w.getBlockAt(x,65,z+dz).setType(Material.OAK_LOG,false);
  for(int dz:new int[]{-10,10}){for(int y=65;y<=68;y++)w.getBlockAt(x,y,z+dz).setType(Material.OAK_FENCE,false);w.getBlockAt(x,69,z+dz).setType(Material.ORANGE_CONCRETE,false);}
 }
 private static void island(World w,int x,int z,int side,boolean boat){
  for(int dx=-2;dx<=2;dx++)for(int dz=side<0?-10:-2;dz<=(side<0?2:10);dz++){
   w.getBlockAt(x+dx,64,z+dz).setType(boat?Material.SMOOTH_SANDSTONE:Material.PACKED_MUD,false);
   for(int y=65;y<=66;y++)w.getBlockAt(x+dx,y,z+dz).setType(boat?Material.SMOOTH_SANDSTONE:Material.HAY_BLOCK,false);
  }
  for(int y=67;y<=69;y++)w.getBlockAt(x,y,z+side*8).setType(boat?Material.PRISMARINE_WALL:Material.OAK_FENCE,false);
  w.getBlockAt(x,70,z+side*8).setType(boat?Material.SEA_LANTERN:Material.ORANGE_CONCRETE,false);
 }
 static void install(KasabaYarisPlugin plugin,World world){
  if(world==null||plugin.getConfig().getInt("obstacles-version",0)>=2)return;
  World reference=null;
  try{
   // Replace only the exact footprints owned by the previous obstacle installer.
   if(plugin.getConfig().getInt("obstacles-version",0)==1){
    String name="yaris_room_reference_"+UUID.randomUUID().toString().replace("-","");EmbeddedWorld.install(plugin,name);
    Files.writeString(Bukkit.getWorldContainer().toPath().resolve(name).resolve(".kasabayaris-instance"),"obstacle-migration");
    reference=Bukkit.createWorld(new WorldCreator(name).generator(new EmptyWorldGenerator()).generateStructures(false));
    if(reference==null)throw new IllegalStateException("Önceki engeller için temiz şablon açılamadı.");
    for(int[] h:new int[][]{{237,55},{190,-55},{151,-55},{118,-55},{-142,55},{-182,-55},{-222,-55}})
     for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++)for(int y=63;y<=68;y++)
      world.getBlockAt(h[0]+dx,y,h[1]+dz).setBlockData(reference.getBlockAt(h[0]+dx,y,h[1]+dz).getBlockData(),false);
   }
   build(world,Mode.HORSE);build(world,Mode.BOAT);world.save(true);plugin.getConfig().set("obstacles-version",2);plugin.saveConfig();
  }catch(Exception e){throw new IllegalStateException("Yarış engelleri güncellenemedi; sürüm kaydı korunuyor.",e);}
  finally{if(reference!=null)plugin.instances().close(reference);}
 }
}
