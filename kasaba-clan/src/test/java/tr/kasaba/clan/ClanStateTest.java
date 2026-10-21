package tr.kasaba.clan;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClanStateTest {
    final UUID world=UUID.randomUUID();
    Clan clan(){return Clan.create("KSB","Kasaba","Redkit");}
    @Test void memberCannotBeInTwoClansOrUseAnExistingCaseInsensitiveTag(){
        Clan first=clan();ClanState state=new ClanState(world,"orman",Map.of(first.id(),first));
        assertThrows(IllegalArgumentException.class,()->state.put(Clan.create("ABC","Başka","redKIT")));
        assertThrows(IllegalArgumentException.class,()->state.put(Clan.create("ksb","Başka","Alex")));
    }
    @Test void membershipLimitLeadershipTransferAndKickPreserveLandOwnership(){
        Clan original=clan().claimed(new Claim(1,world,new Rect(100,100,109,109)));
        Clan joined=original.joined("Alex",2).promoted("alex",true);
        assertThrows(IllegalArgumentException.class,()->joined.joined("Steve",2));
        assertThrows(IllegalArgumentException.class,()->joined.removed("redkit"));
        Clan transferred=joined.transferred("ALEX");assertEquals("alex",transferred.leader());assertTrue(transferred.manages("Redkit"));
        Clan removed=transferred.removed("Redkit");assertEquals(original.claims(),removed.claims());assertFalse(removed.contains("redkit"));assertFalse(removed.assistants().contains("redkit"));
        assertEquals(1,original.members().size());
    }
    @Test void claimRemovalDoesNotRenumberOtherParcelsAndWorldCannotChangeWithLand(){
        Clan owned=clan().claimed(new Claim(1,world,new Rect(0,0,9,9))).claimed(new Claim(3,world,new Rect(20,20,29,29)));
        Clan kept=owned.unclaimed(1);assertEquals(3,kept.claims().getFirst().number());assertEquals(100,kept.area());
        ClanState state=new ClanState(world,"orman",Map.of(owned.id(),owned));
        assertThrows(IllegalArgumentException.class,()->state.forest(UUID.randomUUID(),"other"));
        assertThrows(IllegalArgumentException.class,()->owned.unclaimed(2));
    }
    @Test void invalidRanksMixedWorldsAndOverlappingPersistedClaimsAreRejected(){
        Clan clan=clan();
        assertThrows(IllegalArgumentException.class,()->clan.promoted("Alex",true));
        assertThrows(IllegalArgumentException.class,()->new Clan(clan.id(),clan.name(),clan.tag(),"nobody",clan.members(),Set.of(),List.of()));
        assertThrows(IllegalArgumentException.class,()->new ClanState(world,"orman",Map.of(clan.id(),clan.claimed(new Claim(1,UUID.randomUUID(),new Rect(0,0,1,1))))));
        Clan overlapping=clan.claimed(new Claim(1,world,new Rect(0,0,9,9))).claimed(new Claim(2,world,new Rect(9,9,15,15)));
        assertThrows(IllegalArgumentException.class,()->new ClanState(world,"orman",Map.of(clan.id(),overlapping)));
    }
    @Test void formattingCodesAndAmbiguousPlayerKeysCannotBeInjected(){
        assertEquals("redkit",Names.key("Redkit"));assertEquals("KŞB",Names.tag("kşb"));
        for(String tag:List.of("§cABC","[VIP]","A","ABC DEF"))assertThrows(IllegalArgumentException.class,()->Names.tag(tag));
        assertThrows(IllegalArgumentException.class,()->Names.label("   "));assertThrows(IllegalArgumentException.class,()->Names.key("Alex.other"));
        assertEquals("cikar",Names.command("ÇIKAR"));assertEquals("yardimci",Names.command("yardımcı"));
    }
}
