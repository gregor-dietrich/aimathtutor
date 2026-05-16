package de.vptr.aimathtutor.service.security;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for the private static methods of {@link EncryptionKeyManager} using reflection and temp directories.
 */
@SuppressWarnings({ "NullAway", "PMD.AvoidAccessibilityAlteration" })
public class EncryptionKeyManagerTest {

    @TempDir
    Path tempDir;

    private static Method getPrivateMethod(final String name, final Class<?>... params) throws NoSuchMethodException {
        final Method m = EncryptionKeyManager.class.getDeclaredMethod(name, params);
        m.setAccessible(true);
        return m;
    }

    private void initWithTempHome(final EncryptionKeyManager km) {
        final String originalHome = System.getProperty("user.home");
        try {
            // Manually initialize injected fields for unit test (no CDI)
            try {
                final Field keyFileField = EncryptionKeyManager.class.getDeclaredField("keyFile");
                keyFileField.setAccessible(true);
                keyFileField.set(km, Optional.empty());
            } catch (final NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException("Failed to set keyFile field via reflection", e);
            }

            System.setProperty("user.home", this.tempDir.toString());
            km.init();
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void testGenerateAndPersistKey_createsKeyFileWith32Bytes() throws Exception {
        final Path keyPath = this.tempDir.resolve("test.key");
        final Method method = getPrivateMethod("generateAndPersistKey", Path.class);

        final byte[] key = (byte[]) method.invoke(null, keyPath);

        Assertions.assertNotNull(key);
        Assertions.assertEquals(32, key.length, "Generated key must be 32 bytes");
        Assertions.assertTrue(Files.exists(keyPath), "Key file must be created");

        final byte[] storedKey = Base64.getDecoder().decode(Files.readAllBytes(keyPath));
        Assertions.assertArrayEquals(key, storedKey, "Stored key must match returned key");
    }

    @Test
    public void testLoadKey_loadsValidKey() throws Exception {
        final byte[] originalKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            originalKey[i] = (byte) i;
        }
        final Path keyPath = this.tempDir.resolve("valid.key");
        Files.write(keyPath, Base64.getEncoder().encode(originalKey));

        final Method method = getPrivateMethod("loadKey", Path.class);
        final byte[] loaded = (byte[]) method.invoke(null, keyPath);

        Assertions.assertArrayEquals(originalKey, loaded);
    }

    @Test
    public void testLoadKey_wrongKeyLength_throwsIllegalState() throws Exception {
        final byte[] shortKey = new byte[16];
        final Path keyPath = this.tempDir.resolve("short.key");
        Files.write(keyPath, Base64.getEncoder().encode(shortKey));

        final Method method = getPrivateMethod("loadKey", Path.class);
        final var ex = Assertions.assertThrows(InvocationTargetException.class, () -> method.invoke(null, keyPath));
        Assertions.assertInstanceOf(IllegalStateException.class, ex.getCause());
        Assertions.assertTrue(ex.getCause().getMessage().contains("wrong length"));
    }

    @Test
    public void testLoadKey_nonExistentFile_throwsIllegalState() throws Exception {
        final Path missingPath = this.tempDir.resolve("missing.key");

        final Method method = getPrivateMethod("loadKey", Path.class);
        final var ex = Assertions.assertThrows(InvocationTargetException.class, () -> method.invoke(null, missingPath));
        Assertions.assertInstanceOf(IllegalStateException.class, ex.getCause());
        Assertions.assertTrue(ex.getCause().getMessage().contains("Failed to read encryption key"));
    }

    @Test
    public void testInitWithNewKeyFile_generatesKey() {
        final EncryptionKeyManager km = new EncryptionKeyManager();
        this.initWithTempHome(km);

        Assertions.assertNotNull(km.masterKey);
        Assertions.assertEquals(32, km.masterKey.length);
    }

    @Test
    public void testInitWithExistingKeyFile_loadsKey() throws IOException {
        final byte[] expectedKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            expectedKey[i] = (byte) (i + 10);
        }

        final Path keyDir = this.tempDir.resolve(".local/share/aimathtutor");
        Files.createDirectories(keyDir);
        Files.write(keyDir.resolve("encryption.key"), Base64.getEncoder().encode(expectedKey));

        final EncryptionKeyManager km = new EncryptionKeyManager();
        this.initWithTempHome(km);

        Assertions.assertArrayEquals(expectedKey, km.masterKey);
    }

    @Test
    public void testInitWithHomeDirFallback_loadsKey() throws IOException {
        final byte[] expectedKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            expectedKey[i] = (byte) (i + 20);
        }

        final Path homeKeyDir = this.tempDir.resolve(".aimathtutor");
        Files.createDirectories(homeKeyDir);
        Files.write(homeKeyDir.resolve("encryption.key"), Base64.getEncoder().encode(expectedKey));

        final EncryptionKeyManager km = new EncryptionKeyManager();
        this.initWithTempHome(km);

        Assertions.assertArrayEquals(expectedKey, km.masterKey,
                "init() must load the key from ~/.aimathtutor/encryption.key when neither env var nor XDG path is set");
    }

    @Test
    public void testGenerateAndPersistKey_createsParentDirectories() throws Exception {
        final Path nestedKeyPath = this.tempDir.resolve("deep/nested/dir/test.key");
        final Method method = getPrivateMethod("generateAndPersistKey", Path.class);

        method.invoke(null, nestedKeyPath);

        Assertions.assertTrue(Files.exists(nestedKeyPath), "Key file must be created in nested directories");
    }

    @Test
    public void testGetMasterKey_afterInit_returnsClone() throws IOException {
        final byte[] expectedKey = new byte[32];
        for (int i = 0; i < 32; i++) {
            expectedKey[i] = (byte) (i + 5);
        }

        final Path keyDir = this.tempDir.resolve(".local/share/aimathtutor");
        Files.createDirectories(keyDir);
        Files.write(keyDir.resolve("encryption.key"), Base64.getEncoder().encode(expectedKey));

        final EncryptionKeyManager km = new EncryptionKeyManager();
        this.initWithTempHome(km);

        final byte[] k1 = km.getMasterKey();
        final byte[] k2 = km.getMasterKey();
        Assertions.assertNotSame(k1, k2, "getMasterKey must return a new array each time");
        Assertions.assertArrayEquals(k1, k2);
    }
}
