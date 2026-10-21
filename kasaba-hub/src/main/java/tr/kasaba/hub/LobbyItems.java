package tr.kasaba.hub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import java.util.*;

/** Owns only tagged temporary items; ordinary player items are never replaced or saved elsewhere. */
final class LobbyItems implements Listener {
    private final KasabaHub hub;
    private final NamespacedKey key;
    private final Set<UUID> warned = new HashSet<>();
    LobbyItems(KasabaHub hub) { this.hub = hub; key = new NamespacedKey(hub, "lobi-menusu"); }
    boolean marked(ItemStack item) { return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(key); }
    private ItemStack create() {
        ItemStack item = new ItemStack(Material.COMPASS);
        item.editMeta(meta -> {
            meta.displayName(Component.text("Kasaba • Oyun Menüsü", NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("Menüyü açmak için sağ tıkla", NamedTextColor.AQUA)));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte)1);
        });
        return item;
    }
    void remove(Player p) {
        var inventory = p.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) if (marked(inventory.getItem(i))) inventory.setItem(i, null);
        if (marked(p.getItemOnCursor())) p.setItemOnCursor(null);
    }
    void refresh(Player p) {
        if (!hub.menuItemAllowed(p)) { remove(p); warned.remove(p.getUniqueId()); return; }
        int found = -1;
        var inventory = p.getInventory();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (!marked(item)) continue;
            if (i < 36 && found < 0) { found = i; if (item.getAmount() != 1) item.setAmount(1); }
            else inventory.setItem(i, null);
        }
        if (marked(p.getItemOnCursor())) p.setItemOnCursor(null);
        if (found >= 0) { warned.remove(p.getUniqueId()); return; }
        int slot = inventory.getItem(8) == null || inventory.getItem(8).getType().isAir() ? 8 : inventory.firstEmpty();
        if (slot >= 0 && slot < 36) { inventory.setItem(slot, create()); warned.remove(p.getUniqueId()); }
        else if (warned.add(p.getUniqueId())) hub.tell(p, "Menü pusulası için envanterinde bir yer aç. Şimdilik /merkez kullanabilirsin.");
    }
    void forget(Player p) { remove(p); warned.remove(p.getUniqueId()); }
    private void later(Player p) { Bukkit.getScheduler().runTask(hub, () -> { if (p.isOnline()) refresh(p); }); }

    @EventHandler(priority = EventPriority.HIGHEST) public void use(PlayerInteractEvent e) {
        if (!marked(e.getItem())) return;
        e.setCancelled(true);
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (hub.menuItemAllowed(e.getPlayer())) hub.openItemMenu(e.getPlayer());
        else refresh(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void click(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        boolean swap = e.getHotbarButton() >= 0 && marked(p.getInventory().getItem(e.getHotbarButton()));
        if (e.getClick() == ClickType.SWAP_OFFHAND) swap |= marked(p.getInventory().getItemInOffHand());
        if (marked(e.getCurrentItem()) || marked(e.getCursor()) || swap) e.setCancelled(true);
        later(p);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void drag(InventoryDragEvent e) {
        if (marked(e.getOldCursor()) || e.getNewItems().values().stream().anyMatch(this::marked)) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void drop(PlayerDropItemEvent e) {
        if (marked(e.getItemDrop().getItemStack())) { e.setCancelled(true); later(e.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void swap(PlayerSwapHandItemsEvent e) {
        if (marked(e.getMainHandItem()) || marked(e.getOffHandItem())) e.setCancelled(true);
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void pickup(EntityPickupItemEvent e) {
        if (marked(e.getItem().getItemStack())) { e.setCancelled(true); e.getItem().remove(); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void hopper(InventoryPickupItemEvent e) {
        if (marked(e.getItem().getItemStack())) { e.setCancelled(true); e.getItem().remove(); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void death(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::marked); remove(e.getEntity());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void move(PlayerMoveEvent e) {
        if (e.hasChangedBlock() && hub.inLobby(e.getFrom()) != hub.inLobby(e.getTo())) later(e.getPlayer());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true) public void teleport(PlayerTeleportEvent e) { later(e.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR) public void world(PlayerChangedWorldEvent e) { later(e.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR) public void respawn(PlayerRespawnEvent e) { later(e.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR) public void close(InventoryCloseEvent e) { if (e.getPlayer() instanceof Player p) later(p); }
}
