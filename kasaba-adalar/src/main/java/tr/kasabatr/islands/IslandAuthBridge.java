package tr.kasabatr.islands;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tr.kasaba.auth.api.PlayerAuthenticatedEvent;
/** Loaded only when Auth exists. Recovery must finish before the player starts another game. */
final class IslandAuthBridge implements Listener {
    private final KasabaIslandsPlugin plugin;
    IslandAuthBridge(KasabaIslandsPlugin plugin){this.plugin=plugin;}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(PlayerAuthenticatedEvent event){plugin.verifiedPlayer(event.getPlayer());}
}
