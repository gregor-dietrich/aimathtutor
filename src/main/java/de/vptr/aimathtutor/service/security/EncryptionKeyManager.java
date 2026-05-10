package de.vptr.aimathtutor.service.security;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Loads or generates the master encryption key used for AES-256-GCM field encryption. The key is persisted to a file so
 * it survives application restarts.
 *
 * <p>
 * {@link #resolveKeyPath()} determines the key file location using this order:
 * <ol>
 * <li>Env var {@code AIMATHTUTOR_ENCRYPTION_KEY_FILE} (if set and non-empty)</li>
 * <li>Existing key at {@code $XDG_DATA_HOME/aimathtutor/encryption.key} (or
 * {@code ~/.local/share/aimathtutor/encryption.key})</li>
 * <li>Existing key at {@code ~/.aimathtutor/encryption.key}</li>
 * <li>Auto-generates a new 256-bit key at the XDG location with 0600 permissions</li>
 * </ol>
 */
@ApplicationScoped
public class EncryptionKeyManager {

    private static final Logger LOG = Logger.getLogger(EncryptionKeyManager.class);
    private static final int KEY_BYTES = 32;
    private static final String KEY_FILENAME = "encryption.key";
    private static final String ENV_KEY_FILE = "AIMATHTUTOR_ENCRYPTION_KEY_FILE";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    byte[] masterKey;

    @PostConstruct
    void init() {
        final Path keyPath = resolveKeyPath();
        LOG.infof("Encryption key file: %s", keyPath);
        this.masterKey = Files.exists(keyPath) ? loadKey(keyPath) : generateAndPersistKey(keyPath);
    }

    byte[] getMasterKey() {
        return this.masterKey.clone(); // defensive copy — callers must not mutate
    }

    private static Path resolveKeyPath() {
        final String envKeyFile = System.getenv(ENV_KEY_FILE);
        if (envKeyFile != null && !envKeyFile.isBlank()) {
            return Paths.get(envKeyFile);
        }
        final Path xdgKey = xdgKeyPath();
        if (Files.exists(xdgKey)) {
            return xdgKey;
        }
        final Path homeKey = Paths.get(System.getProperty("user.home"), ".aimathtutor", KEY_FILENAME);
        if (Files.exists(homeKey)) {
            return homeKey;
        }
        return xdgKey;
    }

    private static Path xdgKeyPath() {
        final String xdgDataHome = System.getenv("XDG_DATA_HOME");
        final Path base = (xdgDataHome != null && !xdgDataHome.isBlank()) ? Paths.get(xdgDataHome)
                : Paths.get(System.getProperty("user.home"), ".local", "share");
        return base.resolve("aimathtutor").resolve(KEY_FILENAME);
    }

    private static byte[] loadKey(final Path path) {
        try {
            final byte[] decoded = Base64.getDecoder().decode(Files.readAllBytes(path));
            if (decoded.length != KEY_BYTES) {
                throw new IllegalStateException(
                        "Encryption key file has wrong length: " + decoded.length + " bytes, expected " + KEY_BYTES);
            }
            LOG.info("Encryption key loaded");
            return decoded;
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read encryption key from: " + path, e);
        }
    }

    private static byte[] generateAndPersistKey(final Path path) {
        try {
            final byte[] key = new byte[KEY_BYTES];
            SECURE_RANDOM.nextBytes(key);

            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Write to a temp file in the same directory, set 0600 permissions before writing,
            // then atomically move to the target path to avoid a window of world-readable exposure.
            final Path tmp = parent != null ? Files.createTempFile(parent, ".key-", ".tmp")
                    : Files.createTempFile(".key-", ".tmp");
            try {
                try {
                    Files.setPosixFilePermissions(tmp,
                            Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                } catch (final UnsupportedOperationException e) {
                    LOG.warnf("Cannot set POSIX permissions on %s (non-POSIX filesystem)", path);
                }
                Files.write(tmp, Base64.getEncoder().encode(key));
                try {
                    Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE);
                } catch (final FileAlreadyExistsException e) {
                    LOG.info("Encryption key file created concurrently; loading existing key");
                    return loadKey(path);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }

            LOG.infof("Generated new encryption key at: %s", path);
            return key;
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to generate and save encryption key at: " + path, e);
        }
    }
}
