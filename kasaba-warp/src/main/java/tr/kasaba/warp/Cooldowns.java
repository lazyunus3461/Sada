package tr.kasaba.warp;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Real elapsed time; retained across disconnects, independent of server tick rate. */
final class Cooldowns {
    private final Map<String,Long> deadlines=new HashMap<>();
    private final LongSupplier clock;
    Cooldowns(){this(System::nanoTime);}
    Cooldowns(LongSupplier clock){this.clock=clock;}
    void start(String player,int seconds){deadlines.put(key(player),clock.getAsLong()+TimeUnit.SECONDS.toNanos(seconds));}
    int remaining(String player){
        Long deadline=deadlines.get(key(player));if(deadline==null)return 0;
        long left=deadline-clock.getAsLong();
        if(left<=0){deadlines.remove(key(player));return 0;}
        return (int)((left+999_999_999L)/1_000_000_000L);
    }
    void prune(){long now=clock.getAsLong();deadlines.values().removeIf(deadline->deadline-now<=0);}
    private static String key(String player){return player.toLowerCase(Locale.ROOT);}
}
