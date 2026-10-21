package tr.kasaba.test;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Isolated loopback fixture, excluded from the production plugin. */
public final class WarpProbe extends JavaPlugin implements Listener {
    private final Set<UUID> block=new HashSet<>();
    @Override public void onEnable() {
        if(!getServer().getIp().equals("127.0.0.1"))throw new IllegalStateException("Probe requires loopback binding");
        getServer().getPluginManager().registerEvents(this,this);
        getServer().getMessenger().registerIncomingPluginChannel(this,"kasaba_test:warp",(channel,player,data)->{
            if(data.length>1024)throw new IllegalArgumentException("Oversized test request");
            String operation=new String(data,StandardCharsets.UTF_8);
            if(operation.startsWith("CONSOLE|")) {
                String command=operation.substring(8);
                if(!command.startsWith("warp ")&&!command.startsWith("rol "))throw new IllegalArgumentException("Test command not allowed");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),command);
            }else if(operation.startsWith("PLAYER|warp "))Bukkit.dispatchCommand(player,operation.substring(7));
            else switch(operation) {
                case "ADMIN" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"rol ver "+player.getName()+" kurucu");
                case "A" -> player.teleport(new Location(player.getWorld(),21.5,-60,18.5,123,-12));
                case "B" -> player.teleport(new Location(player.getWorld(),2.5,-60,2.5,10,0));
                case "C" -> player.teleport(new Location(player.getWorld(),-8.5,-60,6.5,-45,7));
                case "SAPLING" -> {
                    ItemStack item=new ItemStack(Material.OAK_SAPLING,7);var meta=item.getItemMeta();
                    meta.setCustomModelData(731);meta.displayName(Component.text("Test Fidanı"));item.setItemMeta(meta);
                    player.getInventory().setHeldItemSlot(0);player.getInventory().setItem(0,item);
                }
                case "SAPLING_CHECK" -> {
                    ItemStack item=player.getInventory().getItem(0);
                    player.sendMessage(Component.text("warp-probe:sapling="+(item!=null&&item.getType()==Material.OAK_SAPLING&&item.getAmount()==7&&item.getItemMeta().getCustomModelData()==731)));
                }
                case "DAMAGE" -> {player.setHealth(20);player.damage(1);}
                case "BLOCK" -> block.add(player.getUniqueId());
                case "ITEM" -> player.getInventory().setItem(0,new ItemStack(Material.DIAMOND,7));
                case "ITEM_CHECK" -> {
                    ItemStack item=player.getInventory().getItem(0);boolean intact=item!=null&&item.getType()==Material.DIAMOND&&item.getAmount()==7;
                    boolean pearls=Arrays.stream(player.getInventory().getContents()).filter(Objects::nonNull).anyMatch(stack->stack.getType()==Material.ENDER_PEARL);
                    player.sendMessage(Component.text("warp-probe:inventory="+intact+",pearls="+pearls));
                }
                case "MANY" -> {for(int i=0;i<46;i++)Bukkit.dispatchCommand(player,"warp oluştur test"+String.format(Locale.ROOT,"%02d",i));}
                default -> throw new IllegalArgumentException("Unknown test request");
            }
            player.sendMessage(Component.text("warp-probe:done:"+operation));
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void teleport(PlayerTeleportEvent event) {
        if(event.getCause()==PlayerTeleportEvent.TeleportCause.PLUGIN&&block.remove(event.getPlayer().getUniqueId()))event.setCancelled(true);
    }
}
