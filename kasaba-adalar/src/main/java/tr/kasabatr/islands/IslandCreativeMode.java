package tr.kasabatr.islands;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Owns the mode only after an authenticated player's island inventory is ready. */
final class IslandCreativeMode implements Listener {
    private final JavaPlugin plugin;
    private final Predicate<Player> eligible;
    private final Set<UUID> active = new HashSet<>();
    private final Set<UUID> pending = new HashSet<>();

    IslandCreativeMode(JavaPlugin plugin, Predicate<Player> eligible) {
        this.plugin = plugin;
        this.eligible = eligible;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        // Also repairs changes made by a world manager after the teleport tick.
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID id : Set.copyOf(active)) {
                Player player = Bukkit.getPlayer(id);
                if (player != null) enforce(player);
            }
        }, 10L, 10L);
    }

    void enter(Player player) {
        active.add(player.getUniqueId());
        enforce(player);
        later(player);
    }

    void leave(UUID id) {
        // Release BEFORE restoring the outside snapshot, even if the player is
        // still in the island world while Hub prepares the lobby teleport.
        active.remove(id);
    }

    private boolean managed(Player player) {
        return active.contains(player.getUniqueId()) && player.isOnline()
                && eligible.test(player);
    }

    private void enforce(Player player) {
        if (!managed(player)) return;
        if (player.getGameMode() != GameMode.CREATIVE) player.setGameMode(GameMode.CREATIVE);
        // A teleport/login listener can reset flight without changing the mode.
        if (player.getGameMode() == GameMode.CREATIVE && !player.getAllowFlight()) player.setAllowFlight(true);
    }

    private void later(Player player) {
        UUID id = player.getUniqueId();
        if (!pending.add(id)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            pending.remove(id);
            // Recheck the world, authentication and session at execution time:
            // an old teleport callback must never turn a lobby/arena creative.
            enforce(player);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onModeChange(PlayerGameModeChangeEvent event) {
        if (!managed(event.getPlayer())) return;
        // A world's forced survival/adventure setting must not override this
        // game's creative policy. Outside this session we leave events alone.
        event.setCancelled(event.getNewGameMode() != GameMode.CREATIVE);
        later(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void observeModeChange(PlayerGameModeChangeEvent event) {
        if (managed(event.getPlayer())) later(event.getPlayer());
    }
}
