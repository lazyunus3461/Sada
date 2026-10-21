package tr.kasaba.test;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/** Local-only regression fixture. Never packaged inside KasabaAuth. */
public final class FlightProbe extends JavaPlugin {
    @Override public void onEnable() {
        if (!getServer().getIp().equals("127.0.0.1")) throw new IllegalStateException("Probe requires loopback binding");
        // Keep synthetic protocol clients alive after authentication on the local flat world.
        for (var world : Bukkit.getWorlds()) {
            world.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
            world.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
        }
        getServer().getMessenger().registerIncomingPluginChannel(this, "kasaba_test:flight", (channel, player, data) -> {
            String operation = new String(data, StandardCharsets.US_ASCII);
            try {
                if (operation.equals("STALE_FLOAT") || operation.equals("CLEAR_FLOAT")) {
                    Object handle = player.getClass().getMethod("getHandle").invoke(player);
                    Object connection = handle.getClass().getField("connection").get(handle);
                    field(connection,"clientIsFloating").setBoolean(connection, operation.equals("STALE_FLOAT"));
                    field(connection,"aboveGroundTickCount").setInt(connection, operation.equals("STALE_FLOAT") ? 1000 : 0);
                } else {
                    player.kick(Component.text("flight-probe:" + operation), PlayerKickEvent.Cause.valueOf(operation));
                }
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) player.sendMessage(Component.text("flight-probe-survived:" + operation));
                }, 3);
            } catch (ReflectiveOperationException | IllegalArgumentException ex) {
                throw new IllegalStateException("Flight probe failed: " + operation, ex);
            }
        });
        getServer().getMessenger().registerIncomingPluginChannel(this, "kasaba_test:spawn", (channel, player, data) -> {
            String operation = new String(data, StandardCharsets.US_ASCII);
            switch (operation) {
                case "GRANT" -> {
                    PermissionAttachment attachment = player.addAttachment(this);
                    attachment.setPermission("kasabaauth.admin", true);
                }
                case "GRANT_ROLE" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "rol ver " + player.getName() + " kurucu");
                case "MOVE" -> player.teleport(new Location(player.getWorld(), 21.5, -60, 18.5, 123, -12));
                case "AWAY" -> player.teleport(new Location(player.getWorld(), 2.5, -60, 2.5, 10, 0));
                case "LOCKED_TELEPORT" -> player.teleport(player.getLocation().add(40, 0, 40));
                case "CLEAR" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "kayityeri sil");
                default -> throw new IllegalArgumentException("Unknown local spawn probe operation");
            }
            player.sendMessage(Component.text("spawn-probe:" + operation));
        });
    }
    private static Field field(Object target, String name) throws NoSuchFieldException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
