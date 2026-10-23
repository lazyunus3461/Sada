package tr.kasabatr.vampir;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroundRouteTest {
    @Test void takesTheDoorwayInsteadOfPointingThroughAWall() {
        // Solid wall x=2, with its only door at z=4; station is on the far side.
        var grid=new GroundRoute(0,0,6,6,(x,z)->x!=2||z==4);
        var field=grid.toward(5,0);var path=field.steps(0,0,100);
        assertTrue(path.contains(new GroundRoute.Cell(2,4)));
        assertEquals(12,field.distance(0,0));
        assertEquals(1,Math.abs(path.getLast().x()-5)+Math.abs(path.getLast().z()));
        for(int i=1;i<path.size();i++)assertEquals(1,Math.abs(path.get(i).x()-path.get(i-1).x())+Math.abs(path.get(i).z()-path.get(i-1).z()));
        assertTrue(path.stream().noneMatch(p->p.x()==2&&p.z()!=4));
    }

    @Test void cannotCutADiagonalCornerOrCrossAnUnreachableWall() {
        var diagonal=new GroundRoute(0,0,3,3,(x,z)->x==z).toward(2,1);
        assertEquals(-1,diagonal.distance(0,0));assertTrue(diagonal.steps(0,0,20).isEmpty());
        var closed=new GroundRoute(0,0,6,6,(x,z)->x!=2).toward(5,0);
        assertTrue(closed.steps(0,0,20).isEmpty());
        assertTrue(closed.steps(-1,0,20).isEmpty());
    }

    @Test void updatesFromCurrentBlockAndStopsNextToSolidWorkstation() {
        var grid=new GroundRoute(-3,-3,7,7,(x,z)->x!=0||z!=0);
        var field=grid.toward(0,0);
        assertEquals(3,field.distance(-3,1));
        var path=field.steps(-3,1,3);assertEquals(3,path.size());
        var moved=field.steps(path.get(1).x(),path.get(1).z(),20);
        assertEquals(field.distance(-3,1)-1,field.distance(moved.getFirst().x(),moved.getFirst().z()));
        assertEquals(1,Math.abs(moved.getLast().x())+Math.abs(moved.getLast().z()));
        assertTrue(field.steps(0,0,20).isEmpty());
    }

    @Test void eachTargetKeepsItsOwnPathAndPreviewIsBounded() {
        var grid=new GroundRoute(0,0,20,20,(x,z)->true);
        var a=grid.toward(19,19);var b=grid.toward(0,0);
        assertEquals(37,a.distance(0,0));assertEquals(0,b.distance(1,0));
        assertEquals(11,a.steps(0,0,11).size());assertEquals(0,a.steps(0,0,0).size());
        assertEquals(1,a.steps(19,18,11).size());assertTrue(a.steps(19,19,11).isEmpty());
    }
}
