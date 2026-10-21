package tr.kasaba.warp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WarpStoreTest {
    @TempDir Path directory;
    Warp point(String id,boolean restricted){return new Warp(id,"Pazar Yeri","Alışveriş alanı",restricted,UUID.randomUUID(),"world",12.5,64,-7.75,125,-12);}
    @Test void roundTripRetainsWorldFacingLabelsAndPrivateAccess() throws IOException {
        Path path=directory.resolve("warplar.yml");var point=point("pazar",true);
        new WarpStore(path).save(Map.of(point.id(),point));
        assertEquals(Map.of("pazar",point),new WarpStore(path).load());
    }
    @Test void overwriteAndDeletionDoNotLeaveStaleWarpsOrTemporaryFiles() throws IOException {
        Path path=directory.resolve("warplar.yml");var store=new WarpStore(path);var pazar=point("pazar",false);
        store.save(Map.of("pazar",pazar,"orman",point("orman",false)));
        store.save(Map.of("pazar",pazar.renamed("Yeni Pazar")));
        assertEquals(Set.of("pazar"),store.load().keySet());assertEquals("Yeni Pazar",store.load().get("pazar").name());
        store.save(Map.of());assertTrue(new WarpStore(path).load().isEmpty());
        try(var files=Files.list(directory)){assertEquals(1,files.count());}
    }
    @Test void malformedOrPartialDataIsRejectedWithoutChangingFile() throws IOException {
        Path path=directory.resolve("warplar.yml");var store=new WarpStore(path);
        for(String broken:List.of("warps: [","warps: hello","warps:\n  pazar:\n    x: 1\n")) {
            Files.writeString(path,broken);assertThrows(IOException.class,store::load);assertEquals(broken,Files.readString(path));
        }
    }
    @Test void missingFileStartsEmpty() throws IOException {assertTrue(new WarpStore(directory.resolve("missing.yml")).load().isEmpty());}
    @Test void turkishNamesNormalizeConsistentlyAndCommandsCannotBeWarpIds() {
        assertEquals("carsi",Warp.id("Çarşı"));assertEquals("istanbul",Warp.id("İSTANBUL"));
        assertThrows(IllegalArgumentException.class,()->point("oluştur",false));
        for(String invalid:List.of("../secret","a.b","a b",""))assertThrows(IllegalArgumentException.class,()->Warp.id(invalid));
    }
    @Test void invalidNumericPositionsAndTooLongLabelsAreRejected() {
        UUID world=UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,()->new Warp("pazar","Pazar","",false,world,"world",Double.NaN,64,0,0,0));
        assertThrows(IllegalArgumentException.class,()->new Warp("pazar","Pazar","",false,world,"world",0,64,0,0,91));
        assertThrows(IllegalArgumentException.class,()->point("pazar",false).renamed("A".repeat(65)));
    }
    @Test void oldFilesDefaultToAutomaticPlacementAndIconPayloadSurvivesSave() throws IOException {
        Path path=directory.resolve("warplar.yml");var store=new WarpStore(path);Warp original=point("orman",false);
        store.save(Map.of("orman",original));assertEquals(-1,store.load().get("orman").menuSlot());assertEquals("",store.load().get("orman").icon());
        Warp custom=original.displayed("AQID",2).renamed("Orman Alanı");store.save(Map.of("orman",custom));
        assertEquals(custom,store.load().get("orman"));assertTrue(Files.readString(path).contains("menu-yeri: 1-3"));
    }
    @Test void occupiedSlotCannotReplaceAnExistingValidFile() throws IOException {
        Path path=directory.resolve("warplar.yml");var store=new WarpStore(path);Warp first=point("orman",false).displayed("",2);
        store.save(Map.of("orman",first));String before=Files.readString(path);
        assertThrows(IllegalArgumentException.class,()->store.save(Map.of("orman",first,"pazar",point("pazar",false).displayed("",2))));
        assertEquals(before,Files.readString(path));
    }
    @Test void extraPointsAndAppearanceSurviveReloadAndRemovalOfFirstPoint() throws IOException {
        var store=new WarpStore(directory.resolve("warplar.yml"));Warp original=point("orman",true).displayed("AQID",2);
        Warp multiple=original.addedPoint(original.world(),new WarpPoint(111.5,70,-32.5,179,-17));
        store.save(Map.of("orman",multiple));assertEquals(multiple,store.load().get("orman"));
        Warp promoted=multiple.removedPoint(1);store.save(Map.of("orman",promoted));assertEquals(promoted,store.load().get("orman"));
        assertEquals(111.5,promoted.x());assertEquals(179,promoted.yaw());assertEquals(2,promoted.menuSlot());assertTrue(promoted.restricted());
    }
    @Test void malformedExtraPointListIsRejectedWithoutRewritingFile() throws IOException {
        Path path=directory.resolve("warplar.yml");var store=new WarpStore(path);store.save(Map.of("orman",point("orman",false)));
        String old=Files.readString(path);
        for(String invalid:List.of("    ek-noktalar: hello\n","    ek-noktalar: [hello]\n","    ek-noktalar:\n    - x: 1\n")){
            String broken=old+invalid;Files.writeString(path,broken);assertThrows(IOException.class,store::load);assertEquals(broken,Files.readString(path));
        }
    }
}
