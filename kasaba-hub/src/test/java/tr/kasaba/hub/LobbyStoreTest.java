package tr.kasaba.hub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tr.kasaba.hub.api.GameInfo;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LobbyStoreTest {
    @TempDir Path directory;
    @Test void lobbyRoundTripKeepsUuidExactCoordinatesAndFacing() throws IOException{
        LobbyStore store=new LobbyStore(directory.resolve("lobi.yml"));var point=new LobbyStore.Point(UUID.randomUUID(),"lobi",12.5,64,-32.5,123,-12);assertNull(store.load());store.save(point);assertEquals(point,new LobbyStore(directory.resolve("lobi.yml")).load());
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void badOrPartialDataDoesNotResetOrOverwriteTheFile() throws IOException{
        Path path=directory.resolve("lobi.yml");LobbyStore store=new LobbyStore(path);
        for(String broken:List.of("x: [","x: 1\ny: 2\nz: 3\n","hello: world")){Files.writeString(path,broken);assertThrows(IOException.class,store::load);assertEquals(broken,Files.readString(path));}
    }
    @Test void protectionIsBoundedAndWorldSpecific(){
        UUID world=UUID.randomUUID();var point=new LobbyStore.Point(world,"lobi",.5,64,.5,0,0);
        assertTrue(point.contains(world,100.5,100.5,100));assertFalse(point.contains(world,100.6,.5,100));assertFalse(point.contains(UUID.randomUUID(),.5,.5,100));
    }
    @Test void settingsRejectInvalidLimitsBeforeReplacingTheWorkingConfiguration() throws IOException{
        Path path=directory.resolve("config.yml");Files.writeString(path,"{}");assertEquals(8,HubSettings.read(path).capacity());
        for(String broken:List.of("grup-uye-siniri: 1","davet-suresi-saniye: false","otomatik-sayfa: hello","lobi-koruma-yaricapi: 500000")){Files.writeString(path,broken);assertThrows(IOException.class,()->HubSettings.read(path));}
    }
    @Test void invalidGameMetadataAndLobbyNumbersAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new GameInfo("../bad","Oyun","",1,8));assertThrows(IllegalArgumentException.class,()->new GameInfo("oyun","Oyun","",8,2));
        assertThrows(IllegalArgumentException.class,()->new LobbyStore.Point(UUID.randomUUID(),"lobi",Double.NaN,64,0,0,0));
    }
}
