package tr.kasaba.hub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;import java.util.*;
class MatchPenaltiesTest {
 @TempDir Path dir;
 @Test void isolatedPersistentFifteenMinuteBanCannotBeExtendedByDuplicateExit()throws Exception{var id=UUID.randomUUID();var store=new MatchPenalties(dir.resolve("penalties.properties"));store.load();assertTrue(store.ban(id,"ispanya",1000));assertFalse(store.ban(id,"ispanya",2000));assertEquals(0,store.remaining(id,"renk",2000));assertEquals(0,store.remaining(UUID.randomUUID(),"ispanya",2000));var restored=new MatchPenalties(dir.resolve("penalties.properties"));restored.load();assertEquals(899000,restored.remaining(id,"ispanya",2000));assertEquals(0,restored.remaining(id,"ispanya",901000));}
 @Test void corruptFileIsKept()throws Exception{Path file=dir.resolve("bad.properties");Files.writeString(file,"not-a-record=bad");assertThrows(java.io.IOException.class,()->new MatchPenalties(file).load());assertEquals("not-a-record=bad",Files.readString(file));}
}
