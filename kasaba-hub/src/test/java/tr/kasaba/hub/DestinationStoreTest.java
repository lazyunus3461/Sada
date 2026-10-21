package tr.kasaba.hub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DestinationStoreTest {
    @TempDir Path directory;
    private DestinationStore.Destination destination(String id,String name){return new DestinationStore.Destination(id,name,new LobbyStore.Point(UUID.randomUUID(),"oyun_dunyasi",500.5,64,-20.25,125,-13));}
    @Test void entrancesSurviveRestartWithExactWorldNameAndFacing() throws IOException {
        Path path=directory.resolve("oyunlar.yml");var store=new DestinationStore(path);assertTrue(store.load().isEmpty());
        var games=Map.of("parkur",destination("parkur","Kasaba Parkur"),"yaris",destination("yaris","Arkadaş Yarışı"));store.save(games);
        assertEquals(games,new DestinationStore(path).load());try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void deletionOfLastEntrancePersistsAsAnEmptyCollection() throws IOException {
        var store=new DestinationStore(directory.resolve("oyunlar.yml"));store.save(Map.of("parkur",destination("parkur","Parkur")));store.save(Map.of());assertEquals(Map.of(),store.load());
    }
    @Test void invalidOrPartialFileIsNeverSilentlyReset() throws IOException {
        Path path=directory.resolve("oyunlar.yml");var store=new DestinationStore(path);
        for(String broken:List.of("oyunlar: [","hello: world","oyunlar:\n  parkur:\n    ad: Parkur\n","oyunlar:\n  parkur: false")){
            Files.writeString(path,broken);assertThrows(IOException.class,store::load);assertEquals(broken,Files.readString(path));
        }
        store.save(Map.of("parkur",destination("parkur","Parkur")));String content=Files.readString(path);
        Files.writeString(path,content.replaceFirst("x: 500.5","x: .NaN"));assertThrows(IOException.class,store::load);
    }
    @Test void unsafeIdsAndInvalidNamesCannotBecomePathsOrCommands() throws IOException {
        assertThrows(IllegalArgumentException.class,()->destination("../bad","Bad"));assertThrows(IllegalArgumentException.class,()->destination("op redkit","Bad"));assertThrows(IllegalArgumentException.class,()->destination("game"," "));
        Path path=directory.resolve("oyunlar.yml");assertThrows(IllegalArgumentException.class,()->new DestinationStore(path).save(Map.of("other",destination("parkur","Parkur"))));assertFalse(Files.exists(path));
    }
}
