package tr.kasabatr.renk;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class RoundRulesTest {
    @Test void everyColorIsEquallyAvailable(){for(int seed=0;seed<100;seed++){int[] cells=RoundRules.layout(new Random(seed));for(int color=0;color<8;color++){final int c=color;assertEquals(8,Arrays.stream(cells).filter(i->i==c).count());}for(int row=0;row<8;row++){var colors=new HashSet<Integer>();for(int x=0;x<8;x++)colors.add(cells[row*8+x]);assertEquals(8,colors.size());}}}
    @Test void deadlineGetsShorterButAllowsMovement(){assertEquals(8000,RoundRules.warningMillis(1));assertEquals(6400,RoundRules.warningMillis(5));assertEquals(2500,RoundRules.warningMillis(100));for(int round=2;round<100;round++)assertTrue(RoundRules.warningMillis(round)<=RoundRules.warningMillis(round-1));}
    @Test void walkingOutsideCannotCountAsAColor(){int[] cells=RoundRules.layout(new Random(1));assertEquals(-1,RoundRules.colorAt(cells,-1,0));assertEquals(-1,RoundRules.colorAt(cells,0,32));assertEquals(cells[0],RoundRules.colorAt(cells,3,3));assertEquals(cells[63],RoundRules.colorAt(cells,31,31));}
}
