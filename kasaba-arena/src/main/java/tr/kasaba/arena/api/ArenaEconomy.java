package tr.kasaba.arena.api;

import java.math.BigDecimal;
import java.util.UUID;

/** Main-thread service implemented by the future currency plugin, registered with Bukkit ServicesManager. */
public interface ArenaEconomy {
    BigDecimal balance(UUID player);

    /**
     * Atomic, persistent and idempotent debit. A repeated transactionId must return the original result
     * without charging twice. Return false for insufficient funds; throw when the service is unavailable.
     * Arena persists a pending purchase before invoking this method and retries it after a crash.
     */
    boolean withdraw(UUID player, BigDecimal amount, String transactionId);
}
