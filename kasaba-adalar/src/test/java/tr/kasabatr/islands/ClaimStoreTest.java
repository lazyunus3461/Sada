package tr.kasabatr.islands;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
class ClaimStoreTest {
    @TempDir Path dir;
    private ClaimStore store(Path path){return new ClaimStore(path,Logger.getAnonymousLogger());}
    @Test void repeatedClaimNeverAllocatesASecondIslandAndPersists()throws Exception{Path file=dir.resolve("claims.properties");UUID player=UUID.randomUUID();ClaimStore claims=store(file);claims.load();for(int i=0;i<10;i++)assertEquals(1,claims.claim(player));assertEquals(1,claims.size());claims=store(file);claims.load();assertEquals(1,claims.claim(player));assertEquals(1,claims.size());assertEquals(2,claims.claim(UUID.randomUUID()));}
    @Test void friendAccessDoesNotGrantAnotherOwnership()throws Exception{Path file=dir.resolve("claims.properties");ClaimStore claims=store(file);UUID a=UUID.randomUUID(),b=UUID.randomUUID();claims.claim(a);claims.claim(b);assertFalse(claims.canAccess(1,b));assertTrue(claims.addMember(1,b));assertEquals(2,claims.islandOf(b));claims=store(file);claims.load();assertTrue(claims.canAccess(1,b));assertFalse(claims.canAccess(0,b));assertTrue(claims.removeMember(1,b));assertFalse(claims.canAccess(1,b));assertFalse(claims.addMember(1,a));}
    @Test void oldPropertyFormatAndNamesRemainCompatible()throws Exception{Path file=dir.resolve("claims.properties");UUID a=UUID.randomUUID(),b=UUID.randomUUID();Files.writeString(file,"island.7="+a+"\nname."+a+"=Redkit\nmember.7."+b+"=true\n");ClaimStore claims=store(file);claims.load();assertEquals(7,claims.islandOf(a));assertEquals(a,claims.playerByName("redkit"));assertTrue(claims.canAccess(7,b));assertEquals(1,claims.claim(b));assertTrue(claims.canAccess(7,b));}
    @Test void invalidDuplicateOwnershipFailsWithoutOverwriting()throws Exception{UUID a=UUID.randomUUID();Path file=dir.resolve("claims.properties");String original="island.1="+a+"\nisland.2="+a+"\n";Files.writeString(file,original);ClaimStore claims=store(file);assertThrows(java.io.IOException.class,claims::load);assertEquals(original,Files.readString(file));assertEquals(0,claims.size());}
    @Test void failedSaveDoesNotReserveTheIsland()throws Exception{Path target=dir.resolve("claims.properties");Files.createDirectory(target);Files.writeString(target.resolve("keep"),"preserve");ClaimStore claims=store(target);UUID a=UUID.randomUUID();assertThrows(java.io.IOException.class,()->claims.claim(a));assertEquals(0,claims.size());assertNull(claims.islandOf(a));assertEquals("preserve",Files.readString(target.resolve("keep")));}
    @Test void fullGridRejectsExtraPlayersWithoutReplacingOwners()throws Exception{ClaimStore claims=store(dir.resolve("claims.properties"));for(int i=1;i<=300;i++)assertEquals(i,claims.claim(UUID.randomUUID()));assertEquals(0,claims.claim(UUID.randomUUID()));assertEquals(300,claims.size());}
}
