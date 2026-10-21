package tr.kasaba.warp;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class MenuLayoutTest {
    Warp point(String id,int slot){return new Warp(id,id,"",false,UUID.randomUUID(),"world",0,64,0,0,0).displayed("",slot);}
    @Test void rowAndColumnMatchInventoryCellsAndProtectFooter() {
        assertEquals(2,MenuLayout.parse("1-3"));assertEquals(44,MenuLayout.parse("5-9"));assertEquals("1-3",MenuLayout.label(2));
        for(String invalid:List.of("0-1","1-0","6-1","1-10","1:3","1-03"))assertThrows(IllegalArgumentException.class,()->MenuLayout.parse(invalid));
    }
    @Test void automaticEntriesCannotOverwriteFixedOrHiddenSlots() {
        var fixed=point("orman",2);var auto=point("pazar",-1);
        var layout=MenuLayout.plan(List.of(fixed,auto),Set.of(0,2));
        assertEquals(fixed,layout.get(2));assertEquals(auto,layout.get(1));assertFalse(layout.containsKey(0));
    }
    @Test void overflowKeepsPaginationWithoutCollisions() {
        List<Warp> visible=new ArrayList<>();visible.add(point("orman",44));
        for(int i=0;i<45;i++)visible.add(point("test"+i,-1));
        var layout=MenuLayout.plan(visible,Set.of(44));assertEquals(46,layout.size());assertEquals("orman",layout.get(44).id());assertTrue(layout.containsKey(45));
    }
    @Test void duplicatePositionsFailBeforeSavingAndMetadataEditsKeepPosition() {
        var first=point("orman",2);var second=point("pazar",2);
        assertThrows(IllegalArgumentException.class,()->MenuLayout.validate(List.of(first,second)));
        assertEquals(2,first.renamed("Orman").described("Ağaçlar").restricted(true).menuSlot());
    }
}
