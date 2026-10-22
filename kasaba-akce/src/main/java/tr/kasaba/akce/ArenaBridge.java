package tr.kasaba.akce;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import tr.kasaba.arena.api.ArenaEconomy;
import java.math.BigDecimal;
import java.util.UUID;

/** Loaded only when KasabaArena is available; Akçe also runs by itself. */
final class ArenaBridge implements ArenaEconomy {
    private final AkceLedger ledger;
    ArenaBridge(KasabaAkce plugin,AkceLedger ledger){this.ledger=ledger;Bukkit.getServicesManager().register(ArenaEconomy.class,this,plugin,ServicePriority.Normal);}
    public BigDecimal balance(UUID player){return ledger.balance(player);}
    public boolean withdraw(UUID player,BigDecimal amount,String transactionId){return ledger.withdraw(player,amount,transactionId);}
}
