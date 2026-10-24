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
    /** True only in this game's safe lobby/waiting phase, never during play. */
    default boolean lobby(UUID player){return false;}
    default boolean penaltyEligible(UUID player){return false;}
    default String penaltyKey(UUID player){return info().id();}
    default boolean containsMatch(UUID player,org.bukkit.Location at){return true;}
}
