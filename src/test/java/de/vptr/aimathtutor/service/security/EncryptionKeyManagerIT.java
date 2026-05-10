package de.vptr.aimathtutor.service.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Tests for {@link EncryptionKeyManager}: verifies key availability and defensive copying.
 */
@QuarkusTest
public class EncryptionKeyManagerIT {

    @Inject
    EncryptionKeyManager encryptionKeyManager;

    @Test
    public void testGetMasterKey_returns32Bytes() {
        final byte[] key = this.encryptionKeyManager.getMasterKey();
        Assertions.assertNotNull(key);
        Assertions.assertEquals(32, key.length, "Master key must be 32 bytes (AES-256)");
    }

    @Test
    public void testGetMasterKey_returnsDefensiveCopy() {
        final byte[] key1 = this.encryptionKeyManager.getMasterKey();
        final byte[] key2 = this.encryptionKeyManager.getMasterKey();
        Assertions.assertNotSame(key1, key2, "getMasterKey must return a new array on each call");
    }

    @Test
    public void testGetMasterKey_mutatingReturnedKeyDoesNotAffectInternal() {
        final byte[] key1 = this.encryptionKeyManager.getMasterKey();
        final byte original = key1[0];
        key1[0] = (byte) ~key1[0];
        final byte[] key2 = this.encryptionKeyManager.getMasterKey();
        Assertions.assertEquals(original, key2[0],
                "Mutating the returned array must not corrupt the internal key");
    }

    @Test
    public void testGetMasterKey_consistentContent() {
        final byte[] key1 = this.encryptionKeyManager.getMasterKey();
        final byte[] key2 = this.encryptionKeyManager.getMasterKey();
        Assertions.assertArrayEquals(key1, key2, "getMasterKey must return the same content each time");
    }
}
