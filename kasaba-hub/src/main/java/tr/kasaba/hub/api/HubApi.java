package tr.kasaba.hub.api;

import org.bukkit.plugin.Plugin;
import java.util.UUID;

/** All methods are main-thread only; retrieved through Bukkit ServicesManager. */
public interface HubApi {
    void register(Plugin owner,HubGame game);
    void unregister(Plugin owner,String id);
    boolean playing(UUID player);
    default boolean mainLobby(UUID player){return false;}
    default boolean socialArea(UUID player){return mainLobby(player);}
    default boolean gameplay(UUID player){return playing(player)&&!socialArea(player);}
    void returnToLobby(UUID player);
    default void returnToLobbyForced(UUID player){returnToLobby(player);}
    default void abandon(UUID player){}
    default void requireGameEntry(UUID player,String game){}
    default void openGames(UUID player){}
    default boolean transferring(UUID player){return false;}
    default void presentLobby(Plugin owner,String game,org.bukkit.Location spawn,String title,java.util.List<String> lines,String metric){}
    default void recordGameResult(Plugin owner,String game,UUID match,java.util.Collection<UUID> winners){}
    default void setGameScore(Plugin owner,String game,UUID player,String name,long score){}
}
