package tr.kasaba.auth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class AccountStoreTest {
    @TempDir Path directory;
    @Test void absentAccountReturnsNull() throws Exception {
        assertNull(new AccountStore(directory).load("Steve"));
    }
    @Test void accountSurvivesRestartAndCaseChange() throws Exception {
        new AccountStore(directory).create("Steve", "encoded-hash");
        assertEquals("encoded-hash", new AccountStore(directory).load("sTEVE"));
    }
    @Test void cannotOverwriteExistingAccount() throws Exception {
        AccountStore store = new AccountStore(directory);
        store.create("Steve", "original");
        assertThrows(FileAlreadyExistsException.class, () -> store.create("STEVE", "replacement"));
        assertEquals("original", store.load("Steve"));
    }
    @Test void rejectsPathTraversalAndInvalidNames() throws Exception {
        AccountStore store = new AccountStore(directory);
        for (String name : new String[]{"../admin", "", "a/b", "abcdefghijklmnopq", "user.name"})
            assertThrows(IllegalArgumentException.class, () -> store.load(name));
    }
    @Test void corruptAccountFailsClosed() throws Exception {
        Files.writeString(directory.resolve("steve.properties"), "wrong=data\n");
        assertThrows(IOException.class, () -> new AccountStore(directory).load("Steve"));
    }
    @Test void successfulWriteLeavesNoTemporaryFiles() throws Exception {
        new AccountStore(directory).create("Steve", "encoded-hash");
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void realPasswordRoundTrip() throws Exception {
        PasswordHasher hasher = new PasswordHasher();
        AccountStore store = new AccountStore(directory);
        store.create("Alex", hasher.hash("TestŞifre123".toCharArray()));
        String stored = new AccountStore(directory).load("ALEX");
        assertTrue(hasher.verify("TestŞifre123".toCharArray(), stored));
        assertFalse(Files.readString(directory.resolve("alex.properties")).contains("TestŞifre123"));
    }
}
