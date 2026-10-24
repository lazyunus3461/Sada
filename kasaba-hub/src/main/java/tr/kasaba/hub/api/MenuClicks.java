package tr.kasaba.hub.api;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import java.util.function.Predicate;

/** Open a tagged control from the player's inventory after the inventory transaction ends. */
public final class MenuClicks {
    private MenuClicks(){}
    public static void open(Plugin plugin,InventoryClickEvent event,Predicate<ItemStack> marked,Runnable action){
        if(!(event.getWhoClicked() instanceof Player player)||event.getClickedInventory()!=player.getInventory()||!marked.test(event.getCurrentItem()))return;
        event.setCancelled(true);
        if(event.getClick()!=ClickType.LEFT&&event.getClick()!=ClickType.RIGHT)return;
        Bukkit.getScheduler().runTask(plugin,()->{if(!player.isOnline())return;player.closeInventory();try{action.run();}catch(IllegalArgumentException e){player.sendMessage(e.getMessage());}});
    }
}
