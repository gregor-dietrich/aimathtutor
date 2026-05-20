package de.vptr.aimathtutor.service.security;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Loads or generates the master encryption key used for AES-256-GCM field encryption. The key is persisted to a file so
 * it survives application restarts.
 *
 * <p>
 * {@link #resolveKeyPath()} determines the key file location using this order:
 * <ol>
 * <li>Property {@code app.security.encryption-key-file} (if set and non-empty)</li>
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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    @ConfigProperty(name = "app.security.encryption-key-file")
    Optional<String> keyFile;

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

    private Path resolveKeyPath() {
        if (this.keyFile.isPresent() && !this.keyFile.get().isBlank()) {
            return Paths.get(this.keyFile.get());
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
            if (parent == null) {
                // Refuse to write to an unspecified directory: createTempFile would land
                // the unencrypted master key in the system temp dir before atomic move,
                // which on multi-user hosts may be readable by others.
                throw new IllegalStateException("Encryption key path must have an explicit parent directory: " + path);
            }
            Files.createDirectories(parent);

            // Write to a temp file in the same directory, restrict permissions before writing,
            // then atomically move to the target path to avoid a window of world-readable exposure.
            final Path tmp = Files.createTempFile(parent, ".key-", ".tmp");
            try {
                restrictToOwner(tmp);
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

    /**
     * Restricts a freshly-created file so only the current OS user can read or write it. Tries POSIX 0600 first; on
     * filesystems that don't support POSIX (Windows NTFS) it falls back to an ACL granting only the file owner. If
     * neither mechanism is supported, refuses to write the file — leaving an unprotected AES-256 master key on disk
     * silently is worse than a startup failure that operators can fix.
     */
    private static void restrictToOwner(final Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            return;
        } catch (final UnsupportedOperationException posixUnsupported) {
            LOG.debugf("POSIX permissions unsupported for %s; trying ACL fallback", file);
        }
        final AclFileAttributeView aclView = Files.getFileAttributeView(file, AclFileAttributeView.class);
        if (aclView == null) {
            throw new IllegalStateException("Cannot restrict permissions on encryption key file " + file
                    + ". Neither POSIX nor ACL attribute views are supported by this filesystem. "
                    + "Move the key to a protected location and set app.security.encryption-key-file.");
        }
        final UserPrincipal owner = Files.getOwner(file);
        final AclEntry entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
                .setPermissions(EnumSet.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.APPEND_DATA, AclEntryPermission.READ_ATTRIBUTES,
                        AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.READ_ACL,
                        AclEntryPermission.SYNCHRONIZE, AclEntryPermission.DELETE))
                .build();
        aclView.setAcl(List.of(entry));
        LOG.infof("Applied owner-only ACL to encryption key file %s (POSIX unavailable)", file);
    }
}
