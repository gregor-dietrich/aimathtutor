package de.vptr.aimathtutor.entity.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests for {@link EncryptedStringConverter}: verifies null passthrough and encrypt/decrypt round-trip via CDI.
 */
@QuarkusTest
public class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Test
    public void testConvertToDatabaseColumn_nullReturnsNull() {
        Assertions.assertNull(this.converter.convertToDatabaseColumn(null));
    }

    @Test
    public void testConvertToDatabaseColumn_returnsVersionedEnvelope() {
        final String result = this.converter.convertToDatabaseColumn("alice@example.com");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.startsWith("1|"), "Stored value must be a versioned AES-GCM envelope");
        Assertions.assertEquals(3, result.split("\\|", 3).length);
    }

    @Test
    public void testConvertToEntityAttribute_nullReturnsNull() {
        Assertions.assertNull(this.converter.convertToEntityAttribute(null));
    }

    @Test
    public void testConvertToEntityAttribute_decryptsToOriginalPlaintext() {
        final String plaintext = "bob@example.com";
        final String ciphertext = this.converter.convertToDatabaseColumn(plaintext);
        Assertions.assertNotNull(ciphertext);
        Assertions.assertEquals(plaintext, this.converter.convertToEntityAttribute(ciphertext));
    }

    @Test
    public void testRoundtrip_preservesSpecialCharacters() {
        final String plaintext = "user+tag@sub.example.co.uk";
        final String ciphertext = this.converter.convertToDatabaseColumn(plaintext);
        Assertions.assertNotNull(ciphertext);
        Assertions.assertEquals(plaintext, this.converter.convertToEntityAttribute(ciphertext));
    }

    @Test
    public void testConvertToDatabaseColumn_uniqueCiphertextPerCall() {
        final String c1 = this.converter.convertToDatabaseColumn("same@example.com");
        final String c2 = this.converter.convertToDatabaseColumn("same@example.com");
        Assertions.assertNotEquals(c1, c2, "Each call must produce a unique ciphertext due to random IV");
    }
}
