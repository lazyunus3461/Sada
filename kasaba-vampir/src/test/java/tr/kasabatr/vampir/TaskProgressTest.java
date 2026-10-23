package tr.kasabatr.vampir;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskProgressTest {
    @Test void realAndFakeWorkMakeIdenticalPublicIncrements() {
        assertEquals(TaskProgress.visible(1,0,15), TaskProgress.visible(0,1,15));
        assertEquals(TaskProgress.visible(2,0,15), TaskProgress.visible(1,1,15));
        assertTrue(TaskProgress.visible(1,1,15) > TaskProgress.visible(1,0,15));
    }

    @Test void fakeWorkNeverAnnouncesCompletedVictory() {
        assertEquals(.99, TaskProgress.visible(0,100,15));
        assertEquals(.99, TaskProgress.visible(14,1,15));
        assertEquals(.99, TaskProgress.visible(15,0,15));
    }

    @Test void progressIsBoundedEvenWithBadCountsAndOverflow() {
        assertEquals(0, TaskProgress.visible(1,1,0));
        assertEquals(0, TaskProgress.visible(-1,-1,15));
        assertEquals(.99, TaskProgress.visible(Integer.MAX_VALUE,Integer.MAX_VALUE,15));
    }
}
