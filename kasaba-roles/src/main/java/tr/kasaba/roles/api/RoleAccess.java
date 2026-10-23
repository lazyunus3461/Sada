package tr.kasaba.roles.api;

import java.util.UUID;
import org.bukkit.entity.Player;

public interface RoleAccess {
    boolean vip(Player player);
    boolean guide(Player player);
    long vipUntil(UUID player);
    /** Absolute expiry makes retrying a committed purchase safe. */
    void grantVip(UUID player,long expiresAt);
}
