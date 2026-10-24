package tr.kasabatr.yaris;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class LapProgressTest {
 @Test void mustCompleteBothLapsInCheckpointOrder(){var p=new LapProgress(6,2);assertEquals(LapProgress.Finish.NOT_READY,p.recordFinish());assertFalse(p.recordCheckpoint(2));for(int lap=1;lap<=2;lap++){for(int c=0;c<6;c++){assertTrue(p.recordCheckpoint(c));assertFalse(p.recordCheckpoint(c));}assertEquals(lap==1?LapProgress.Finish.NEXT_LAP:LapProgress.Finish.RACE_FINISHED,p.recordFinish());}assertTrue(p.complete());assertFalse(p.recordCheckpoint(0));assertEquals(LapProgress.Finish.NOT_READY,p.recordFinish());}
 @Test void invalidCourseCannotStart(){assertThrows(IllegalArgumentException.class,()->new LapProgress(0,2));assertThrows(IllegalArgumentException.class,()->new LapProgress(6,0));}
}
