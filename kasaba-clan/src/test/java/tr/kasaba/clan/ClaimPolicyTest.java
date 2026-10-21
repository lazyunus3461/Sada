package tr.kasaba.clan;

import org.junit.jupiter.api.Test;
import tr.kasaba.warp.api.WarpDestination;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClaimPolicyTest {
    final UUID world=UUID.randomUUID(),other=UUID.randomUUID();
    final Clan clan=Clan.create("KSB","Kasaba","Redkit");
    final ClanState state=new ClanState(world,"orman",Map.of(clan.id(),clan));
    void check(Rect r,List<WarpDestination> warps){ClaimPolicy.check(state,clan,world,r,new ClaimIndex(state),warps,5000,20,1024);}
    @Test void warpZoneContainsExactlyOneHundredBlocksOnBothAxes(){
        Rect r=Rect.warp(.5,.5);assertEquals(100,r.width());assertEquals(100,r.depth());assertEquals(10000,r.area());
        assertTrue(r.contains(-50,-50));assertTrue(r.contains(49,49));assertFalse(r.contains(50,0));assertFalse(r.contains(-51,0));
        Rect negative=Rect.warp(-.5,-.5);assertEquals(-51,negative.minX());assertEquals(48,negative.maxX());
    }
    @Test void partialOverlapIsRejectedEvenWhenBothCornerPlayersCouldStandOutsideTheWarpZone(){
        var warp=new WarpDestination("orman",world,.5,.5);
        assertThrows(IllegalArgumentException.class,()->check(new Rect(-60,-1,60,1),List.of(warp)));
        assertThrows(IllegalArgumentException.class,()->check(new Rect(49,49,55,55),List.of(warp)));
        assertDoesNotThrow(()->check(new Rect(50,0,60,10),List.of(warp)));
    }
    @Test void extraAndPrivateWarpTargetsAreCoveredButOtherWorldsDoNotCollide(){
        var targets=List.of(new WarpDestination("orman",world,0,0),new WarpDestination("orman",world,1000,1000),new WarpDestination("vip",world,2000,2000));
        assertThrows(IllegalArgumentException.class,()->check(new Rect(1000,1000,1001,1001),targets));
        var rejection=assertThrows(IllegalArgumentException.class,()->check(new Rect(2000,2000,2001,2001),targets));assertFalse(rejection.getMessage().contains("vip"));
        assertDoesNotThrow(()->check(new Rect(0,0,1,1),List.of(new WarpDestination("orman",other,0,0))));
    }
    @Test void onlyConfiguredForestAndTheTotalClanBudgetAreAllowed(){
        var index=new ClaimIndex(state);
        assertThrows(IllegalArgumentException.class,()->ClaimPolicy.check(state,clan,other,new Rect(0,0,1,1),index,List.of(),5000,20,1024));
        assertThrows(IllegalArgumentException.class,()->ClaimPolicy.check(ClanState.empty(),clan,world,new Rect(0,0,1,1),index,List.of(),5000,20,1024));
        Clan owned=clan.claimed(new Claim(1,world,new Rect(0,0,49,49)));ClanState with=state.put(owned);
        assertDoesNotThrow(()->ClaimPolicy.check(with,owned,world,new Rect(100,100,149,149),new ClaimIndex(with),List.of(),5000,20,1024));
        assertThrows(IllegalArgumentException.class,()->ClaimPolicy.check(with,owned,world,new Rect(100,100,150,149),new ClaimIndex(with),List.of(),5000,20,1024));
    }
    @Test void ownAndForeignOverlapsAndExcessiveSideOrCountAreRejected(){
        Clan owned=clan.claimed(new Claim(1,world,new Rect(100,100,110,110)));ClanState with=state.put(owned);var index=new ClaimIndex(with);
        assertThrows(IllegalArgumentException.class,()->ClaimPolicy.check(with,owned,world,new Rect(105,105,120,120),index,List.of(),5000,20,1024));
        assertThrows(IllegalArgumentException.class,()->ClaimPolicy.check(with,owned,world,new Rect(500,500,501,501),index,List.of(),5000,1,1024));
        assertThrows(IllegalArgumentException.class,()->check(new Rect(100,100,1124,100),List.of()));
        Clan foreign=Clan.create("ABC","Başka Klan","Alex");
        assertThrows(IllegalArgumentException.class,()->ClaimPolicy.check(with,foreign,world,new Rect(105,105,106,106),index,List.of(),5000,20,1024));
    }
    @Test void warpCreationAndMovementCannotSilentlyCoverAnExistingClaim(){
        Clan owned=clan.claimed(new Claim(1,world,new Rect(100,100,110,110)));ClaimIndex index=new ClaimIndex(state.put(owned));
        assertTrue(ClaimPolicy.warpRejection(index,List.of(new WarpDestination("orman",world,60,60))).isPresent());
        assertTrue(ClaimPolicy.warpRejection(index,List.of(new WarpDestination("orman",world,-100,-100))).isEmpty());
        assertEquals(1,owned.claims().size());
    }
    @Test void negativeCoordinatesAndAdjacentClaimsHaveNoOffByOneGap(){
        Clan owned=clan.claimed(new Claim(1,world,new Rect(-32,-32,-17,-17)));ClaimIndex index=new ClaimIndex(state.put(owned));
        assertNotNull(index.at(world,-32,-32));assertNotNull(index.at(world,-17,-17));assertNull(index.at(world,-16,-17));assertNull(index.at(other,-32,-32));
        assertNotNull(index.overlap(world,new Rect(-18,-18,-16,-16)));assertNull(index.overlap(world,new Rect(-16,-32,-1,-17)));
    }
}
