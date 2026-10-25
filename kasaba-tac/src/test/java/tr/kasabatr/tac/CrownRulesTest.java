package tr.kasabatr.tac;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;import java.util.*;
class CrownRulesTest {
 @Test void oddGroupsStayTogetherAndAreNotForcedIntoEvenTeams(){var a=UUID.randomUUID();var b=UUID.randomUUID();var c=UUID.randomUUID();assertEquals(List.of(a,b,c),CrownRules.batch(List.of(List.of(a,b,c)),16));assertEquals(List.of(a,b,c),CrownRules.batch(List.of(List.of(a),List.of(b,c)),3));}
 @Test void duplicateMembersAreRejected(){var a=UUID.randomUUID();assertThrows(IllegalArgumentException.class,()->CrownRules.batch(List.of(List.of(a),List.of(a)),16));}
 @Test void allHighestScorersShareWinAndZeroScoreAwardsNobody(){var a=UUID.randomUUID();var b=UUID.randomUUID();var c=UUID.randomUUID();assertEquals(Set.of(a,b),CrownRules.winners(Map.of(a,8,b,8,c,3)));assertEquals(Set.of(),CrownRules.winners(Map.of(a,0,b,0)));}
}
