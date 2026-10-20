package tr.kasaba.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();
    @Test void acceptsCorrectPassword() throws Exception {
        String hash = hasher.hash("GüçlüŞifre123".toCharArray());
        assertTrue(hasher.verify("GüçlüŞifre123".toCharArray(), hash));
    }
    @Test void rejectsWrongPasswordAndCaseChanges() throws Exception {
        String hash = hasher.hash("Secret123".toCharArray());
        assertFalse(hasher.verify("secret123".toCharArray(), hash));
        assertFalse(hasher.verify("Wrong1234".toCharArray(), hash));
    }
    @Test void randomSaltProducesDistinctHashes() throws Exception {
        assertNotEquals(hasher.hash("password123".toCharArray()), hasher.hash("password123".toCharArray()));
    }
    @Test void rejectsCorruptAndUnboundedRecords() throws Exception {
        for (String value : new String[]{"", "plaintext", "pbkdf2-sha256$999999999$x$x", "pbkdf2-sha256$600000$!$!", "pbkdf2-sha256$600000$YQ==$YQ=="})
            assertFalse(hasher.verify("password123".toCharArray(), value));
    }
    @Test void doesNotAlterCallersPassword() throws Exception {
        char[] password = "password123".toCharArray();
        char[] original = password.clone();
        hasher.hash(password);
        assertArrayEquals(original, password);
    }
}
