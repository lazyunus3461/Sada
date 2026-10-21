package tr.kasaba.test;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.hub.api.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Test adapter and fixture; loopback only, never shipped in the production JAR. */
public final class HubProbe extends JavaPlugin {
    private HubApi api;private boolean accept;private int calls,teamSize;private final Set<UUID> inside=new HashSet<>();
    private final HubGame game=new HubGame(){
        public GameInfo info(){return new GameInfo("testoyun","Yerel Test","Yalnızca entegrasyon testi",2,4);}
        public boolean available(){return true;}
        public boolean join(List<Player> team){calls++;teamSize=team.size();if(!accept)return false;team.forEach(p->inside.add(p.getUniqueId()));return true;}
        public void leave(UUID id){inside.remove(id);}
    };
    @Override public void onEnable(){
        if(!getServer().getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback required");api=Bukkit.getServicesManager().load(HubApi.class);Objects.requireNonNull(api);
        Bukkit.getMessenger().registerIncomingPluginChannel(this,"kasaba_test:hub",(channel,p,data)->{
            if(data.length>1024)throw new IllegalArgumentException("Fixture request too large");String op=new String(data,StandardCharsets.UTF_8);
            try{
                if(op.startsWith("PLAYER|")){String cmd=op.substring(7);if(!cmd.startsWith("grup ")&&!cmd.startsWith("merkez ")&&!cmd.equals("lobi"))throw new IllegalArgumentException("Fixture command forbidden");Bukkit.dispatchCommand(p,cmd);}
                else switch(op){
                    case "ADMIN" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"rol ver "+p.getName()+" kurucu");
                    case "LOBBY" -> p.teleport(new Location(Bukkit.getWorld("world"),35.5,-60,35.5,123,-12));
                    case "AWAY" -> p.teleport(new Location(Bukkit.getWorld("world"),500.5,-60,500.5,0,0));
                    case "REGISTER" -> api.register(this,game);
                    case "ACCEPT" -> accept=true;
                    case "UNREGISTER" -> api.unregister(this,"testoyun");
                    case "STATUS" -> say(p,"status:playing="+api.playing(p.getUniqueId())+",calls="+calls+",size="+teamSize+",inside="+inside.size());
                    case "ITEM" -> p.getInventory().setItem(0,new org.bukkit.inventory.ItemStack(Material.DIAMOND,7));
                    case "ITEM_CHECK" -> {var item=p.getInventory().getItem(0);say(p,"item="+(item!=null&&item.getType()==Material.DIAMOND&&item.getAmount()==7));}
                    case "ACCESS" -> {
                        p.teleport(new Location(Bukkit.getWorld("world"),35.5,-60,35.5,123,-12));
                        BlockBreakEvent in=new BlockBreakEvent(p.getWorld().getBlockAt(36,-61,35),p),out=new BlockBreakEvent(p.getWorld().getBlockAt(500,-61,500),p);
                        EntityDamageEvent damage=new EntityDamageEvent(p,EntityDamageEvent.DamageCause.FALL,1);FoodLevelChangeEvent hunger=new FoodLevelChangeEvent(p,10);
                        for(var event:List.of(in,out,damage,hunger))Bukkit.getPluginManager().callEvent(event);
                        say(p,"access:inside="+in.isCancelled()+",outside="+out.isCancelled()+",damage="+damage.isCancelled()+",hunger="+hunger.isCancelled());
                    }
                    default -> throw new IllegalArgumentException("Unknown fixture operation");
                }
                say(p,"done:"+op);
            }catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"Hub fixture failed",e);say(p,"error:"+e.getClass().getSimpleName()+":"+e.getMessage());}
        });
    }
    private void say(Player p,String text){p.sendMessage(Component.text("hub-probe:"+text));}
}
