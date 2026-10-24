package tr.kasaba.social;
import java.time.*;

/** Accumulated active match time; a calendar day in the server community's timezone. */
final class ActivityRules {
 static final long INTERVAL=15*60_000L;
 static final int DAILY_LIMIT=5;
 static final ZoneId ZONE=ZoneId.of("Europe/Istanbul");
 record Result(long carry,String day,int claims,boolean reward){}
 static String day(long now){return Instant.ofEpochMilli(now).atZone(ZONE).toLocalDate().toString();}
 static Result add(long carry,String previousDay,int previousClaims,long elapsed,long now){
  String day=day(now);int count=day.equals(previousDay)?Math.clamp(previousClaims,0,DAILY_LIMIT):0;carry=Math.max(0,carry);
  if(count>=DAILY_LIMIT)return new Result(Math.min(carry,INTERVAL-1),day,count,false);
  long total=Math.addExact(carry,Math.max(0,elapsed));boolean reward=total>=INTERVAL;
  return new Result(reward?total-INTERVAL:total,day,count+(reward?1:0),reward);
 }
}
