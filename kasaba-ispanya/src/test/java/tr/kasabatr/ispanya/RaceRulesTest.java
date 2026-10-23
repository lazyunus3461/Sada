package tr.kasabatr.ispanya;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.*;

class RaceRulesTest {
    @Test void allDicePairsAdvanceTheirSumUntilFinish(){
        for(int first=1;first<=6;first++)for(int second=1;second<=6;second++)assertEquals(10+first+second,RaceRules.destination(10,first,second,134,false));
    }
    @Test void finishingAndExactRoll(){assertEquals(134,RaceRules.destination(130,6,6,134,false));assertEquals(130,RaceRules.destination(130,6,6,134,true));assertEquals(134,RaceRules.destination(130,2,2,134,true));}
    @Test void invalidDiceAndLocationsFail(){for(int invalid:new int[]{-1,0,7,99}){assertThrows(IllegalArgumentException.class,()->RaceRules.destination(0,invalid,1,134,false));assertThrows(IllegalArgumentException.class,()->RaceRules.destination(0,1,invalid,134,false));}assertThrows(IllegalArgumentException.class,()->RaceRules.destination(-1,1,1,134,false));assertThrows(IllegalArgumentException.class,()->RaceRules.destination(135,1,1,134,false));}
    @Test void effectsCannotLeaveTheBoardOrOverflow(){assertEquals(0,RaceRules.effectDestination(1,-3,134));assertEquals(134,RaceRules.effectDestination(133,3,134));assertEquals(134,RaceRules.effectDestination(133,Integer.MAX_VALUE,134));assertEquals(0,RaceRules.effectDestination(1,Integer.MIN_VALUE,134));}
    @Test void twoAndFourPlayersCanCompleteAWholeRace(){for(int players:new int[]{2,4}){Random random=new Random(31+players);int[] positions=new int[players];Set<Integer> finished=new LinkedHashSet<>();for(int rounds=0;finished.size()<players&&rounds<1000;rounds++)for(int seat=0;seat<players;seat++){if(finished.contains(seat))continue;positions[seat]=RaceRules.destination(positions[seat],random.nextInt(6)+1,random.nextInt(6)+1,134,false);if(positions[seat]==134)assertTrue(finished.add(seat));}assertEquals(players,finished.size());assertTrue(Arrays.stream(positions).allMatch(p->p==134));}}
}
