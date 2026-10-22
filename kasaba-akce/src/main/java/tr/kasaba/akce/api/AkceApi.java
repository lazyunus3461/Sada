package tr.kasaba.akce.api;

import java.math.BigDecimal;
import java.util.UUID;

/** Persistent server currency service. Two decimal places, no negative balances. */
public interface AkceApi {
    BigDecimal balance(UUID player);
    void deposit(UUID player, BigDecimal amount);
    boolean transfer(UUID from, UUID to, BigDecimal amount);
    /** The same transaction id, player and amount always return the saved result, including after restart. */
    boolean withdraw(UUID player, BigDecimal amount, String transactionId);
}
