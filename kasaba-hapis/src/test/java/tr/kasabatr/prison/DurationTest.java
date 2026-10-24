package tr.kasabatr.prison;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DurationTest {
 @Test void onlyBoundedPositiveMinutesAccepted(){for(String s:new String[]{"1","60dk","100m","43200DK"})assertTrue(KasabaHapisPlugin.parseMinutes(s,43200)>0);for(String s:new String[]{"0","-1","1.5","43201","9999999999999999999999999","10s","3 saat"})assertEquals(-1,KasabaHapisPlugin.parseMinutes(s,43200),s);}
}
