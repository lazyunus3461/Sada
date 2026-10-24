package tr.kasabatr.ispanya;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class BoardLayoutTest {
    @Test void hundredMatchesPreserveTileCountsAndHaveBuildableRandomBridges(){
        List<BoardLayout.Node> nodes=new ArrayList<>();for(int row=0;row<7;row++)for(int col=0;col<19;col++)nodes.add(new BoardLayout.Node((row%2==0?col:18-col)*4-36,row*10,"plain"));
        String[] specials={"red","green","purple","black"};for(int i=4;i<44;i++)nodes.set(i,new BoardLayout.Node(nodes.get(i).x(),nodes.get(i).z(),specials[i%4]));
        Set<String> seen=new HashSet<>();for(int seed=0;seed<100;seed++){
            BoardLayout layout=BoardLayout.create(nodes,3,new Random(seed));assertEquals(3,layout.shortcuts.size());assertEquals("start",layout.kinds.getFirst());assertEquals("finish",layout.kinds.getLast());
            for(String color:specials)assertEquals(10,layout.kinds.stream().filter(color::equals).count());assertEquals(3,layout.kinds.stream().filter("yellow"::equals).count());
            Set<Integer> used=new HashSet<>();layout.shortcuts.forEach((a,b)->{assertTrue(used.add(a));assertTrue(used.add(b));assertEquals(nodes.get(a).x(),nodes.get(b).x());assertEquals(10,Math.abs(nodes.get(a).z()-nodes.get(b).z()));assertTrue(b-a>=12);assertEquals("yellow",layout.kinds.get(a));assertEquals("plain",layout.kinds.get(b));});
            seen.add(layout.kinds.toString()+layout.shortcuts);
        }assertEquals(100,seen.size());
    }
}
