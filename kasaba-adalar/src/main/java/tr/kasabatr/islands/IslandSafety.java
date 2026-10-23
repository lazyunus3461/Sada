package tr.kasabatr.islands;

import java.util.*;
import java.util.function.Predicate;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** Creative building remains available; server machinery and portals do not. */
final class IslandSafety implements Listener {
 private final Predicate<World> island;
 private final Plugin plugin;
 private static final Set<String> MACHINERY=Set.of("SPAWNER","TRIAL_SPAWNER","VAULT","OBSERVER","PISTON","STICKY_PISTON","HOPPER","DISPENSER","DROPPER","CRAFTER","REPEATER","COMPARATOR","TARGET","LEVER","SCULK_SENSOR","CALIBRATED_SCULK_SENSOR","SCULK_SHRIEKER","TRIPWIRE","TRIPWIRE_HOOK","STRING","DAYLIGHT_DETECTOR","NOTE_BLOCK","JUKEBOX","TRAPPED_CHEST","SLIME_BLOCK","HONEY_BLOCK","IRON_DOOR","IRON_TRAPDOOR","MINECART","RAIL","LAVA_BUCKET","FIRE_CHARGE","FLINT_AND_STEEL","FIREWORK_ROCKET","END_CRYSTAL");
 IslandSafety(Plugin plugin,Predicate<World> island){this.plugin=plugin;this.island=island;Bukkit.getScheduler().runTaskTimer(plugin,()->{for(Player p:Bukkit.getOnlinePlayers())clean(p);},20,20);}
 static boolean restricted(Material type){String n=type.name();return n.contains("COMMAND_BLOCK")||n.contains("STRUCTURE")||n.equals("JIGSAW")||n.contains("PORTAL")||n.contains("TNT")||n.contains("REDSTONE")||n.endsWith("_SPAWN_EGG")||n.endsWith("_MINECART")||n.endsWith("_RAIL")||n.endsWith("_BUTTON")||n.endsWith("_PRESSURE_PLATE")||n.endsWith("_COPPER_BULB")||n.equals("COPPER_BULB")||n.endsWith("LIGHTNING_ROD")||n.endsWith("_CHEST_BOAT")||n.endsWith("_CHEST_RAFT")||MACHINERY.contains(n);}
 private static boolean restricted(ItemStack item){return item!=null&&restricted(item.getType());}
 private void warn(Player p){p.sendMessage("§cAdalarda redstone eşyaları, çağırma yumurtaları, portallar ve patlayıcılar kapalı.");}
 private void clean(Player p){if(!island.test(p.getWorld()))return;for(int slot=0;slot<p.getInventory().getSize();slot++)if(restricted(p.getInventory().getItem(slot)))p.getInventory().setItem(slot,null);if(restricted(p.getItemOnCursor()))p.setItemOnCursor(null);}
 @EventHandler(priority=EventPriority.HIGHEST) public void creative(InventoryCreativeEvent e){if(e.getWhoClicked() instanceof Player p&&island.test(p.getWorld())&&restricted(e.getCursor())){e.setCancelled(true);e.setCursor(null);warn(p);}}
 @EventHandler(priority=EventPriority.HIGHEST) public void click(InventoryClickEvent e){if(e.getWhoClicked() instanceof Player p&&island.test(p.getWorld())&&(restricted(e.getCursor())||restricted(e.getCurrentItem()))){e.setCancelled(true);warn(p);}}
 @EventHandler(priority=EventPriority.HIGHEST) public void drag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p&&island.test(p.getWorld())&&(restricted(e.getOldCursor())||e.getNewItems().values().stream().anyMatch(IslandSafety::restricted))){e.setCancelled(true);warn(p);}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p&&island.test(p.getWorld())&&restricted(e.getItem().getItemStack()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drop(PlayerDropItemEvent e){if(island.test(e.getPlayer().getWorld())&&restricted(e.getItemDrop().getItemStack()))e.getItemDrop().remove();}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true) public void enter(PlayerTeleportEvent e){if(e.getTo()!=null&&island.test(e.getTo().getWorld()))Bukkit.getScheduler().runTask(plugin,()->clean(e.getPlayer()));}
 @EventHandler(priority=EventPriority.MONITOR) public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->clean(e.getPlayer()));}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e){if(island.test(e.getBlock().getWorld())&&restricted(e.getBlock().getType())){e.setCancelled(true);warn(e.getPlayer());}}
 @EventHandler(priority=EventPriority.HIGHEST) public void use(PlayerInteractEvent e){if(island.test(e.getPlayer().getWorld())&&e.getItem()!=null&&restricted(e.getItem().getType())){e.setCancelled(true);warn(e.getPlayer());}}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void portal(PortalCreateEvent e){if(island.test(e.getWorld()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityPortal(EntityPortalEvent e){if(island.test(e.getEntity().getWorld()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST) public void redstone(BlockRedstoneEvent e){if(island.test(e.getBlock().getWorld()))e.setNewCurrent(0);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fluid(BlockFromToEvent e){if(island.test(e.getBlock().getWorld()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonExtendEvent e){if(island.test(e.getBlock().getWorld()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonRetractEvent e){if(island.test(e.getBlock().getWorld()))e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void spawn(EntitySpawnEvent e){if(!island.test(e.getEntity().getWorld()))return;Entity entity=e.getEntity();if(entity instanceof TNTPrimed||entity instanceof EnderCrystal||entity instanceof org.bukkit.entity.minecart.ExplosiveMinecart||entity instanceof Projectile||entity instanceof Mob){e.setCancelled(true);return;}if((entity instanceof ArmorStand||entity instanceof Hanging)&&entity.getNearbyEntities(48,384,48).stream().filter(v->v instanceof ArmorStand||v instanceof Hanging).count()>=32)e.setCancelled(true);}
 @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hanging(HangingPlaceEvent e){if(island.test(e.getEntity().getWorld())&&e.getEntity().getNearbyEntities(48,384,48).stream().filter(v->v instanceof ArmorStand||v instanceof Hanging).count()>=32)e.setCancelled(true);}
}
