package tr.kasaba.hub;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.vehicle.*;

/** Limited to the configured lobby square; game worlds and player inventories are not rewritten. */
final class LobbyProtection implements Listener {
    private final KasabaHub hub;
    LobbyProtection(KasabaHub hub){this.hub=hub;}
    private boolean denied(Player p,Location l){return hub.inLobby(l)&&!hub.editing(p);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void broken(BlockBreakEvent e){if(denied(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void placed(BlockPlaceEvent e){if(denied(e.getPlayer(),e.getBlockPlaced().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void multi(BlockMultiPlaceEvent e){if(e.getReplacedBlockStates().stream().anyMatch(b->denied(e.getPlayer(),b.getLocation())))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){if(e.getClickedBlock()!=null&&denied(e.getPlayer(),e.getClickedBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bucket(PlayerBucketEmptyEvent e){if(denied(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bucket(PlayerBucketFillEvent e){if(denied(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entity(PlayerInteractEntityEvent e){if(denied(e.getPlayer(),e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entity(PlayerInteractAtEntityEvent e){if(denied(e.getPlayer(),e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void damage(EntityDamageEvent e){
        if(e.getEntity() instanceof Player p){if(hub.protectedPlayer(p))e.setCancelled(true);}
        else if(hub.inLobby(e.getEntity().getLocation())&&!(e instanceof EntityDamageByEntityEvent by&&by.getDamager() instanceof Player editor&&hub.editing(editor)))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hanging(HangingBreakEvent e){if(hub.inLobby(e.getEntity().getLocation())&&!(e instanceof HangingBreakByEntityEvent by&&by.getRemover() instanceof Player p&&hub.editing(p)))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void armor(PlayerArmorStandManipulateEvent e){if(denied(e.getPlayer(),e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bucket(PlayerBucketEntityEvent e){if(denied(e.getPlayer(),e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void container(InventoryOpenEvent e){if(e.getPlayer() instanceof Player p&&e.getInventory().getHolder() instanceof org.bukkit.block.BlockState state&&denied(p,state.getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void vehicle(VehicleDestroyEvent e){if(hub.inLobby(e.getVehicle().getLocation())&&!(e.getAttacker() instanceof Player p&&hub.editing(p)))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void food(FoodLevelChangeEvent e){if(e.getEntity() instanceof Player p&&hub.protectedPlayer(p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void drop(PlayerDropItemEvent e){if(hub.protectedPlayer(e.getPlayer())&&!hub.editing(e.getPlayer()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p&&hub.protectedPlayer(p)&&!hub.editing(p))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void spawn(CreatureSpawnEvent e){if(hub.inLobby(e.getLocation())&&!(e.getEntity() instanceof org.bukkit.entity.ArmorStand)&&e.getSpawnReason()==CreatureSpawnEvent.SpawnReason.NATURAL)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explode(EntityExplodeEvent e){e.blockList().removeIf(b->hub.inLobby(b.getLocation()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explode(BlockExplodeEvent e){e.blockList().removeIf(b->hub.inLobby(b.getLocation()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void burn(BlockBurnEvent e){if(hub.inLobby(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void spread(BlockSpreadEvent e){if(hub.inLobby(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fluid(BlockFromToEvent e){if(hub.inLobby(e.getToBlock().getLocation())&&!hub.inLobby(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonExtendEvent e){if(hub.inLobby(e.getBlock().getRelative(e.getDirection()).getLocation())){e.setCancelled(true);return;}for(Block b:e.getBlocks())if(hub.inLobby(b.getLocation())||hub.inLobby(b.getRelative(e.getDirection()).getLocation())){e.setCancelled(true);return;}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void piston(BlockPistonRetractEvent e){for(Block b:e.getBlocks())if(hub.inLobby(b.getLocation())||hub.inLobby(b.getRelative(e.getDirection()).getLocation())){e.setCancelled(true);return;}}
}
