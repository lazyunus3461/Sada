package tr.kasaba.battle;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class BattleRulesTest {
    @Test void adjacentAreasDoNotOverlapButSharedBlocksDo(){UUID w=UUID.randomUUID();var a=new BattleRules.Area(w,0,15,0,15);assertFalse(a.overlaps(new BattleRules.Area(w,16,31,0,15)));assertTrue(a.overlaps(new BattleRules.Area(w,15,31,0,15)));assertFalse(a.overlaps(new BattleRules.Area(UUID.randomUUID(),0,15,0,15)));assertTrue(a.contains(w,15.99,15.99));assertFalse(a.contains(w,16,4));}
    @Test void partyAlwaysStaysTogether(){var a=UUID.randomUUID();var b=UUID.randomUUID();var c=UUID.randomUUID();var d=UUID.randomUUID();var teams=BattleRules.teams(List.of(List.of(a,b),List.of(c),List.of(d)),2);assertEquals(List.of(a,b),teams.getFirst());assertEquals(List.of(c,d),teams.get(1));assertThrows(IllegalArgumentException.class,()->BattleRules.teams(List.of(List.of(a,b)),1));assertThrows(IllegalArgumentException.class,()->BattleRules.teams(List.of(List.of(a),List.of(a)),2));}
    @Test void tenAndTwentyPlayersFitSquadsWithoutSplittingParties(){for(int players:List.of(10,20)){List<List<UUID>> groups=new ArrayList<>();for(int i=0;i<players;i++)groups.add(List.of(UUID.randomUUID()));var result=BattleRules.teams(groups,4);assertEquals(players,result.stream().mapToInt(List::size).sum());assertTrue(result.stream().allMatch(t->t.size()<=4));assertTrue(result.size()>=2);}}
    @Test void zoneWaitsShrinksAndDoesNotGoNegative(){assertEquals(240,BattleRules.zone(240,8,179,180,600));assertEquals(124,BattleRules.zone(240,8,480,180,600));assertEquals(8,BattleRules.zone(240,8,99999,180,600));assertEquals(4,BattleRules.zone(4,8,999,1,1));}
    @Test void healingIsTwoHeartsNotPermanentMaximum(){assertEquals(14,BattleRules.healed(10,20,2));assertEquals(20,BattleRules.healed(19,20,2));assertEquals(20,BattleRules.healed(20,20,2));}
    @Test void chestLocationsRotateAndNeverRepeatWhileEnoughFreshPositionsExist(){Set<Integer> previous=Set.of();for(int run=0;run<50;run++){List<Integer> now=BattleRules.chestSelection(160,30,previous,new Random(run));assertEquals(30,now.size());assertEquals(30,new HashSet<>(now).size());assertTrue(Collections.disjoint(previous,now));previous=new HashSet<>(now);}}
    @Test void tinyPoolsStillRotateInsteadOfFillingEveryCandidate(){assertEquals(2,BattleRules.chestSelection(4,24,Set.of(),new Random(1)).size());assertThrows(IllegalArgumentException.class,()->BattleRules.chestSelection(3,24,Set.of(),new Random()));}
    @Test void turkishModeNamesAreAccepted(){assertEquals(BattleRules.Mode.IKILI,BattleRules.Mode.parse("İkili"));assertEquals(BattleRules.Mode.DORTLU,BattleRules.Mode.parse("dörtlü"));assertEquals(BattleRules.Mode.DORTLU,BattleRules.Mode.parse("squad"));}
    @Test void nativeSquareBorderAndDamageHaveTheSameEdges(){assertFalse(BattleRules.outsideZone(9,9,0,0,10));assertFalse(BattleRules.outsideZone(-10,10,0,0,10));assertTrue(BattleRules.outsideZone(10.01,0,0,0,10));assertTrue(BattleRules.outsideZone(0,-10.01,0,0,10));}
    @Test void outdoorQuotaRotatesOnTheExistingMap(){Set<Integer> outside=new HashSet<>();for(int i=128;i<160;i++)outside.add(i);Set<Integer> previous=Set.of();for(int run=0;run<40;run++){var chosen=BattleRules.chestSelection(160,30,previous,outside,new Random(run));assertEquals(30,chosen.size());assertEquals(15,chosen.stream().filter(outside::contains).count());assertTrue(Collections.disjoint(previous,chosen));previous=new HashSet<>(chosen);}}
    @Test void smallCustomOutdoorPoolsStillPreferFreshLocations(){var chosen=BattleRules.chestSelection(12,6,Set.of(0,1),Set.of(0,1,2),new Random(4));assertEquals(6,chosen.size());assertTrue(chosen.contains(2));assertFalse(chosen.contains(0));assertFalse(chosen.contains(1));}
}
