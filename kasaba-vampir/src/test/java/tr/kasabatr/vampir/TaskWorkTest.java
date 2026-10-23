package tr.kasabatr.vampir;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TaskWorkTest {
    private TaskWork work(int kind){TaskWork work=new TaskWork(kind,new Random(42));work.begin(1000);return work;}
    @Test void rapidClicksCannotCompleteAnEasyTask(){TaskWork w=work(0);assertEquals(TaskWork.Result.WAIT,w.select(w.sequence[0],1699));assertEquals(0,w.step);assertEquals(TaskWork.Result.STEP,w.select(w.sequence[0],1700));assertEquals(TaskWork.Result.WAIT,w.select(w.sequence[1],1700));assertEquals(1,w.step);}
    @Test void correctSequenceStillNeedsPreparationTime(){TaskWork w=work(0);for(int i=0;i<3;i++)assertEquals(TaskWork.Result.STEP,w.select(w.sequence[i],1800+i*800));assertEquals(TaskWork.Result.WAIT,w.select(w.sequence[3],5999));assertEquals(TaskWork.Result.DONE,w.select(w.sequence[3],6000));assertEquals(TaskWork.Result.IGNORED,w.select(0,9000));}
    @Test void materialSortingContainsAllFourItemsAndTakesSevenSeconds(){TaskWork w=work(1);assertEquals(Set.of(0,1,2,3),Arrays.stream(w.sequence).boxed().collect(java.util.stream.Collectors.toSet()));for(int i=0;i<3;i++)w.select(w.sequence[i],2000+i*1000);assertEquals(TaskWork.Result.WAIT,w.select(w.sequence[3],7999));assertEquals(TaskWork.Result.DONE,w.select(w.sequence[3],8000));}
    @Test void aMistakeRestartsOnlyTheCurrentWork(){TaskWork w=work(0);w.select(w.sequence[0],2000);assertEquals(TaskWork.Result.WRONG,w.select((w.sequence[1]+1)%3,3000));assertEquals(0,w.step);assertEquals(3000,w.startedAt);}
    @Test void memoryHasTwoRoundsWithAnEnforcedPreview(){TaskWork w=work(3);assertEquals(TaskWork.Result.WAIT,w.select(TaskWork.READY,4999));assertEquals(TaskWork.Result.HIDDEN,w.select(TaskWork.READY,5000));long t=6000;for(int i=0;i<3;i++){TaskWork.Result result=w.select(w.sequence[i],t);assertEquals(i==2?TaskWork.Result.ROUND:TaskWork.Result.STEP,result);t+=1000;}assertEquals(1,w.round);assertEquals(3,w.sequence.length);assertTrue(w.preview);assertEquals(TaskWork.Result.HIDDEN,w.select(TaskWork.READY,12000));for(int i=0;i<3;i++)assertEquals(i==2?TaskWork.Result.DONE:TaskWork.Result.STEP,w.select(w.sequence[i],13000+i*1000));}
    @Test void hintAndInterruptionRequireAnotherPreviewButKeepCompletedMemoryRound(){TaskWork w=work(3);w.select(TaskWork.READY,5000);w.select(w.sequence[0],6000);assertEquals(TaskWork.Result.ROUND,w.select(TaskWork.HINT,6100));assertEquals(0,w.step);assertTrue(w.preview);assertEquals(TaskWork.Result.WAIT,w.select(TaskWork.READY,9000));w.round=1;w.interrupt();assertEquals(TaskWork.Result.IGNORED,w.select(0,10000));w.begin(11000);assertEquals(1,w.round);assertEquals(TaskWork.Result.WAIT,w.select(TaskWork.READY,14000));}
    @Test void deliveryRequiresPackingBeforeItCanComplete(){TaskWork w=work(2);assertEquals("Orta",w.difficulty());assertEquals(TaskWork.Result.STEP,w.select(0,2000));assertEquals(TaskWork.Result.STEP,w.select(1,3000));assertEquals(TaskWork.Result.WAIT,w.select(2,4000));assertEquals(TaskWork.Result.DONE,w.select(2,5000));}
}
