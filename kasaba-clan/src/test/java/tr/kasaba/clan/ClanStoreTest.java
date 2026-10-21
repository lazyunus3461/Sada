package tr.kasaba.clan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClanStoreTest {
    @TempDir Path directory;
    @Test void forestMembersRanksAndExactClaimBoundsRoundTripAfterRestart() throws IOException {
        UUID world=UUID.randomUUID();Clan clan=Clan.create("KSB","Kasaba Türkçe","Redkit").joined("Alex",10).promoted("Alex",true).claimed(new Claim(8,world,new Rect(-33,200,-17,250)));
        ClanState state=new ClanState(world,"orman_dunyasi",Map.of(clan.id(),clan));Path path=directory.resolve("klanlar.yml");
        new ClanStore(path).save(state);assertEquals(state,new ClanStore(path).load());
        ClanState removed=state.put(clan.removed("Alex").unclaimed(8));new ClanStore(path).save(removed);assertEquals(removed,new ClanStore(path).load());
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void missingAndEmptyStateCanBeSafelyCreated() throws IOException {
        ClanStore store=new ClanStore(directory.resolve("klanlar.yml"));assertEquals(ClanState.empty(),store.load());store.save(ClanState.empty());assertEquals(ClanState.empty(),store.load());
    }
    @Test void malformedAndPartialFilesAreNeverSilentlyReset() throws IOException {
        Path path=directory.resolve("klanlar.yml");ClanStore store=new ClanStore(path);
        for(String data:List.of("surum: [","surum: 99\nklanlar: {}","surum: 1\nklanlar: hello","surum: 1\nklanlar: {}\norman: hello")){
            Files.writeString(path,data);assertThrows(IOException.class,store::load);assertEquals(data,Files.readString(path));
        }
    }
    @Test void defaultLimitsMatchUserChoiceAndInvalidSettingsAreRejected() throws IOException {
        Path path=directory.resolve("config.yml");Files.writeString(path,"{}");Settings settings=Settings.read(path);assertEquals(10,settings.members());assertEquals(5000,settings.area());
        for(String data:List.of("uye-siniri: -1","toplam-alan-siniri: 5000.5","etiket-rengi: nonsense","kenar-siniri: 500000")){
            Files.writeString(path,data);assertThrows(IOException.class,()->Settings.read(path));
        }
    }
}
