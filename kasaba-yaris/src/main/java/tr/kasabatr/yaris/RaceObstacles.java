package tr.kasabatr.yaris;

import java.util.*;
import org.bukkit.*;

/** Fixed, passable hazards on straight sections. No checkpoint or starting lane is obstructed. */
final class RaceObstacles {
 record Hazard(String kind,int x,int z){}
 static final List<Hazard> HORSE=List.of(new Hazard("jump",237,55),new Hazard("slalom",190,-55),new Hazard("water",151,-55),new Hazard("jump",118,-55));
 static final List<Hazard> BOAT=List.of(new Hazard("buoys",-142,55),new Hazard("slalom",-182,-55),new Hazard("buoys",-222,-55));
 static void build(World world,Mode mode){
  for(Hazard h:mode==Mode.HORSE?HORSE:BOAT){
   if(h.kind.equals("water")){for(int dx=-5;dx<=5;dx++)for(int dz=-8;dz<=8;dz++){world.getBlockAt(h.x+dx,63,h.z+dz).setType(Material.PRISMARINE,false);world.getBlockAt(h.x+dx,64,h.z+dz).setType(Math.abs(dx)==5||Math.abs(dz)==8?Material.GRAVEL:Material.WATER,false);}continue;}
   if(h.kind.equals("jump")){for(int dz=-6;dz<=6;dz++)if(Math.abs(dz)>2)world.getBlockAt(h.x,65,h.z+dz).setType(Material.OAK_FENCE,false);continue;}
   // Alternating islands leave at least five full blocks to pass on the other side.
   for(int gate=0;gate<3;gate++){int px=h.x+(gate-1)*7,side=gate%2==0?-1:1;for(int dx=-1;dx<=1;dx++)for(int dz=side<0?-7:-1;dz<=(side<0?1:7);dz++){for(int y=64;y<=66;y++)world.getBlockAt(px+dx,y,h.z+dz).setType(mode==Mode.BOAT?Material.SMOOTH_SANDSTONE:Material.HAY_BLOCK,false);}world.getBlockAt(px,67,h.z+side*6).setType(mode==Mode.BOAT?Material.SEA_LANTERN:Material.ORANGE_CONCRETE,false);}
  }
 }
 static void install(KasabaYarisPlugin plugin,World world){if(world!=null&&plugin.getConfig().getInt("obstacles-version",0)<1){build(world,Mode.HORSE);build(world,Mode.BOAT);world.save(true);plugin.getConfig().set("obstacles-version",1);plugin.saveConfig();}}
}
