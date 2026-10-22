package tr.kasaba.test;
import com.google.gson.Gson;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.*;
import java.util.*;

/** Loopback fixture, never shipped to production. */
public final class IslandProbe extends JavaPlugin {
 private JavaPlugin islands;private final Gson gson=new Gson();
 public void onEnable(){if(!Bukkit.getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback required");islands=(JavaPlugin)Bukkit.getPluginManager().getPlugin("KasabaAdalar");Bukkit.getMessenger().registerIncomingPluginChannel(this,"kasaba_test:islands",(channel,p,bytes)->{if(bytes.length>1024||p.getAddress()==null||!p.getAddress().getAddress().isLoopbackAddress())return;String op=new String(bytes,StandardCharsets.UTF_8);try{String[] a=op.split("\\|");switch(a[0]){
 case "PLAYER"->{String cmd=op.substring(7);if(!cmd.startsWith("ada")&&!cmd.startsWith("arena")&&!cmd.startsWith("oyunlar")&&!cmd.startsWith("grup")&&!cmd.equals("lobi")&&!cmd.equals("rehber"))throw new IllegalArgumentException("Forbidden fixture command");Bukkit.dispatchCommand(p,cmd);}
 case "USER"->p.setOp(false);
 case "ADMIN"->p.setOp(true);
 case "ITEM"->p.getInventory().setItem(0,new ItemStack(Material.valueOf(a[1]),Integer.parseInt(a[2])));
 case "STATE"->state(p);
 case "BREAK"->{BlockBreakEvent event=new BlockBreakEvent(p.getLocation().clone().subtract(0,1,0).getBlock(),p);Bukkit.getPluginManager().callEvent(event);say(p,"break="+event.isCancelled());}
 case "PLACE"->{var b=p.getLocation().getBlock();BlockPlaceEvent event=new BlockPlaceEvent(b,b.getState(),b.getRelative(0,-1,0),new ItemStack(Material.STONE),p,true,EquipmentSlot.HAND);Bukkit.getPluginManager().callEvent(event);say(p,"place="+event.isCancelled());}
 case "TO"->{World world=Bukkit.getWorld("ada");say(p,"teleported="+p.teleport(new Location(world,Double.parseDouble(a[1]),66,Double.parseDouble(a[2]))));}
 case "FLOOR"->{World world=Bukkit.getWorld("ada");boolean solid=true;for(int y=-64;y<=58;y++)solid&=world.getBlockAt(Integer.parseInt(a[1]),y,Integer.parseInt(a[2])).getType().isSolid();say(p,"floor="+solid);}
 case "GUIDE_GUARDS"->{ItemStack item=p.getInventory().getItem(7);if(item==null||item.getType()!=Material.WRITTEN_BOOK)throw new IllegalStateException("Guide missing");var click=new org.bukkit.event.inventory.InventoryClickEvent(p.getOpenInventory(),org.bukkit.event.inventory.InventoryType.SlotType.QUICKBAR,43,org.bukkit.event.inventory.ClickType.SHIFT_LEFT,org.bukkit.event.inventory.InventoryAction.MOVE_TO_OTHER_INVENTORY);Bukkit.getPluginManager().callEvent(click);say(p,"guideProtected="+click.isCancelled());}
 default->throw new IllegalArgumentException("Unknown fixture operation");}say(p,"done:"+op);}catch(Throwable error){say(p,"error:"+error);getLogger().log(java.util.logging.Level.SEVERE,"Fixture failed",error);}});}
 private Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
 private Object call(Object target,String name,Object... args)throws Exception{Method method=Arrays.stream(target.getClass().getDeclaredMethods()).filter(m->m.getName().equals(name)&&m.getParameterCount()==args.length).findFirst().orElseThrow();method.setAccessible(true);return method.invoke(target,args);}
 private void state(Player p)throws Exception{Object claims=field(islands,"claims");Map<String,Object> data=new LinkedHashMap<>();data.put("island",call(claims,"islandOf",p.getUniqueId()));data.put("total",call(claims,"size"));data.put("mode",p.getGameMode().name());data.put("world",p.getWorld().getName());data.put("x",p.getX());data.put("z",p.getZ());data.put("items",Arrays.stream(p.getInventory().getContents()).filter(Objects::nonNull).filter(i->!i.getType().isAir()).map(i->i.getType()+":"+i.getAmount()).toList());int menu=0,book=0,pages=0;String text="";for(ItemStack item:p.getInventory().getContents())if(item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("kasabahub","lobi-menusu"))){menu++;if(item.getItemMeta() instanceof BookMeta meta){book++;pages=meta.getPageCount();text=meta.pages().toString();}}data.put("temporary",menu);data.put("books",book);data.put("pages",pages);data.put("bookText",text);say(p,"state:"+gson.toJson(data));}
 private void say(Player p,String message){p.sendMessage(net.kyori.adventure.text.Component.text("island-probe:"+message));}
}
