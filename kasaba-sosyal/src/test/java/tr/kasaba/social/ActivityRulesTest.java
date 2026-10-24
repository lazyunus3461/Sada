package tr.kasaba.social;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
class ActivityRulesTest {
 long now=Instant.parse("2026-10-24T12:00:00Z").toEpochMilli();
 @Test void timeCombinesAcrossDifferentMatchesAndSessionRestarts(){var r=ActivityRules.add(0,"",0,3*60_000,now);assertFalse(r.reward());r=ActivityRules.add(r.carry(),r.day(),r.claims(),7*60_000,now);assertFalse(r.reward());r=ActivityRules.add(r.carry(),r.day(),r.claims(),5*60_000,now);assertTrue(r.reward());assertEquals(0,r.carry());assertEquals(1,r.claims());}
 @Test void fiveDailyRewardsThenFreezeUntilIstanbulMidnight(){long before=Instant.parse("2026-10-24T20:59:59Z").toEpochMilli();var r=ActivityRules.add(0,"",0,0,before);for(int i=0;i<5;i++){r=ActivityRules.add(r.carry(),r.day(),r.claims(),ActivityRules.INTERVAL,before);assertTrue(r.reward());}r=ActivityRules.add(r.carry(),r.day(),r.claims(),ActivityRules.INTERVAL*6,before);assertFalse(r.reward());assertEquals(5,r.claims());assertEquals(0,r.carry());r=ActivityRules.add(r.carry(),r.day(),r.claims(),ActivityRules.INTERVAL,before+1000);assertTrue(r.reward());assertEquals(1,r.claims());assertEquals("2026-10-25",r.day());}
 @Test void partialProgressIsPreservedAtMidnight(){var r=ActivityRules.add(3*60_000,"2026-10-23",4,12*60_000,now);assertEquals(1,r.claims());assertTrue(r.reward());assertEquals(0,r.carry());}
}
