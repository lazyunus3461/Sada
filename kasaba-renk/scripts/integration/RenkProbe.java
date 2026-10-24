package tr.kasaba.test;

import com.google.gson.Gson;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.hub.api.HubApi;

/** Loopback fixture only. Never distribute. */
public final class RenkProbe extends JavaPlugin {
    JavaPlugin game;Gson json=new Gson();
    public void onEnable(){if(!Bukkit.getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback only");game=(JavaPlugin)Bukkit.getPluginManager().getPlugin("KasabaRenk");Bukkit.getMessenger().registerIncomingPluginChannel(this,"kasaba_test:renk",(channel,p,data)->{if(data.length>1024||p.getAddress()==null||!p.getAddress().getAddress().isLoopbackAddress())return;String op=new String(data,StandardCharsets.UTF_8);try{Object room=((Map<?,?>)field(game,"membership")).get(p.getUniqueId());switch(op){
        case "STATE"->{Map<String,Object> s=new LinkedHashMap<>();HubApi hub=Bukkit.getServicesManager().load(HubApi.class);s.put("uuid",p.getUniqueId());s.put("world",p.getWorld().getName());s.put("mode",p.getGameMode());s.put("social",hub.socialArea(p.getUniqueId()));s.put("gameplay",hub.gameplay(p.getUniqueId()));s.put("room",room==null?0:field(room,"id"));s.put("phase",room==null?"LOBBY":field(room,"phase"));s.put("round",room==null?0:field(room,"round"));s.put("target",room==null?0:field(room,"target"));s.put("lives",room==null?Map.of():field(room,"lives"));s.put("passes",room==null?Map.of():field(room,"passes"));s.put("rooms",((Map<?,?>)field(game,"rooms")).size());s.put("items",Arrays.stream(p.getInventory().getContents()).map(i->i==null?"AIR":i.getType()+":"+i.getAmount()).toList());s.put("backup",new java.io.File(game.getDataFolder(),"recoveries/"+p.getUniqueId()+".yml").isFile());s.put("gravity",p.hasGravity());if(room!=null){int origin=(int)call(room,"origin"),air=0;World w=(World)field(game,"world");for(int x=0;x<32;x++)for(int z=0;z<32;z++)if(w.getBlockAt(origin+x,80,z-16).getType().isAir())air++;s.put("floorAir",air);}say(p,"state:"+json.toJson(s));}
        case "SAFE","WRONG"->{int[] cells=(int[])field(room,"cells");int target=(int)field(room,"target"),index=0;while((cells[index]==target)!=op.equals("SAFE"))index++;int origin=(int)call(room,"origin");call(game,"teleport",p,new Location((World)field(game,"world"),origin+(int)call(room,"offset")+(index%8)*(int)call(room,"cellSize")+.5,81,(int)call(room,"offset")+(index/8)*(int)call(room,"cellSize")-15.5));}
        case "EXPIRE"->{Field f=room.getClass().getDeclaredField("deadline");f.setAccessible(true);f.setLong(room,System.currentTimeMillis()-1);call(room,"tick",System.currentTimeMillis());}
        case "GUARDS"->{BlockBreakEvent broken=new BlockBreakEvent(p.getLocation().clone().add(0,-1,0).getBlock(),p);Bukkit.getPluginManager().callEvent(broken);EntityDamageEvent hurt=new EntityDamageEvent(p,EntityDamageEvent.DamageCause.FALL,5);Bukkit.getPluginManager().callEvent(hurt);say(p,"guards:"+(broken.isCancelled()&&hurt.isCancelled()));}
        default->throw new IllegalArgumentException(op);}say(p,"done:"+op);}catch(Throwable e){say(p,"error:"+e);getLogger().log(java.util.logging.Level.SEVERE,"Fixture failed",e);}});}
    Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    Object call(Object o,String name,Object... args)throws Exception{Method m=Arrays.stream(o.getClass().getDeclaredMethods()).filter(x->x.getName().equals(name)&&x.getParameterCount()==args.length).filter(x->{for(int i=0;i<args.length;i++)if(args[i]!=null&&!x.getParameterTypes()[i].isPrimitive()&&!x.getParameterTypes()[i].isInstance(args[i]))return false;return true;}).findFirst().orElseThrow();m.setAccessible(true);return m.invoke(o,args);}
    void say(Player p,String text){p.sendMessage(net.kyori.adventure.text.Component.text("renk-probe:"+text));}
}
