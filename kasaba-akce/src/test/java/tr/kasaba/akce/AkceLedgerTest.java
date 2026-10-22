package tr.kasaba.akce;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AkceLedgerTest {
    @TempDir Path dir;
    private final UUID a=UUID.randomUUID(),b=UUID.randomUUID();
    private static BigDecimal money(String n){return AkceLedger.parse(n);}
    @Test void transferPersistsBothSidesAndSupportsOfflineNames()throws Exception{
        Path file=dir.resolve("ledger");AkceLedger bank=new AkceLedger(file);bank.remember(a,"Redkit",money("100"));bank.remember(b,"Dalton",money("0"));assertTrue(bank.transfer(a,b,money("25,50")));bank=new AkceLedger(file);assertEquals(money("74.50"),bank.balance(a));assertEquals(money("25.50"),bank.balance(b));assertEquals(b,bank.find("DALTON").orElseThrow());bank.remember(a,"Redkit",money("999"));assertEquals(money("74.50"),bank.balance(a));assertEquals(a,bank.richest().getFirst());
    }
    @Test void repeatedMarketDebitRemainsIdempotentAfterRestart()throws Exception{
        Path file=dir.resolve("ledger");AkceLedger bank=new AkceLedger(file);bank.deposit(a,money("100"));assertTrue(bank.withdraw(a,money("40"),"market:1"));bank=new AkceLedger(file);assertTrue(bank.withdraw(a,money("40"),"market:1"));assertEquals(money("60"),bank.balance(a));AkceLedger loaded=bank;assertThrows(IllegalArgumentException.class,()->loaded.withdraw(b,money("40"),"market:1"));assertThrows(IllegalArgumentException.class,()->loaded.withdraw(a,money("50"),"market:1"));
    }
    @Test void rejectedPaymentCannotBecomeAChargeOnRetry()throws Exception{
        Path file=dir.resolve("ledger");AkceLedger bank=new AkceLedger(file);assertFalse(bank.withdraw(a,money("10"),"no-money"));bank.deposit(a,money("100"));bank=new AkceLedger(file);assertFalse(bank.withdraw(a,money("10"),"no-money"));assertEquals(money("100"),bank.balance(a));
    }
    @Test void invalidAmountsAndSelfTransferNeverChangeBalances()throws Exception{
        AkceLedger bank=new AkceLedger(dir.resolve("ledger"));bank.deposit(a,money("5"));assertFalse(bank.transfer(a,b,money("10")));assertThrows(IllegalArgumentException.class,()->bank.transfer(a,a,money("1")));assertThrows(IllegalArgumentException.class,()->bank.transfer(a,b,money("0")));assertThrows(IllegalArgumentException.class,()->bank.deposit(a,new BigDecimal("0.001")));for(String n:List.of("-1","NaN","1e99","1.234","1000000000001"))assertThrows(IllegalArgumentException.class,()->money(n));assertEquals(money("5"),bank.balance(a));assertEquals(money("0"),bank.balance(b));
    }
    @Test void failedCommitDoesNotDebitOrCreditEitherAccount()throws Exception{
        Path file=dir.resolve("ledger");AkceLedger bank=new AkceLedger(file);bank.deposit(a,money("100"));Files.createDirectory(dir.resolve("ledger.tmp"));assertThrows(IllegalStateException.class,()->bank.transfer(a,b,money("30")));assertEquals(money("100"),bank.balance(a));assertEquals(money("0"),bank.balance(b));AkceLedger restarted=new AkceLedger(file);assertEquals(money("100"),restarted.balance(a));assertEquals(money("0"),restarted.balance(b));
    }
    @Test void corruptLedgerIsPreservedAndStartupFails()throws Exception{
        Path file=dir.resolve("ledger");String text="schema=1\nbalance."+a+"=-100\n";Files.writeString(file,text);assertThrows(java.io.IOException.class,()->new AkceLedger(file));assertEquals(text,Files.readString(file));
    }
    @Test void simultaneousWithdrawalsNeverOverdrawOrLoseUpdates()throws Exception{
        AkceLedger bank=new AkceLedger(dir.resolve("ledger"));bank.deposit(a,money("10"));try(ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor()){List<Future<Boolean>> results=new ArrayList<>();for(int i=0;i<30;i++){int id=i;results.add(executor.submit(()->bank.withdraw(a,money("1"),"parallel:"+id)));}int paid=0;for(Future<Boolean> result:results)if(result.get())paid++;assertEquals(10,paid);assertEquals(money("0"),bank.balance(a));}assertEquals(money("0"),new AkceLedger(dir.resolve("ledger")).balance(a));
    }
}
