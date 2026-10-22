package tr.kasaba.battle;
import org.bukkit.event.*;
import tr.kasaba.auth.api.PlayerAuthenticatedEvent;
final class BattleAuthBridge implements Listener {
    private final KasabaBattleRoyale plugin;
    BattleAuthBridge(KasabaBattleRoyale plugin){this.plugin=plugin;}
    @EventHandler(priority=EventPriority.LOWEST) public void verified(PlayerAuthenticatedEvent event){plugin.recover(event.getPlayer());}
}
