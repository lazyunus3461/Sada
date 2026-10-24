package tr.kasabatr.esya;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class EsyaRulesTest {
 @Test void balancedRolesAtTheBoundary(){assertEquals(1,EsyaRules.seekerCount(2));assertEquals(1,EsyaRules.seekerCount(4));assertEquals(1,EsyaRules.seekerCount(7));assertEquals(2,EsyaRules.seekerCount(8));assertEquals(2,EsyaRules.seekerCount(12));assertThrows(IllegalArgumentException.class,()->EsyaRules.seekerCount(1));assertThrows(IllegalArgumentException.class,()->EsyaRules.seekerCount(13));}
 @Test void queuedGroupsAreNeverSplitAcrossMatches(){List<UUID> first=new ArrayList<>(),second=new ArrayList<>();for(int i=0;i<8;i++)first.add(UUID.randomUUID());for(int i=0;i<6;i++)second.add(UUID.randomUUID());assertEquals(first,EsyaRules.batch(List.of(first,second),12));assertEquals(second,EsyaRules.batch(List.of(second),12));assertThrows(IllegalArgumentException.class,()->EsyaRules.batch(List.of(List.of(first.getFirst()),List.of(first.getFirst())),12));}
 @Test void foundHidersDoNotAutomaticallyWinWhenEverybodyBecomesASeeker(){UUID seeker=UUID.randomUUID(),survivor=UUID.randomUUID(),found=UUID.randomUUID();Set<UUID> all=Set.of(seeker,survivor,found);assertEquals(List.of(seeker),EsyaRules.winners(true,Set.of(seeker),Set.of(),all));assertEquals(List.of(survivor),EsyaRules.winners(false,Set.of(seeker),Set.of(survivor),all));assertTrue(EsyaRules.winners(true,Set.of(seeker),Set.of(),Set.of(found)).isEmpty());}
}
