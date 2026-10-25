package tr.kasaba.roles.api;

import java.util.UUID;
import org.bukkit.entity.Player;

public interface RoleAccess {
    /** Current visible rank, including temporary VIP and staff priority. Main thread only. */
    default net.kyori.adventure.text.Component rank(Player player){return net.kyori.adventure.text.Component.text("Oyuncu");}

    boolean vip(Player player);
    boolean guide(Player player);
    long vipUntil(UUID player);
    /** Absolute expiry makes retrying a committed purchase safe. */
    void grantVip(UUID player,long expiresAt);
}
