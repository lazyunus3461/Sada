package tr.kasabatr.prison;
import java.util.Locale;
final class PrisonClock {
 static String remaining(long millis){long seconds=Math.max(0,(millis+999)/1000),days=seconds/86400;return(days>0?days+" gün ":"")+String.format(Locale.ROOT,"%02d:%02d:%02d",seconds/3600%24,seconds/60%60,seconds%60);}
}
