package de.vptr.aimathtutor.service.security;

import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/**
 * Tests for {@link EncryptionService}: encrypt/decrypt roundtrip, IV uniqueness, and blind index behaviour.
 */
@QuarkusTest
public class EncryptionServiceTest {

    @Inject
    EncryptionService encryptionService;

    @Test
    public void testEncryptDecryptRoundtrip() {
        final String plaintext = "alice@example.com";
        final String ciphertext = this.encryptionService.encrypt(plaintext);
        Assertions.assertNotNull(ciphertext);
        Assertions.assertEquals(plaintext, this.encryptionService.decrypt(ciphertext));
    }

    @Test
    public void testEncryptProducesVersionedEnvelope() {
        final String ciphertext = this.encryptionService.encrypt("test@example.com");
        Assertions.assertNotNull(ciphertext);
        Assertions.assertTrue(ciphertext.startsWith("1|"), "Envelope must start with version prefix '1|'");
        Assertions.assertEquals(3, ciphertext.split("\\|", 3).length, "Envelope must have three pipe-delimited parts");
    }

    @Test
    public void testEncryptProducesUniqueIvPerCall() {
        final String plaintext = "same@example.com";
        final String c1 = this.encryptionService.encrypt(plaintext);
        final String c2 = this.encryptionService.encrypt(plaintext);
        Assertions.assertNotNull(c1);
        Assertions.assertNotNull(c2);
        Assertions.assertNotEquals(c1, c2, "Each encryption must produce a unique ciphertext due to random IV");
    }

    @Test
    public void testEncryptNullReturnsNull() {
        Assertions.assertNull(this.encryptionService.encrypt(null));
    }

    @Test
    public void testDecryptNullReturnsNull() {
        Assertions.assertNull(this.encryptionService.decrypt(null));
    }

    @Test
    public void testBlindIndexDeterministic() {
        final String email = "bob@example.com";
        final String idx1 = this.encryptionService.generateBlindIndex(email);
        final String idx2 = this.encryptionService.generateBlindIndex(email);
        Assertions.assertNotNull(idx1);
        Assertions.assertEquals(idx1, idx2, "Blind index must be deterministic for the same input");
    }

    @Test
    public void testBlindIndexCaseInsensitive() {
        final String idx1 = this.encryptionService.generateBlindIndex("carol@example.com");
        final String idx2 = this.encryptionService.generateBlindIndex("CAROL@EXAMPLE.COM");
        Assertions.assertNotNull(idx1);
        Assertions.assertEquals(idx1, idx2, "Blind index must be case-insensitive");
    }

    @Test
    public void testBlindIndexNullReturnsNull() {
        Assertions.assertNull(this.encryptionService.generateBlindIndex(null));
    }

    @Test
    public void testBlindIndexDiffersForDifferentInputs() {
        final String idx1 = this.encryptionService.generateBlindIndex("alice@example.com");
        final String idx2 = this.encryptionService.generateBlindIndex("bob@example.com");
        Assertions.assertNotNull(idx1);
        Assertions.assertNotEquals(idx1, idx2);
    }

    @Test
    public void testDecrypt_invalidEnvelopeFormat_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.encryptionService.decrypt("not-a-valid-envelope"));
    }

    @Test
    public void testDecrypt_wrongVersion_throws() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.encryptionService.decrypt("2|aGVsbG8=|d29ybGQ="));
    }

    @Test
    public void testEncryptDecryptRoundtrip_emptyString() {
        final String plaintext = "";
        final String ciphertext = this.encryptionService.encrypt(plaintext);
        Assertions.assertNotNull(ciphertext);
        Assertions.assertEquals(plaintext, this.encryptionService.decrypt(ciphertext));
    }

    @Test
    public void testBlindIndex_emptyString_returnsNonNull() {
        final String idx = this.encryptionService.generateBlindIndex("");
        Assertions.assertNotNull(idx);
        Assertions.assertFalse(idx.isBlank());
    }

    @Test
    public void testDecrypt_tamperedTag_throws() {
        final String envelope = this.encryptionService.encrypt("sensitive@example.com");
        Assertions.assertNotNull(envelope);

        final String[] parts = envelope.split("\\|", 3);
        Assertions.assertEquals(3, parts.length, "Envelope must have three parts before tampering");
        final byte[] cipherBytes = Base64.getDecoder().decode(parts[2]);
        cipherBytes[cipherBytes.length - 1] ^= (byte) 0xFF;
        final String tampered = parts[0] + "|" + parts[1] + "|" + Base64.getEncoder().encodeToString(cipherBytes);

        Assertions.assertThrows(IllegalStateException.class, () -> this.encryptionService.decrypt(tampered),
                "AES-GCM must reject tampered ciphertext/auth tag");
    }
}
