package tr.kasaba.clan;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.*;
import org.bukkit.event.world.*;
import org.bukkit.inventory.*;
import org.bukkit.projectiles.BlockProjectileSource;
import java.util.*;

/** All access checks use the current immutable membership/index; visiting claims is allowed. */
final class Protection implements Listener {
    private final KasabaKlan plugin;
    Protection(KasabaKlan plugin){this.plugin=plugin;}
    private boolean deny(Player p,Location location){if(!plugin.forbidden(p,location))return false;plugin.warn(p);return true;}
    private boolean protectedAt(Location location){return plugin.locked()||plugin.owner(location)!=null;}
    private boolean foreignNatural(Location from,Location to){return plugin.locked()||(plugin.owner(to)!=null&&!Objects.equals(plugin.owner(from),plugin.owner(to)));}
    private Player actor(Entity entity){
        if(entity instanceof Player p)return p;
        if(entity instanceof Projectile projectile&&projectile.getShooter() instanceof Player p)return p;
        if(entity instanceof TNTPrimed tnt&&tnt.getSource() instanceof Player p)return p;
        if(entity instanceof AreaEffectCloud cloud&&cloud.getSource() instanceof Player p)return p;
        if(entity instanceof Tameable tame&&tame.getOwner() instanceof Player p)return p;
        return null;
    }
    private boolean entityDenied(Entity target,Entity source){
        if(target instanceof Player)return false; // Preserve the server's existing PvP behavior.
        Player player=actor(source);if(player!=null)return deny(player,target.getLocation());
        if(source instanceof Projectile p&&p.getShooter() instanceof BlockProjectileSource block)return foreignNatural(block.getBlock().getLocation(),target.getLocation());
        return protectedAt(target.getLocation());
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(plugin.selecting(e.getPlayer())||deny(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void place(BlockPlaceEvent e){
        if(deny(e.getPlayer(),e.getBlockPlaced().getLocation())){e.setCancelled(true);return;}
        if(e.getBlockPlaced().getType()==Material.CHEST||e.getBlockPlaced().getType()==Material.TRAPPED_CHEST)
            for(BlockFace face:List.of(BlockFace.NORTH,BlockFace.SOUTH,BlockFace.EAST,BlockFace.WEST)){
                Block neighbor=e.getBlockPlaced().getRelative(face);
                if(neighbor.getType()==e.getBlockPlaced().getType()&&plugin.blockedAutomation(e.getBlockPlaced().getLocation(),neighbor.getLocation()))e.setCancelled(true);
            }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void multiPlace(BlockMultiPlaceEvent e){for(BlockState state:e.getReplacedBlockStates())if(deny(e.getPlayer(),state.getLocation())){e.setCancelled(true);return;}}
    @EventHandler(priority=EventPriority.HIGHEST) public void interact(PlayerInteractEvent e){
        Player p=e.getPlayer();Block block=e.getClickedBlock();
        if(plugin.selecting(p)){
            e.setCancelled(true);
            if(e.getHand()==EquipmentSlot.HAND&&block!=null&&(e.getAction()==Action.LEFT_CLICK_BLOCK||e.getAction()==Action.RIGHT_CLICK_BLOCK))plugin.corner(p,block,e.getAction()==Action.LEFT_CLICK_BLOCK);
            return;
        }
        if(block!=null&&plugin.forbidden(p,block.getLocation())){
            e.setUseInteractedBlock(Event.Result.DENY);e.setUseItemInHand(Event.Result.DENY);e.setCancelled(true);plugin.warn(p);
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bucketEmpty(PlayerBucketEmptyEvent e){if(deny(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bucketFill(PlayerBucketFillEvent e){if(deny(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void interactEntity(PlayerInteractEntityEvent e){if(deny(e.getPlayer(),e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void atEntity(PlayerInteractAtEntityEvent e){if(deny(e.getPlayer(),e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void armorStand(PlayerArmorStandManipulateEvent e){if(deny(e.getPlayer(),e.getRightClicked().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void shear(PlayerShearEntityEvent e){if(deny(e.getPlayer(),e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void leash(PlayerLeashEntityEvent e){if(deny(e.getPlayer(),e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void unleash(PlayerUnleashEntityEvent e){if(deny(e.getPlayer(),e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void harvest(PlayerHarvestBlockEvent e){if(deny(e.getPlayer(),e.getHarvestedBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void sign(SignChangeEvent e){if(deny(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void lectern(PlayerTakeLecternBookEvent e){if(deny(e.getPlayer(),e.getLectern().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bed(PlayerBedEnterEvent e){if(deny(e.getPlayer(),e.getBed().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fish(PlayerFishEvent e){if(e.getCaught()!=null&&!(e.getCaught() instanceof Player)&&deny(e.getPlayer(),e.getCaught().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void bucketEntity(PlayerBucketEntityEvent e){if(deny(e.getPlayer(),e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityPlace(EntityPlaceEvent e){if(e.getPlayer()!=null&&deny(e.getPlayer(),e.getEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityDamage(EntityDamageByEntityEvent e){if(entityDenied(e.getEntity(),e.getDamager()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void environmentalDamage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player)&&protectedAt(e.getEntity().getLocation())&&Set.of(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION,EntityDamageEvent.DamageCause.ENTITY_EXPLOSION).contains(e.getCause()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void potion(PotionSplashEvent e){for(LivingEntity target:e.getAffectedEntities())if(!(target instanceof Player)&&entityDenied(target,e.getEntity()))e.setIntensity(target,0);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void cloud(AreaEffectCloudApplyEvent e){e.getAffectedEntities().removeIf(target->!(target instanceof Player)&&entityDenied(target,e.getEntity()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hanging(HangingBreakEvent e){
        if(e instanceof HangingBreakByEntityEvent by){if(entityDenied(by.getEntity(),by.getRemover()))e.setCancelled(true);}
        else if(protectedAt(e.getEntity().getLocation()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void vehicleDamage(VehicleDamageEvent e){if(entityDenied(e.getVehicle(),e.getAttacker()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void vehicleDestroy(VehicleDestroyEvent e){if(entityDenied(e.getVehicle(),e.getAttacker()))e.setCancelled(true);}
    private List<Location> inventoryLocations(Inventory inventory){
        InventoryHolder holder=inventory.getHolder();
        if(holder instanceof DoubleChest chest){List<Location> list=new ArrayList<>();for(InventoryHolder side:List.of(chest.getLeftSide(),chest.getRightSide()))if(side instanceof BlockState state)list.add(state.getLocation());return list;}
        if(holder instanceof BlockState state)return List.of(state.getLocation());
        if(holder instanceof Entity entity&&!(entity instanceof Player))return List.of(entity.getLocation());
        return List.of();
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void open(InventoryOpenEvent e){if(e.getPlayer() instanceof Player p)for(Location location:inventoryLocations(e.getInventory()))if(deny(p,location)){e.setCancelled(true);return;}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void inventoryClick(InventoryClickEvent e){
        if(e.getWhoClicked() instanceof Player p)for(Location location:inventoryLocations(e.getView().getTopInventory()))if(deny(p,location)){e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void inventoryDrag(InventoryDragEvent e){
        if(e.getWhoClicked() instanceof Player p)for(Location location:inventoryLocations(e.getView().getTopInventory()))if(deny(p,location)){e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hopper(InventoryMoveItemEvent e){
        List<Location> from=inventoryLocations(e.getSource()),to=inventoryLocations(e.getDestination());
        for(Location source:from)for(Location target:to)if(plugin.blockedAutomation(source,target)){e.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void hopperPickup(InventoryPickupItemEvent e){for(Location location:inventoryLocations(e.getInventory()))if(plugin.blockedAutomation(e.getItem().getLocation(),location)){e.setCancelled(true);return;}}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void flow(BlockFromToEvent e){if(foreignNatural(e.getBlock().getLocation(),e.getToBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void extend(BlockPistonExtendEvent e){piston(e,e.getBlocks(),e.getDirection());}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void retract(BlockPistonRetractEvent e){piston(e,e.getBlocks(),e.getDirection());}
    private void piston(BlockPistonEvent event,List<Block> blocks,BlockFace direction){
        Location base=event.getBlock().getLocation();
        if(blocks.isEmpty()&&plugin.blockedAutomation(base,event.getBlock().getRelative(direction).getLocation())){event.setCancelled(true);return;}
        for(Block block:blocks)if(plugin.blockedAutomation(base,block.getLocation())||plugin.blockedAutomation(block.getLocation(),block.getRelative(direction).getLocation())){event.setCancelled(true);return;}
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explosion(EntityExplodeEvent e){e.blockList().removeIf(b->protectedAt(b.getLocation()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void explosion(BlockExplodeEvent e){e.blockList().removeIf(b->protectedAt(b.getLocation()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void burn(BlockBurnEvent e){if(protectedAt(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void ignite(BlockIgniteEvent e){
        if(e.getPlayer()!=null){if(deny(e.getPlayer(),e.getBlock().getLocation()))e.setCancelled(true);}
        else if(e.getIgnitingBlock()!=null){if(foreignNatural(e.getIgnitingBlock().getLocation(),e.getBlock().getLocation()))e.setCancelled(true);}
        else if(protectedAt(e.getBlock().getLocation()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void spread(BlockSpreadEvent e){if(foreignNatural(e.getSource().getLocation(),e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void grow(StructureGrowEvent e){e.getBlocks().removeIf(b->foreignNatural(e.getLocation(),b.getLocation()));}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void fertilize(BlockFertilizeEvent e){
        e.getBlocks().removeIf(b->e.getPlayer()!=null?plugin.forbidden(e.getPlayer(),b.getLocation()):foreignNatural(e.getBlock().getLocation(),b.getLocation()));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void mobInteract(EntityInteractEvent e){if(protectedAt(e.getBlock().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void entityBlock(EntityChangeBlockEvent e){
        if(e.getEntity() instanceof FallingBlock falling){Location origin=falling.getOrigin();if(origin==null?protectedAt(e.getBlock().getLocation()):foreignNatural(origin,e.getBlock().getLocation()))e.setCancelled(true);}
        else if(protectedAt(e.getBlock().getLocation()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void dispense(BlockDispenseEvent e){
        if(e.getBlock().getBlockData() instanceof Directional directional&&foreignNatural(e.getBlock().getLocation(),e.getBlock().getRelative(directional.getFacing()).getLocation()))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void dispenseArmor(BlockDispenseArmorEvent e){if(foreignNatural(e.getBlock().getLocation(),e.getTargetEntity().getLocation()))e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void projectile(ProjectileHitEvent e){
        if(e.getHitBlock()==null||!Tag.BUTTONS.isTagged(e.getHitBlock().getType())||!(e.getEntity() instanceof AbstractArrow))return;Player p=actor(e.getEntity());Location location=e.getHitBlock().getLocation();
        if(p!=null){if(deny(p,location))e.setCancelled(true);}
        else if(e.getEntity().getShooter() instanceof BlockProjectileSource source){if(foreignNatural(source.getBlock().getLocation(),location))e.setCancelled(true);}
        else if(protectedAt(location))e.setCancelled(true);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void portal(PortalCreateEvent e){
        for(BlockState state:e.getBlocks())if(e.getEntity() instanceof Player p?deny(p,state.getLocation()):protectedAt(state.getLocation())){e.setCancelled(true);return;}
    }
}
