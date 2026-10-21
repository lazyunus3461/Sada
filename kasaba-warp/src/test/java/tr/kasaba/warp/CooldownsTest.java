package tr.kasaba.warp;

import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class CooldownsTest {
    @Test void completeTwentySecondsMustElapseBeforeNextUse() {
        var time=new AtomicLong();var cooldowns=new Cooldowns(time::get);cooldowns.start("Redkit",20);
        assertEquals(20,cooldowns.remaining("Redkit"));time.set(TimeUnit.SECONDS.toNanos(19)+999_999_999);
        assertEquals(1,cooldowns.remaining("Redkit"));time.incrementAndGet();assertEquals(0,cooldowns.remaining("Redkit"));
    }
    @Test void playerNamesAreCaseInsensitiveAndIndependent() {
        var time=new AtomicLong();var cooldowns=new Cooldowns(time::get);cooldowns.start("Redkit",20);
        assertEquals(20,cooldowns.remaining("REDKIT"));assertEquals(0,cooldowns.remaining("AnotherPlayer"));
    }
    @Test void noSuccessfulTravelMeansNoCooldownAndPruningKeepsActivePlayers() {
        var time=new AtomicLong();var cooldowns=new Cooldowns(time::get);assertEquals(0,cooldowns.remaining("Redkit"));
        cooldowns.start("Redkit",20);time.set(TimeUnit.SECONDS.toNanos(5));cooldowns.start("Other",20);
        time.set(TimeUnit.SECONDS.toNanos(20));cooldowns.prune();assertEquals(0,cooldowns.remaining("Redkit"));assertEquals(5,cooldowns.remaining("Other"));
    }
}
