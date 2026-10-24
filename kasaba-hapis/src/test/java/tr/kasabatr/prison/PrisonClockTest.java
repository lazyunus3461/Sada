package tr.kasabatr.prison;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PrisonClockTest {
 @Test void rendersSecondsHoursAndLongSentencesWithoutNegativeTime(){assertEquals("00:00:00",PrisonClock.remaining(-1));assertEquals("00:00:01",PrisonClock.remaining(1));assertEquals("00:59:59",PrisonClock.remaining(3599000));assertEquals("01:00:00",PrisonClock.remaining(3600000));assertEquals("2 gün 01:00:00",PrisonClock.remaining(49*3600000L));}
}
