package tr.kasaba.hub.api;

import org.bukkit.plugin.Plugin;
import java.util.UUID;

/** All methods are main-thread only; retrieved through Bukkit ServicesManager. */
public interface HubApi {
    void register(Plugin owner,HubGame game);
    void unregister(Plugin owner,String id);
    boolean playing(UUID player);
    void returnToLobby(UUID player);
}
