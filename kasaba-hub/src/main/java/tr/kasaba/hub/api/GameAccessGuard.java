package tr.kasaba.hub.api;

import java.util.UUID;

/** Optional moderation gate, also checked for native dialog callbacks. */
public interface GameAccessGuard {
    boolean blocked(UUID player);
    String message(UUID player);
}
