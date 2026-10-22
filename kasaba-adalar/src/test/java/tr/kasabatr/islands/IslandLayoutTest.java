package tr.kasabatr.islands;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IslandLayoutTest {
    @Test void allThreeHundredPlotsHaveStableIdsAndHalfSpacing(){IslandLayout layout=new IslandLayout(100);for(int id=1;id<=300;id++){int x=layout.centerX(id),z=layout.centerZ(id);assertEquals(id,layout.at(x,z));assertTrue(layout.contains(id,x-48,z-48));assertTrue(layout.contains(id,x+47,z+47));assertFalse(layout.contains(id,x+48,z));if(id%20!=0)assertEquals(100,layout.centerX(id+1)-x);}assertEquals(0,layout.at(48,0));assertEquals(0,layout.at(51,0));assertEquals(2,layout.at(52,0));assertEquals(0,layout.at(-49,0));}
    @Test void oldUnmarkedWorldStillResolvesAtTwoHundred(){IslandLayout old=new IslandLayout(200);assertEquals(200,old.centerX(2));assertEquals(200,old.centerZ(21));assertEquals(2,old.at(200,0));assertEquals(0,old.at(100,0));assertThrows(IllegalArgumentException.class,()->new IslandLayout(50));}
}
