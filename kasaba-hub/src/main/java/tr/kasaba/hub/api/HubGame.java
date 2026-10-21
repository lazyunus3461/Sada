package tr.kasaba.hub.api;

import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

/** Main-thread adapter. join must admit the entire team atomically; false must leave everyone unchanged. */
public interface HubGame {
    GameInfo info();
    boolean available();
    boolean join(List<Player> team);
    void leave(UUID player);
}
