package tr.kasaba.auth;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Locale;
import java.util.Properties;

final class AccountStore {
    private final Path directory;
    AccountStore(Path directory) throws IOException {
        this.directory = directory;
        Files.createDirectories(directory);
    }
    private Path file(String name) {
        if (!name.matches("[a-zA-Z0-9_]{1,16}")) throw new IllegalArgumentException("Geçersiz oyuncu adı");
        return directory.resolve(name.toLowerCase(Locale.ROOT) + ".properties");
    }
    synchronized String load(String name) throws IOException {
        Path path = file(name);
        if (!Files.exists(path)) return null;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) { p.load(r); }
        String hash = p.getProperty("hash");
        if (hash == null || hash.isBlank()) throw new IOException("Hesap kaydı geçersiz");
        return hash;
    }
    synchronized void create(String name, String hash) throws IOException {
        Path destination = file(name);
        if (Files.exists(destination)) throw new FileAlreadyExistsException(destination.toString());
        Path temp = Files.createTempFile(directory, ".account-", ".tmp");
        try {
            try { Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------")); }
            catch (UnsupportedOperationException ignored) { /* Windows ACL'leri sistem tarafından yönetilir. */ }
            Properties p = new Properties();
            p.setProperty("hash", hash);
            try (Writer w = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { p.store(w, "KasabaAuth password hash"); }
            // Aynı dizinde taşı; hiçbir mevcut hesabı üzerine yazma.
            Files.move(temp, destination);
        } finally { Files.deleteIfExists(temp); }
    }
}
