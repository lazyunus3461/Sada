package tr.kasaba.test;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import tr.kasaba.hub.api.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Test adapter and fixture; loopback only, never shipped in the production JAR. */
public final class HubProbe extends JavaPlugin implements Listener {
    private HubApi api;private boolean accept;private int calls,teamSize;private final Set<UUID> inside=new HashSet<>();
    private final Set<UUID> veto=new HashSet<>();
    private boolean menu(ItemStack item){return item!=null&&item.hasItemMeta()&&item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey("kasabahub","lobi-menusu"));}
    @EventHandler(priority=EventPriority.HIGHEST) public void teleport(PlayerTeleportEvent e){if(veto.contains(e.getPlayer().getUniqueId())&&e.getTo().getX()==500.5)e.setCancelled(true);}
    private final HubGame game=new HubGame(){
        public GameInfo info(){return new GameInfo("testoyun","Yerel Test","Yalnızca entegrasyon testi",2,4);}
        public boolean available(){return true;}
        public boolean join(List<Player> team){
            if(team.stream().anyMatch(p->Arrays.stream(p.getInventory().getContents()).anyMatch(HubProbe.this::menu)))throw new IllegalStateException("Temporary menu leaked into game adapter inventory snapshot");
            calls++;teamSize=team.size();if(!accept)return false;team.forEach(p->inside.add(p.getUniqueId()));return true;
        }
        public void leave(UUID id){inside.remove(id);}
    };
    @Override public void onEnable(){
        if(!getServer().getIp().equals("127.0.0.1"))throw new IllegalStateException("Loopback required");api=Bukkit.getServicesManager().load(HubApi.class);Objects.requireNonNull(api);
        Bukkit.getPluginManager().registerEvents(this,this);
        Bukkit.getMessenger().registerIncomingPluginChannel(this,"kasaba_test:hub",(channel,p,data)->{
            if(data.length>1024)throw new IllegalArgumentException("Fixture request too large");String op=new String(data,StandardCharsets.UTF_8);
            try{
                if(op.startsWith("PLAYER|")){String cmd=op.substring(7);if(!cmd.startsWith("grup ")&&!cmd.startsWith("merkez ")&&!cmd.startsWith("oyunlar ")&&!cmd.equals("lobi"))throw new IllegalArgumentException("Fixture command forbidden");Bukkit.dispatchCommand(p,cmd);}
                else switch(op){
                    case "ADMIN" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"rol ver "+p.getName()+" kurucu");
                    case "LOBBY" -> p.teleport(new Location(Bukkit.getWorld("world"),35.5,-60,35.5,123,-12));
                    case "AWAY" -> p.teleport(new Location(Bukkit.getWorld("world"),500.5,-60,500.5,0,0));
                    case "VETO" -> veto.add(p.getUniqueId());
                    case "ALLOW" -> veto.remove(p.getUniqueId());
                    case "REGISTER" -> api.register(this,game);
                    case "ACCEPT" -> accept=true;
                    case "UNREGISTER" -> api.unregister(this,"testoyun");
                    case "STATUS" -> say(p,"status:playing="+api.playing(p.getUniqueId())+",calls="+calls+",size="+teamSize+",inside="+inside.size());
                    case "ITEM" -> p.getInventory().setItem(0,new org.bukkit.inventory.ItemStack(Material.DIAMOND,7));
                    case "ITEM_CHECK" -> {var item=p.getInventory().getItem(0);say(p,"item="+(item!=null&&item.getType()==Material.DIAMOND&&item.getAmount()==7));}
                    case "MENU" -> {
                        int count=0,slot=-1;for(int i=0;i<p.getInventory().getSize();i++)if(menu(p.getInventory().getItem(i))&&p.getInventory().getItem(i).getType()==Material.COMPASS){count+=p.getInventory().getItem(i).getAmount();slot=i;}
                        say(p,"menu:count="+count+",slot="+slot+",normal="+(p.getInventory().getItem(1)!=null&&p.getInventory().getItem(1).getType()==Material.COMPASS&&!menu(p.getInventory().getItem(1))));
                    }
                    case "GUIDE" -> {
                        int count=0;ItemStack book=null;for(ItemStack item:p.getInventory().getContents())if(menu(item)&&item.getType()==Material.WRITTEN_BOOK){count++;book=item;}
                        String pages=book!=null?((org.bukkit.inventory.meta.BookMeta)book.getItemMeta()).pages().toString():"";
                        say(p,"guide:count="+count+",pages="+(book==null?0:((org.bukkit.inventory.meta.BookMeta)book.getItemMeta()).getPageCount())+",arena="+pages.contains("/arena 1v1")+",xarena="+pages.contains("/arena xarena")+",ada="+pages.contains("/ada al")+",akce="+pages.contains("/akçe"));
                    }
                    case "NORMAL_COMPASS" -> p.getInventory().setItem(1,new ItemStack(Material.COMPASS));
                    case "FULL" -> {for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Material.DIAMOND,64));}
                    case "FULL_CHECK" -> {boolean full=true;for(int i=0;i<36;i++){var item=p.getInventory().getItem(i);full&=item!=null&&item.getType()==Material.DIAMOND&&item.getAmount()==64;}say(p,"full="+full);}
                    case "FREE_SLOT" -> p.getInventory().setItem(8,null);
                    case "MENU_GUARDS" -> {
                        ItemStack item=p.getInventory().getItem(8);if(!menu(item))throw new IllegalStateException("Menu missing");
                        InventoryClickEvent click=new InventoryClickEvent(p.getOpenInventory(),InventoryType.SlotType.QUICKBAR,44,ClickType.SHIFT_LEFT,InventoryAction.MOVE_TO_OTHER_INVENTORY);
                        InventoryClickEvent number=new InventoryClickEvent(p.getOpenInventory(),InventoryType.SlotType.QUICKBAR,36,ClickType.NUMBER_KEY,InventoryAction.HOTBAR_SWAP,8);
                        var entity=p.getWorld().dropItem(p.getLocation(),item.clone());PlayerDropItemEvent drop=new PlayerDropItemEvent(p,entity);
                        PlayerSwapHandItemsEvent swap=new PlayerSwapHandItemsEvent(p,new ItemStack(Material.AIR),item.clone());
                        for(var event:List.of(click,number,drop,swap))Bukkit.getPluginManager().callEvent(event);entity.remove();
                        say(p,"guards:click="+click.isCancelled()+",number="+number.isCancelled()+",drop="+drop.isCancelled()+",swap="+swap.isCancelled());
                    }
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
