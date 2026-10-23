package tr.kasabatr.islands;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

/** A reset stays claimed and inaccessible until its final block is restored. */
final class IslandResetter {
 private final KasabaIslandsPlugin plugin;private final ClaimStore claims;private final IslandLayout layout;private final Supplier<World> world;
 private final BlockData stone=Material.STONE.createBlockData(),bedrock=Material.BEDROCK.createBlockData();
 private BlockData[] palette;private Job job;private boolean preparing;private final List<Chunk> tickets=new ArrayList<>();
 private static final class Job{final int id,minY,height;final char[] blocks;int cursor;Job(int id,int minY,int height,char[] blocks){this.id=id;this.minY=minY;this.height=height;this.blocks=blocks;}}
 IslandResetter(KasabaIslandsPlugin plugin,ClaimStore claims,IslandLayout layout,Supplier<World> world){this.plugin=plugin;this.claims=claims;this.layout=layout;this.world=world;}
 void sweep(){World w=world.get();if(w==null)return;long now=System.currentTimeMillis();try{for(UUID owner:claims.owners()){Player p=Bukkit.getPlayer(owner);if(p!=null&&plugin.authenticated(p)){claims.seen(owner,now);continue;}if(claims.lastSeen(owner)>0&&now-claims.lastSeen(owner)>=ClaimStore.INACTIVE)for(int id:claims.owned(owner))if(!claims.resetting(id)){claims.beginReset(id);plugin.getLogger().info("15 gün giriş yapılmadığı için ada sıfırlama kuyruğuna alındı: #"+id);}}}catch(IOException e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Ada sıfırlama kuyruğu korunuyor.",e);}}
 void tick(){World w=world.get();if(w==null)return;if(job==null){if(!preparing&&!claims.resets().isEmpty())prepare(w,claims.resets().iterator().next());return;}long deadline=System.nanoTime()+4_000_000L;int volume=96*96*(w.getMaxHeight()-w.getMinHeight()),done=0;while(job.cursor<volume&&done<8192&&System.nanoTime()<deadline){int value=job.cursor++,column=value%(96*96),dy=value/(96*96),x=column%96,z=column/96,y=w.getMinHeight()+dy;BlockData expected;if(y>=job.minY&&y<job.minY+job.height)expected=palette[job.blocks[(y-job.minY)*96*96+column]];else if(y==w.getMinHeight())expected=bedrock;else if(y<58)expected=stone;else expected=palette[0];var block=w.getBlockAt(layout.centerX(job.id)-48+x,y,layout.centerZ(job.id)-48+z);if(block.getType()!=expected.getMaterial()||y>=job.minY&&y<job.minY+job.height&&!block.getBlockData().matches(expected))block.setBlockData(expected,false);done++;}if(job.cursor>=volume)try{int id=job.id;w.save(true);claims.finishReset(id);plugin.getLogger().info("Ada temiz başlangıç haline getirildi ve yeniden satışa açıldı: #"+id);job=null;release();}catch(IOException e){plugin.getLogger().log(java.util.logging.Level.SEVERE,"Temizlenen ada serbest bırakılamadı; kaydı korunuyor.",e);}}
 private void prepare(World w,int id){preparing=true;try{if(palette==null)try(var in=new BufferedReader(new InputStreamReader(Objects.requireNonNull(plugin.getResource("reset-template/palette.txt")),StandardCharsets.UTF_8))){palette=in.lines().map(Bukkit::createBlockData).toArray(BlockData[]::new);}
  Job next;try(var in=new DataInputStream(new GZIPInputStream(Objects.requireNonNull(plugin.getResource("reset-template/"+id+".bin.gz"))))){if(in.readInt()!=0x4b415331)throw new IOException("Ada başlangıç verisi bozuk.");int min=in.readInt(),height=in.readInt();if(min!=58||height!=40)throw new IOException("Ada başlangıç boyutu bozuk.");char[] blocks=new char[height*96*96];for(int i=0;i<blocks.length;i++){blocks[i]=in.readChar();if(blocks[i]>=palette.length)throw new IOException("Ada blok paleti bozuk.");}next=new Job(id,min,height,blocks);}
  List<CompletableFuture<Chunk>> futures=new ArrayList<>();for(int x=(layout.centerX(id)-48)>>4;x<=(layout.centerX(id)+47)>>4;x++)for(int z=(layout.centerZ(id)-48)>>4;z<=(layout.centerZ(id)+47)>>4;z++)futures.add(w.getChunkAtAsync(x,z,true));
  CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).whenComplete((ignored,error)->{if(!plugin.isEnabled())return;Bukkit.getScheduler().runTask(plugin,()->{preparing=false;if(error!=null){plugin.getLogger().warning("Ada sıfırlama bölgesi yüklenemedi; yeniden denenecek.");return;}for(var future:futures){Chunk chunk=future.join();chunk.addPluginChunkTicket(plugin);tickets.add(chunk);for(var state:chunk.getTileEntities())if(layout.contains(id,state.getX(),state.getZ()))state.getBlock().setType(Material.AIR,false);for(var entity:chunk.getEntities())if(!(entity instanceof Player)&&layout.contains(id,entity.getLocation().getBlockX(),entity.getLocation().getBlockZ()))entity.remove();}for(Player p:w.getPlayers())if(layout.contains(id,p.getLocation().getBlockX(),p.getLocation().getBlockZ())){var hub=Bukkit.getServicesManager().load(tr.kasaba.hub.api.HubApi.class);if(hub!=null)hub.returnToLobby(p.getUniqueId());}job=next;});});
 }catch(Exception e){preparing=false;plugin.getLogger().log(java.util.logging.Level.SEVERE,"Ada başlangıç verisi açılamadı; sahiplik korunuyor.",e);}}
 private void release(){for(Chunk chunk:tickets)chunk.removePluginChunkTicket(plugin);tickets.clear();}
 void close(){release();}
}
