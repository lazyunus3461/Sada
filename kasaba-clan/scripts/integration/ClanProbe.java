package tr.kasaba.test;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Loopback-only event fixture, never included in production artifacts. */
public final class ClanProbe extends JavaPlugin {
    private byte[] backup;
    @Override public void onEnable(){
        if(!getServer().getIp().equals("127.0.0.1"))throw new IllegalStateException("ClanProbe requires loopback");
        getServer().getMessenger().registerIncomingPluginChannel(this,"kasaba_test:clan",(channel,p,data)->{
            if(data.length>1024)throw new IllegalArgumentException("Oversized fixture message");
            String operation=new String(data,StandardCharsets.UTF_8);String[] args=operation.split("\\|",-1);
            try{
                switch(args[0]){
                    case "ADMIN" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"rol ver "+p.getName()+" kurucu");
                    case "CONSOLE","PLAYER" -> {
                        String command=operation.substring(operation.indexOf('|')+1);
                        if(!command.startsWith("klan ")&&!command.startsWith("warp ")&&!command.startsWith("rol "))throw new IllegalArgumentException("Fixture command forbidden");
                        Bukkit.dispatchCommand(args[0].equals("CONSOLE")?Bukkit.getConsoleSender():p,command);
                    }
                    case "POS" -> p.teleport(new Location(Bukkit.getWorld("world"),Double.parseDouble(args[1]),-60,Double.parseDouble(args[2]),0,0));
                    case "MODE" -> p.setGameMode(GameMode.valueOf(args[1]));
                    case "STONE" -> Bukkit.getWorld("world").getBlockAt(202,-60,202).setType(Material.STONE,false);
                    case "BLOCK" -> say(p,"block="+Bukkit.getWorld("world").getBlockAt(202,-60,202).getType());
                    case "ACCESS" -> access(p);
                    case "AUTOMATION" -> automation(p);
                    case "CORRUPT" -> {
                        Path path=Bukkit.getPluginManager().getPlugin("KasabaKlan").getDataFolder().toPath().resolve("klanlar.yml");backup=Files.readAllBytes(path);Files.writeString(path,"surum: [");Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"klan yenile");
                    }
                    case "RESTORE" -> {
                        if(backup==null)throw new IllegalStateException("No fixture backup");
                        Path path=Bukkit.getPluginManager().getPlugin("KasabaKlan").getDataFolder().toPath().resolve("klanlar.yml");Files.write(path,backup);backup=null;Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"klan yenile");
                    }
                    default -> throw new IllegalArgumentException("Unknown fixture operation");
                }
                say(p,"done:"+operation);
            }catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"Clan fixture failed",e);say(p,"error:"+e.getClass().getSimpleName()+":"+e.getMessage());}
        });
    }
    private void say(Player p,String value){p.sendMessage(Component.text("clan-probe:"+value));}
    private void call(Event e){Bukkit.getPluginManager().callEvent(e);}
    private void access(Player p){
        World world=Bukkit.getWorld("world");Block block=world.getBlockAt(203,-60,203);block.setType(Material.STONE,false);
        BlockBreakEvent broken=new BlockBreakEvent(block,p);call(broken);
        BlockPlaceEvent placed=new BlockPlaceEvent(block,block.getState(),block.getRelative(BlockFace.DOWN),new ItemStack(Material.STONE),p,true,EquipmentSlot.HAND);call(placed);
        PlayerBucketEmptyEvent bucket=new PlayerBucketEmptyEvent(p,block,world.getBlockAt(199,-60,203),BlockFace.EAST,Material.WATER_BUCKET,new ItemStack(Material.BUCKET),EquipmentSlot.HAND);call(bucket);
        Block high=world.getBlockAt(203,90,203);BlockBreakEvent vertical=new BlockBreakEvent(high,p);call(vertical);
        Block chest=world.getBlockAt(204,-60,204);chest.setType(Material.CHEST,false);
        PlayerInteractEvent interact=new PlayerInteractEvent(p,Action.RIGHT_CLICK_BLOCK,null,chest,BlockFace.UP,EquipmentSlot.HAND);call(interact);
        ArmorStand stand=world.spawn(new Location(world,205.5,-60,205.5),ArmorStand.class);stand.setGravity(false);
        EntityDamageByEntityEvent damage=new EntityDamageByEntityEvent(p,stand,EntityDamageEvent.DamageCause.ENTITY_ATTACK,1);call(damage);stand.remove();
        Player other=Bukkit.getOnlinePlayers().stream().filter(x->!x.equals(p)).findFirst().orElseThrow();
        EntityDamageByEntityEvent pvp=new EntityDamageByEntityEvent(p,other,EntityDamageEvent.DamageCause.ENTITY_ATTACK,1);call(pvp);
        say(p,"access:break="+broken.isCancelled()+",place="+placed.isCancelled()+",bucket="+bucket.isCancelled()+",high="+vertical.isCancelled()+",chest="+(interact.useInteractedBlock()==Event.Result.DENY)+",entity="+damage.isCancelled()+",pvp="+pvp.isCancelled());
    }
    private void automation(Player p){
        World w=Bukkit.getWorld("world");Block outside=w.getBlockAt(199,-60,205),inside=w.getBlockAt(200,-60,205),middle=w.getBlockAt(201,-60,205);
        BlockFromToEvent flow=new BlockFromToEvent(outside,inside);call(flow);BlockFromToEvent ownFlow=new BlockFromToEvent(inside,middle);call(ownFlow);
        BlockPistonExtendEvent push=new BlockPistonExtendEvent(outside,List.of(inside),BlockFace.EAST);call(push);
        BlockPistonRetractEvent pull=new BlockPistonRetractEvent(outside,List.of(inside),BlockFace.WEST);call(pull);
        outside.setType(Material.HOPPER,false);inside.setType(Material.CHEST,false);middle.setType(Material.CHEST,false);
        Inventory source=((org.bukkit.block.Hopper)outside.getState()).getInventory(),target=((Chest)inside.getState()).getInventory(),same=((Chest)middle.getState()).getInventory();
        InventoryMoveItemEvent hopper=new InventoryMoveItemEvent(target,new ItemStack(Material.DIAMOND),source,true);call(hopper);
        InventoryMoveItemEvent ownHopper=new InventoryMoveItemEvent(target,new ItemStack(Material.DIAMOND),same,true);call(ownHopper);
        List<Block> affected=new ArrayList<>(List.of(inside,outside));
        EntityExplodeEvent explode=new EntityExplodeEvent(p,p.getLocation(),affected,1,ExplosionResult.DESTROY);call(explode);
        BlockBurnEvent burn=new BlockBurnEvent(inside);call(burn);
        say(p,"automation:flow="+flow.isCancelled()+",ownflow="+ownFlow.isCancelled()+",push="+push.isCancelled()+",pull="+pull.isCancelled()+",hopper="+hopper.isCancelled()+",ownhopper="+ownHopper.isCancelled()+",explosion="+(!explode.blockList().contains(inside)&&explode.blockList().contains(outside))+",burn="+burn.isCancelled());
    }
}
