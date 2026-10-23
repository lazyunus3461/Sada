package tr.kasaba.social;

import java.math.BigDecimal;
import java.util.Map;

final class NetworkRules {
 static final long DAY=86_400_000L,HOUR=3_600_000L;
 static final Map<Integer,BigDecimal> PRICES=Map.of(3,new BigDecimal("40.00"),7,new BigDecimal("75.00"),15,new BigDecimal("145.00"),30,new BigDecimal("250.00"));
 static long expiry(long now,long existing,int days){if(!PRICES.containsKey(days))throw new IllegalArgumentException("VIP süresi 3, 7, 15 veya 30 gün olmalı.");return Math.addExact(Math.max(now,existing),days*DAY);}
 static int sellable(int owned,int alreadySold,long lockUntil,long now){return lockUntil>now?0:Math.max(0,Math.min(owned,15-alreadySold));}
 static boolean earns(boolean game,boolean social,boolean spectator,long lastAction,long now){return game&&!social&&!spectator&&lastAction>0&&now-lastAction<=120_000;}
}
