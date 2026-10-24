package tr.kasabatr.ispanya;

import java.io.*;
import java.nio.file.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import tr.kasaba.hub.api.MenuStyle;

/** A dedicated dice-themed waiting plaza, separate from the playing path. */
final class LobbyBuilder {
 static void ensure(KasabaIspanyaPlugin plugin,World world)throws IOException{
  boolean migrate=plugin.getConfig().getInt("lobby-build-version",0)<1;
  int x=plugin.getConfig().getInt("generated-lobby.x",0),z=plugin.getConfig().getInt("generated-lobby.z",86);
  if(!migrate&&world.getBlockAt(x,64,z).getType()==Material.SMOOTH_QUARTZ){rails(world,x,z);if(plugin.getConfig().getInt("lobby-build-version",0)<2){plugin.getConfig().set("lobby-build-version",2);plugin.saveConfig();world.save(true);}return;}
  if(migrate){for(int attempt=0;attempt<10;attempt++){boolean empty=true;for(int dx=-19;dx<=19&&empty;dx++)for(int dz=-15;dz<=15&&empty;dz++)for(int y=63;y<=76;y++)if(!world.getBlockAt(x+dx,y,z+dz).getType().isAir()){empty=false;break;}if(empty)break;if(attempt==9)throw new IOException("Yeni lobi için boş alan bulunamadı; mevcut yapılar korundu.");z+=48;}}
  final int cx=x,cz=z;
  for(int dx=-18;dx<=18;dx++)for(int dz=-14;dz<=14;dz++){
   world.getBlockAt(x+dx,63,z+dz).setType(Material.POLISHED_DEEPSLATE,false);
   world.getBlockAt(x+dx,64,z+dz).setType(Material.SMOOTH_QUARTZ,false);
   world.getBlockAt(x+dx,65,z+dz).setType(Math.abs(dx)==18||Math.abs(dz)==14?Material.POLISHED_BLACKSTONE_BRICKS:((dx+dz)&3)==0?Material.CALCITE:Material.QUARTZ_BLOCK,false);
   if(Math.abs(dx)==18||Math.abs(dz)==14)world.getBlockAt(x+dx,66,z+dz).setType(Material.DEEPSLATE_BRICK_WALL,false);
  }
  for(int dx:new int[]{-15,15})for(int dz:new int[]{-11,11}){for(int y=66;y<=70;y++)world.getBlockAt(x+dx,y,z+dz).setType(Material.CHERRY_LOG,false);world.getBlockAt(x+dx,71,z+dz).setType(Material.SEA_LANTERN,false);}
  for(int dx=-10;dx<=10;dx++)for(int dz=-3;dz<=3;dz++)world.getBlockAt(x+dx,66,z-10+dz).setType(Material.CHERRY_PLANKS,false);
  // Two large dice frame the information pavilion.
  for(int side:new int[]{-1,1}){int bx=x+side*12;for(int dx=-2;dx<=2;dx++)for(int y=66;y<=70;y++)for(int dz=-2;dz<=2;dz++)world.getBlockAt(bx+dx,y,z-5+dz).setType(Material.WHITE_CONCRETE,false);for(int offset:new int[]{-1,1}){world.getBlockAt(bx+offset,67,z-3).setType(Material.BLACK_CONCRETE,false);world.getBlockAt(bx+offset,69,z-3).setType(Material.BLACK_CONCRETE,false);}world.getBlockAt(bx,68,z-3).setType(Material.BLACK_CONCRETE,false);}
  for(Entity entity:world.getEntities())if(entity.getScoreboardTags().contains("kasaba-sans-lobby"))entity.remove();
  for(int i=0;i<3;i++){final int line=i;world.spawn(new Location(world,x+.5,72.2-i*.55,z-9.5),TextDisplay.class,text->{text.addScoreboardTag("kasaba-sans-lobby");text.setBillboard(Display.Billboard.CENTER);text.setPersistent(true);text.setSeeThrough(false);text.setShadowed(true);text.text(line==0?MenuStyle.title("ŞANS YOLU"):MenuStyle.line(line==1?"Pusuladan açık odaları seç • 2–4 kişi":"Renkler ve kestirmeler her maç değişir"));});}
  if(migrate){Path config=plugin.getDataFolder().toPath().resolve("config.yml"),backup=config.resolveSibling("config-before-lobby-0.6.0.yml");if(Files.isRegularFile(config)&&!Files.exists(backup))Files.copy(config,backup);plugin.getConfig().set("generated-lobby.x",cx);plugin.getConfig().set("generated-lobby.z",cz);plugin.getConfig().set("lobby.x",cx+.5);plugin.getConfig().set("lobby.y",66.0);plugin.getConfig().set("lobby.z",cz+6.5);plugin.getConfig().set("lobby.yaw",180);plugin.getConfig().set("lobby.pitch",0);plugin.getConfig().set("lobby-build-version",2);plugin.saveConfig();}
  rails(world,x,z);world.save(true);
 }
 private static void rails(World w,int x,int z){for(int dx=-18;dx<=18;dx++)for(int dz=-14;dz<=14;dz++)if(Math.abs(dx)==18||Math.abs(dz)==14)for(int y=66;y<=67;y++)w.getBlockAt(x+dx,y,z+dz).setType(Material.LIGHT_BLUE_STAINED_GLASS,false);}

}
