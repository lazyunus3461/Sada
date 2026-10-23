package tr.kasaba.test;

import com.google.gson.Gson;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.hub.api.*;

/** Loopback-only server event adapter; never included in the production JAR. */
public final class IspanyaProbe extends JavaPlugin implements Listener {
    JavaPlugin game;final Gson json=new Gson();UUID cancel;boolean failPublic;
    public void onEnable(){if(!Bukkit.getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback only");game=(JavaPlugin)Bukkit.getPluginManager().getPlugin("KasabaIspanya");Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getMessenger().registerIncomingPluginChannel(this,"kasaba_test:ispanya",(channel,p,data)->{
        if(data.length>2048||p.getAddress()==null||!p.getAddress().getAddress().isLoopbackAddress())return;
        String op=new String(data,StandardCharsets.UTF_8);String[] a=op.split("\\|",-1);
        try{Object room=membership().get(p.getUniqueId());switch(a[0]){
            case "STATE"->state(p);
            case "DRAIN"->{for(int i=0;i<2000&&field(room,"movingId")!=null;i++)call(room,"moveStep");if(field(room,"movingId")!=null)throw new IllegalStateException("Animation stuck");}
            case "POS"->{int index=Integer.parseInt(a[1]);((Map<UUID,Integer>)field(room,"positions")).put(p.getUniqueId(),index);Object track=call(game,"track"),tile=call(track,"tile",index);call(game,"teleport",p,call(tile,"location",(World)field(room,"world")));}
            case "SEED"->{int sum=Integer.parseInt(a[1]);boolean luck=a.length<3||Boolean.parseBoolean(a[2]);long seed;for(seed=0;;seed++){Random r=new Random(seed);if(r.nextInt(6)+r.nextInt(6)+2==sum&&(a.length<3||r.nextBoolean()==luck))break;}((Random)field(room,"random")).setSeed(seed);}
            case "TIMEOUT"->{set(room,"turnLeft",1);call(room,"tick");}
            case "GUARDS"->{Location old=p.getLocation();PlayerMoveEvent move=new PlayerMoveEvent(p,old,old.clone().add(2,0,0));Bukkit.getPluginManager().callEvent(move);EntityDamageEvent damage=new EntityDamageEvent(p,EntityDamageEvent.DamageCause.FALL,5);Bukkit.getPluginManager().callEvent(damage);FoodLevelChangeEvent food=new FoodLevelChangeEvent(p,1);Bukkit.getPluginManager().callEvent(food);BlockBreakEvent broken=new BlockBreakEvent(old.clone().add(0,-1,0).getBlock(),p);Bukkit.getPluginManager().callEvent(broken);PlayerSwapHandItemsEvent swap=new PlayerSwapHandItemsEvent(p,new ItemStack(Material.STONE),new ItemStack(Material.DIAMOND));Bukkit.getPluginManager().callEvent(swap);InventoryClickEvent click=new InventoryClickEvent(p.getOpenInventory(),InventoryType.SlotType.CONTAINER,36,ClickType.LEFT,InventoryAction.PICKUP_ALL);Bukkit.getPluginManager().callEvent(click);say(p,"guards:"+json.toJson(Map.of("move",move.isCancelled(),"damage",damage.isCancelled(),"food",food.isCancelled(),"break",broken.isCancelled(),"swap",swap.isCancelled(),"inventory",click.isCancelled())));}
            case "FAIL"->{cancel=Bukkit.getPlayerExact(a[1]).getUniqueId();failPublic=Boolean.parseBoolean(a[2]);}
            case "COMPASS"->{ItemStack compass=p.getInventory().getItem(8);Bukkit.getPluginManager().callEvent(new PlayerInteractEvent(p,Action.RIGHT_CLICK_AIR,compass,null,org.bukkit.block.BlockFace.UP,EquipmentSlot.HAND));}
            case "VOID"->{World w=p.getWorld();var chunk=w.getChunkAt(12,12);boolean air=true;for(int y=w.getMinHeight();y<w.getMaxHeight();y++)if(!chunk.getBlock(0,y,0).getType().isAir())air=false;say(p,"void:"+air);}
            default->throw new IllegalArgumentException("Unknown operation");}say(p,"done:"+op);
        }catch(Throwable e){say(p,"error:"+e);getLogger().log(java.util.logging.Level.SEVERE,"Ispanya fixture failed",e);}
    });}
    @EventHandler(priority=EventPriority.HIGHEST) public void fail(PlayerTeleportEvent e){if(cancel!=null&&cancel.equals(e.getPlayer().getUniqueId())&&e.getTo()!=null&&(failPublic?e.getTo().getWorld().getName().equals("ispanya"):e.getTo().getWorld().getName().startsWith("isp_room_"))){cancel=null;e.setCancelled(true);}}
    Map<UUID,Object> membership()throws Exception{return (Map<UUID,Object>)field(game,"membership");}
    Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    Object call(Object target,String name,Object... args)throws Exception{Method m=Arrays.stream(target.getClass().getDeclaredMethods()).filter(x->x.getName().equals(name)&&x.getParameterCount()==args.length).filter(x->{Class<?>[] types=x.getParameterTypes();for(int i=0;i<types.length;i++)if(args[i]!=null&&!types[i].isPrimitive()&&!types[i].isInstance(args[i]))return false;return true;}).findFirst().orElseThrow();m.setAccessible(true);return m.invoke(target,args);}
    void state(Player p)throws Exception{
        Map<String,Object> d=new LinkedHashMap<>();HubApi hub=Bukkit.getServicesManager().load(HubApi.class);Object room=membership().get(p.getUniqueId());
        d.put("world",p.getWorld().getName());d.put("mode",p.getGameMode().name());d.put("social",hub.socialArea(p.getUniqueId()));d.put("gameplay",hub.gameplay(p.getUniqueId()));d.put("playing",hub.playing(p.getUniqueId()));d.put("session",((Map<?,?>)field(game,"sessions")).containsKey(p.getUniqueId()));d.put("items",Arrays.stream(p.getInventory().getContents()).map(i->i==null?"AIR":i.getType()+":"+i.getAmount()).toList());d.put("gravity",p.hasGravity());d.put("flight",p.getAllowFlight());d.put("absorption",p.getAbsorptionAmount());d.put("xp",p.getLevel());
        d.put("room",room==null?null:field(room,"id"));d.put("active",room!=null&&Boolean.TRUE.equals(field(room,"active")));d.put("current",room==null?null:field(room,"current"));d.put("uuid",p.getUniqueId());d.put("moving",room==null?null:field(room,"movingId"));d.put("positions",room==null?null:field(room,"positions"));d.put("skips",room==null?null:field(room,"skippedTurns"));d.put("finished",room==null?null:field(room,"finished"));d.put("owner",room==null?null:field(room,"owner"));d.put("rooms",((Map<?,?>)field(game,"rooms")).size());d.put("worlds",Bukkit.getWorlds().stream().map(World::getName).filter(n->n.startsWith("isp_room_")).toList());d.put("backup",new java.io.File(game.getDataFolder(),"recoveries/"+p.getUniqueId()+".yml").isFile());say(p,"state:"+json.toJson(d));
    }
    void say(Player p,String text){p.sendMessage(net.kyori.adventure.text.Component.text("ispanya-probe:"+text));}
}
