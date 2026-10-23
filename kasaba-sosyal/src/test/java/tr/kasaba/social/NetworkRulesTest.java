package tr.kasaba.social;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class NetworkRulesTest {
 @Test void pricesAndRenewalUseApprovedAmountsAndRealExpiry(){assertEquals(new BigDecimal("40.00"),NetworkRules.PRICES.get(3));assertEquals(new BigDecimal("75.00"),NetworkRules.PRICES.get(7));assertEquals(new BigDecimal("145.00"),NetworkRules.PRICES.get(15));assertEquals(new BigDecimal("250.00"),NetworkRules.PRICES.get(30));assertEquals(1000+33*NetworkRules.DAY,NetworkRules.expiry(1000,1000+30*NetworkRules.DAY,3));assertThrows(IllegalArgumentException.class,()->NetworkRules.expiry(0,0,5));}
 @Test void activityExcludesEveryLobbyAfkAndSpectating(){assertTrue(NetworkRules.earns(true,false,false,1000,5000));assertFalse(NetworkRules.earns(false,false,false,1000,5000));assertFalse(NetworkRules.earns(true,true,false,1000,5000));assertFalse(NetworkRules.earns(true,false,true,1000,5000));assertFalse(NetworkRules.earns(true,false,false,1000,121001));assertFalse(NetworkRules.earns(true,false,false,0,5000));}
 @Test void fishingStopsAtFifteenAndOnlyUnlocksAfterTwentyFourHours(){assertEquals(3,NetworkRules.sellable(10,12,0,1000));assertEquals(0,NetworkRules.sellable(100,15,1000+NetworkRules.DAY,1001));assertEquals(15,NetworkRules.sellable(20,0,1000+NetworkRules.DAY,1000+NetworkRules.DAY));}
}
