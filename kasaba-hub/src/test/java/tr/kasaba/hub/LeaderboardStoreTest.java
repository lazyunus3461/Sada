package tr.kasaba.hub;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class LeaderboardStoreTest {
 @TempDir Path dir;
 @Test void teamResultsAreCountedOncePersistedAndSeparatedByGame()throws Exception{
  var file=dir.resolve("scores.properties");var store=new LeaderboardStore(file);var one=UUID.randomUUID();var two=UUID.randomUUID();var match=UUID.randomUUID();var winners=Map.of(one,"Redkit",two,"Dalton");assertTrue(store.result("battle",match,winners));assertFalse(store.result("battle",match,winners));assertEquals(2,store.top("battle").size());assertTrue(store.top("renk").isEmpty());var restored=new LeaderboardStore(file);restored.load();assertFalse(restored.result("battle",match,winners));assertEquals(1,restored.top("battle").getFirst().score());
 }
 @Test void topFiveHasStableTiesAndOldTotalsCannotDecrease()throws Exception{
  var store=new LeaderboardStore(dir.resolve("scores"));for(int i=0;i<8;i++)store.total("arena",UUID.randomUUID(),"Player"+i,i);assertEquals(5,store.top("arena").size());assertEquals(7,store.top("arena").getFirst().score());UUID id=store.top("arena").getFirst().id();store.total("arena",id,"Rename",1);assertEquals(7,store.top("arena").getFirst().score());
 }
 @Test void damagedFileIsNotOverwritten()throws Exception{var file=dir.resolve("bad");Files.writeString(file,"battle/invalid=nan:Redkit");assertThrows(java.io.IOException.class,()->new LeaderboardStore(file).load());assertEquals("battle/invalid=nan:Redkit",Files.readString(file));}
}
