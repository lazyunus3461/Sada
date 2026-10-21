package tr.kasaba.warp;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WarpPointsTest {
    final UUID world=UUID.randomUUID();
    final WarpPoint second=new WarpPoint(32.5,70,-18.5,123,-12);
    final WarpPoint third=new WarpPoint(-75.5,65,98.5,-70,21);
    Warp original(){return new Warp("orman","Orman","Ağaçlık alan",true,world,"orman_dunyasi",0.5,64,0.5,0,0).displayed("AQID",2);}
    @Test void onlySavedPointsCanBeSelectedAndAllPointsAreReachable(){
        Warp warp=original().addedPoint(world,second).addedPoint(world,third);
        Set<WarpPoint> selected=new HashSet<>();Random random=new Random(412);
        for(int i=0;i<100;i++){WarpPoint point=warp.choosePoint(random);assertTrue(warp.points().contains(point));selected.add(point);}
        assertEquals(new HashSet<>(warp.points()),selected);
        assertEquals(original().points().getFirst(),original().choosePoint(random));
    }
    @Test void editsAndFirstPointRemovalRetainRemainingLocationsAndAppearance(){
        Warp warp=original().addedPoint(world,second).addedPoint(world,third)
                .renamed("Büyük Orman").described("Yeni açıklama").restricted(false).removedPoint(1);
        assertEquals(List.of(second,third),warp.points());assertEquals(2,warp.menuSlot());assertEquals("AQID",warp.icon());
        assertEquals("Büyük Orman",warp.name());assertFalse(warp.restricted());assertEquals(world,warp.world());
        Warp moved=warp.moved(world,"orman_dunyasi",new WarpPoint(4,65,6,50,0));
        assertEquals(third,moved.extraPoints().getFirst());assertEquals("AQID",moved.icon());
    }
    @Test void invalidPointEditsCannotLoseOrMixWorldData(){
        Warp warp=original().addedPoint(world,second);
        assertThrows(IllegalArgumentException.class,()->warp.addedPoint(UUID.randomUUID(),third));
        assertThrows(IllegalArgumentException.class,()->warp.moved(UUID.randomUUID(),"other",third));
        assertThrows(IllegalArgumentException.class,()->warp.addedPoint(world,new WarpPoint(second.x(),second.y(),second.z(),0,0)));
        assertThrows(IllegalArgumentException.class,()->warp.moved(world,"world",second));
        assertThrows(IllegalArgumentException.class,()->warp.removedPoint(0));
        assertThrows(IllegalArgumentException.class,()->warp.removedPoint(3));
        assertThrows(IllegalArgumentException.class,()->original().removedPoint(1));
        assertEquals(2,warp.points().size());
    }
    @Test void singlePointWarpCanStillMoveToAnotherWorld(){
        UUID other=UUID.randomUUID();Warp moved=original().moved(other,"other",second);
        assertEquals(other,moved.world());assertEquals(List.of(second),moved.points());
    }
}
