package de.vptr.aimathtutor.service.security;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.ObserverMethod;

/**
 * Unit tests for the private static methods of {@link EncryptionKeyManager} using reflection and temp directories.
 */
@SuppressWarnings({ "NullAway", "PMD.AvoidAccessibilityAlteration" })
public class EncryptionKeyManagerTest {

    private static boolean notCalled() {
        throw new AssertionError("The database must not be queried");
    }

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
            System.setProperty("user.home", this.tempDir.toString());
            km.init();
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    private static EncryptionKeyManager managerWithHomeOnly() {
        return new EncryptionKeyManager(LaunchMode.TEST, Optional.empty(), EncryptionKeyManagerTest::notCalled);
    }

    private EncryptionKeyManager productionManager(final BooleanSupplier encryptedDataExists) {
        return new EncryptionKeyManager(LaunchMode.NORMAL,
                Optional.of(this.tempDir.resolve("keys/encryption.key").toString()), encryptedDataExists);
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
        final EncryptionKeyManager km = managerWithHomeOnly();
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

        final EncryptionKeyManager km = managerWithHomeOnly();
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

        final EncryptionKeyManager km = managerWithHomeOnly();
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

        final EncryptionKeyManager km = managerWithHomeOnly();
        this.initWithTempHome(km);

        final byte[] k1 = km.getMasterKey();
        final byte[] k2 = km.getMasterKey();
        Assertions.assertNotSame(k1, k2, "getMasterKey must return a new array each time");
        Assertions.assertArrayEquals(k1, k2);
    }

    @Test
    public void testInit_productionMissingKeyWithEncryptedData_refuses() {
        final Path keyPath = this.tempDir.resolve("keys/encryption.key");
        final EncryptionKeyManager km = this.productionManager(() -> true);

        final var ex = Assertions.assertThrows(IllegalStateException.class, km::init);

        Assertions.assertTrue(ex.getMessage().contains(keyPath.toString()), ex.getMessage());
        Assertions.assertTrue(ex.getMessage().contains("restore"), ex.getMessage());
        Assertions.assertFalse(Files.exists(keyPath), "No key file may be created");
        Assertions.assertFalse(Files.exists(keyPath.getParent()), "No directory may be created");
    }

    @Test
    public void testInit_productionMissingKeyOnEmptyDatabase_generatesKey() throws IOException {
        final EncryptionKeyManager km = this.productionManager(() -> false);

        km.init();

        Assertions.assertEquals(32, km.masterKey.length);
        Assertions.assertEquals(32,
                Base64.getDecoder().decode(Files.readAllBytes(this.tempDir.resolve("keys/encryption.key"))).length);
    }

    @Test
    public void testInit_productionExistingKey_doesNotQuery() throws IOException {
        final Path keyPath = this.tempDir.resolve("keys/encryption.key");
        Files.createDirectories(keyPath.getParent());
        Files.write(keyPath, Base64.getEncoder().encode(new byte[32]));

        final EncryptionKeyManager km = this.productionManager(EncryptionKeyManagerTest::notCalled);
        km.init();

        Assertions.assertEquals(32, km.masterKey.length);
    }

    @Test
    public void testInit_devMissingKey_generatesWithoutQuery() {
        final Path keyPath = this.tempDir.resolve("keys/encryption.key");
        final EncryptionKeyManager km = new EncryptionKeyManager(LaunchMode.DEVELOPMENT,
                Optional.of(keyPath.toString()), EncryptionKeyManagerTest::notCalled);

        km.init();

        Assertions.assertTrue(Files.exists(keyPath));
    }

    @Test
    public void testLoadAtStartup_observesStartupEventBeforeDefaultPriority() throws Exception {
        final Method method = EncryptionKeyManager.class.getDeclaredMethod("loadAtStartup", StartupEvent.class);

        Assertions.assertTrue(method.getParameters()[0].isAnnotationPresent(Observes.class));
        final int priority = method.getParameters()[0].getAnnotation(Priority.class).value();
        Assertions.assertTrue(priority < ObserverMethod.DEFAULT_PRIORITY, "priority " + priority);
    }
}
