package tr.kasaba.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuthSpawnStoreTest {
    @TempDir Path directory;
    @Test void absentPointLeavesExistingServersUnchanged() throws IOException {
        assertNull(new AuthSpawnStore(directory.resolve("kayit-yeri.yml")).load());
    }
    @Test void fullPositionAndFacingSurviveRestart() throws IOException {
        Path path = directory.resolve("kayit-yeri.yml");
        var point = new AuthSpawnStore.Point(UUID.randomUUID(), "kayit-dunyasi", 123.75, 65.25, -456.5, 135.25f, -12.5f);
        new AuthSpawnStore(path).save(point);
        assertEquals(point, new AuthSpawnStore(path).load());
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void clearingPointStaysDisabledAfterRestart() throws IOException {
        Path path = directory.resolve("kayit-yeri.yml");
        var store = new AuthSpawnStore(path);
        store.save(new AuthSpawnStore.Point(UUID.randomUUID(), "world", 0, 64, 0, 0, 0));
        store.save(null);
        assertNull(new AuthSpawnStore(path).load());
    }
    @Test void corruptOrPartialPointCannotSilentlyFallBack() throws IOException {
        Path path = directory.resolve("kayit-yeri.yml");
        var store = new AuthSpawnStore(path);
        for (String broken : new String[]{"enabled: [", "enabled: yesplease", "enabled: true\nworld-uuid: missing\n"}) {
            Files.writeString(path, broken);
            assertThrows(IOException.class, store::load);
            assertEquals(broken, Files.readString(path));
        }
    }
    @Test void invalidCoordinatesAreRejected() {
        UUID world = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new AuthSpawnStore.Point(world,"world",Double.NaN,64,0,0,0));
        assertThrows(IllegalArgumentException.class, () -> new AuthSpawnStore.Point(world,"world",0,64,0,Float.POSITIVE_INFINITY,0));
        assertThrows(IllegalArgumentException.class, () -> new AuthSpawnStore.Point(world,"world",0,64,0,0,100));
    }
}
