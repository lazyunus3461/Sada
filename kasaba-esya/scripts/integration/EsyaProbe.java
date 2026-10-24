package tr.kasaba.test;
import com.google.gson.Gson;
import java.lang.reflect.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.hub.api.*;

/** Test-only adapter. Accepts exclusively loopback players; never packaged for distribution. */
public final class EsyaProbe extends JavaPlugin implements org.bukkit.plugin.messaging.PluginMessageListener {
 Object game; final Gson json=new com.google.gson.GsonBuilder().serializeNulls().create();
 public void onEnable(){game=Bukkit.getPluginManager().getPlugin("KasabaEsya");getServer().getMessenger().registerIncomingPluginChannel(this,"kasaba_test:esya",this);}
 public void onPluginMessageReceived(String channel,Player p,byte[] data){if(p.getAddress()==null||!p.getAddress().getAddress().isLoopbackAddress())return;String op=new String(data,StandardCharsets.UTF_8);Bukkit.getScheduler().runTask(this,()->{try{String[] a=op.split("\\|");Object room=((Map<?,?>)get(game,"membership")).get(p.getUniqueId());switch(a[0]){
 case "STATE"->{Map<String,Object>d=new LinkedHashMap<>();var hub=Bukkit.getServicesManager().load(HubApi.class);d.put("uuid",p.getUniqueId());d.put("world",p.getWorld().getName());d.put("location",List.of(p.getX(),p.getY(),p.getZ()));d.put("social",hub.socialArea(p.getUniqueId()));d.put("gameplay",hub.gameplay(p.getUniqueId()));d.put("visitor",((Set<?>)get(game,"visitors")).contains(p.getUniqueId()));d.put("queued",((Set<?>)get(game,"queue")).contains(p.getUniqueId()));d.put("mode",p.getGameMode());d.put("collidable",p.isCollidable());d.put("rooms",((Map<?,?>)get(game,"rooms")).size());d.put("privateWorlds",Bukkit.getWorlds().stream().map(World::getName).filter(n->n.startsWith("esya_room_")).toList());d.put("items",Arrays.stream(p.getInventory().getContents()).map(i->i==null?"AIR":i.getType().name()+":"+i.getAmount()).toList());d.put("slow",p.hasPotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS));d.put("visible",Bukkit.getOnlinePlayers().stream().filter(p::canSee).map(Player::getName).toList());d.put("backup",java.nio.file.Files.exists(((org.bukkit.plugin.Plugin)game).getDataFolder().toPath().resolve("recoveries/"+p.getUniqueId()+".yml")));d.put("room",room==null?null:get(room,"id"));if(room!=null){for(String f:List.of("phase","practice","hiders","seekers","firstSeekers","seconds"))d.put(f,get(room,f));d.put("map",get(get(room,"map"),"worldName"));d.put("decoys",((Set<?>)get(room,"activeDecoys")).size());d.put("bar",((org.bukkit.boss.BossBar)get(room,"bar")).getTitle());Map<String,Object> props=new LinkedHashMap<>();for(var e:((Map<UUID,Interaction>)get(room,"hitboxes")).entrySet()){var b=e.getValue();props.put(e.getKey().toString(),Map.of("id",b.getEntityId(),"valid",b.isValid(),"x",b.getX(),"y",b.getY(),"z",b.getZ(),"kind",((Map<?,?>)get(room,"disguises")).get(e.getKey()).toString()));}d.put("props",props);}say(p,"state:"+json.toJson(d));}
 case "LOBBY_INFO"->{World w=(World)get(game,"lobbyWorld");boolean floor=true,walls=true;for(int x=-20;x<=20;x++)for(int z=-16;z<=16;z++){floor &= w.getBlockAt(x,80,z).getType().isSolid();if(Math.abs(x)==20||Math.abs(z)==16)for(int y=81;y<=88;y++)walls &= !w.getBlockAt(x,y,z).isPassable();}Map<String,Object>d=new LinkedHashMap<>();d.put("world",w.getName());d.put("floor",floor);d.put("walls",walls);d.put("outside",w.getBlockAt(64,80,0).getType());d.put("editedFloor",w.getBlockAt(18,80,14).getType());d.put("templatesSeparate",((java.util.List<?>)get(game,"maps")).stream().allMatch(m->{try{return !get(m,"worldName").equals(w.getName());}catch(Exception e){throw new RuntimeException(e);}}));d.put("boards",w.getEntitiesByClass(TextDisplay.class).stream().filter(e->e.getScoreboardTags().contains("kasaba-board:esyasaklambaci/info")||e.getScoreboardTags().contains("kasaba-board:esyasaklambaci/top")).count());say(p,"lobby:"+json.toJson(d));}
 case "EDIT_LOBBY"->{World w=(World)get(game,"lobbyWorld");w.getBlockAt(18,80,14).setType(Material.EMERALD_BLOCK,false);}
 case "RELEASE"->{set(room,"startedAt",System.currentTimeMillis()-21000);call(room,"tick",System.currentTimeMillis());}
 case "TIMEOUT"->{set(room,"startedAt",System.currentTimeMillis()-270000);call(room,"tick",System.currentTimeMillis());}
 case "END"->{set(room,"finishAt",System.currentTimeMillis()-1);call(room,"tick",System.currentTimeMillis());}
 case "AGEQUEUE"->{set(game,"queueDeadline",System.currentTimeMillis()-1);call(game,"tick");}
 case "PAIR"->{Player hidden=Bukkit.getPlayerExact(a[1]);World w=p.getWorld();for(int x=-1;x<=5;x++)for(int z=-1;z<=1;z++){w.getBlockAt(x,64,z).setType(Material.STONE);for(int y=65;y<=68;y++)w.getBlockAt(x,y,z).setType(Material.AIR);}call(game,"teleport",p,new Location(w,.5,65,.5,-90,0));call(game,"teleport",hidden,new Location(w,3.5,65,.5,90,0));call(room,"follow",hidden,hidden.getLocation());p.setGravity(false);hidden.setGravity(false);}
 case "WALL"->{p.getWorld().getBlockAt(2,65,0).setType(Boolean.parseBoolean(a[1])?Material.STONE:Material.AIR);p.getWorld().getBlockAt(2,66,0).setType(Boolean.parseBoolean(a[1])?Material.STONE:Material.AIR);}
 case "GUESS"->{Player target=a.length>1?Bukkit.getPlayerExact(a[1]):null;call(room,"guess",p,target==null?null:target.getUniqueId());}
 case "READY"->{((Map<?,?>)get(room,"guessCooldown")).remove(p.getUniqueId());((Map<?,?>)get(room,"changeCooldown")).remove(p.getUniqueId());}
 case "GUARDS"->{var broken=new BlockBreakEvent(p.getLocation().clone().subtract(0,1,0).getBlock(),p);Bukkit.getPluginManager().callEvent(broken);var damage=new EntityDamageEvent(p,EntityDamageEvent.DamageCause.FALL,5);Bukkit.getPluginManager().callEvent(damage);var mode=new PlayerGameModeChangeEvent(p,GameMode.CREATIVE);Bukkit.getPluginManager().callEvent(mode);var fly=new PlayerToggleFlightEvent(p,true);Bukkit.getPluginManager().callEvent(fly);var tp=new PlayerTeleportEvent(p,p.getLocation(),Bukkit.getWorlds().getFirst().getSpawnLocation());Bukkit.getPluginManager().callEvent(tp);var move=new PlayerMoveEvent(p,p.getLocation(),p.getLocation().clone().add(200,0,0));Bukkit.getPluginManager().callEvent(move);var cmd=new PlayerCommandPreprocessEvent(p,"/give @s diamond");Bukkit.getPluginManager().callEvent(cmd);say(p,"guards:"+json.toJson(Map.of("break",broken.isCancelled(),"damage",damage.isCancelled(),"mode",mode.isCancelled(),"flight",fly.isCancelled(),"teleport",tp.isCancelled(),"move",move.isCancelled()||!move.getTo().equals(p.getLocation().clone().add(200,0,0)),"command",cmd.isCancelled())));}
 case "INV_COMPASS"->{var view=p.getOpenInventory();int slot=-1;for(int i=0;i<view.countSlots();i++)if(view.getInventory(i)==p.getInventory()&&view.convertSlot(i)==8){slot=i;break;}if(slot<0)throw new IllegalStateException("Hotbar slot missing");var event=new InventoryClickEvent(view,InventoryType.SlotType.QUICKBAR,slot,ClickType.RIGHT,InventoryAction.PICKUP_HALF);Bukkit.getPluginManager().callEvent(event);}
 case "CLOSE_ALL"->{for(Object r:List.copyOf(((Map<?,?>)get(game,"rooms")).values()))call(r,"close");}
 case "ACTIVE_SECONDS"->{Object social=Bukkit.getPluginManager().getPlugin("KasabaSosyal");Object activity=call(social,"activity",p.getUniqueId());say(p,"activity:"+json.toJson(activity));}
 default->throw new IllegalArgumentException(op);}
 say(p,"done:"+op);}catch(Throwable e){say(p,"error:"+e);getLogger().log(java.util.logging.Level.SEVERE,"Esya fixture",e);}});}
 Object get(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
 Object call(Object target,String name,Object... args)throws Exception{Method m=Arrays.stream(target.getClass().getDeclaredMethods()).filter(x->x.getName().equals(name)&&x.getParameterCount()==args.length).filter(x->{for(int i=0;i<args.length;i++)if(args[i]!=null&&!x.getParameterTypes()[i].isPrimitive()&&!x.getParameterTypes()[i].isInstance(args[i]))return false;return true;}).findFirst().orElseThrow();m.setAccessible(true);return m.invoke(target,args);}
 void say(Player p,String text){p.sendMessage(net.kyori.adventure.text.Component.text("esya-probe:"+text));}
}
