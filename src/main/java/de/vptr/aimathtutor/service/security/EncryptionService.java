package de.vptr.aimathtutor.service.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jboss.logging.Logger;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Provides AES-256-GCM authenticated encryption and HMAC-SHA256 blind-index generation for PII fields. Both the
 * encryption key and blind-index key are derived from the master key managed by {@link EncryptionKeyManager}.
 *
 * <p>
 * Ciphertext envelope format: {@code version|base64(iv)|base64(ciphertext+tag)}
 */
@ApplicationScoped
public class EncryptionService {

    private static final Logger LOG = Logger.getLogger(EncryptionService.class);
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ENVELOPE_VERSION = "1";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    EncryptionKeyManager keyManager;

    private SecretKey encryptionKey;
    private byte[] blindIndexKeyBytes;

    @PostConstruct
    @SuppressWarnings("PMD.HardCodedCryptoKey") // false positive: labels are HKDF domain separators, not keys
    void init() {
        final byte[] master = this.keyManager.getMasterKey();
        this.encryptionKey = new SecretKeySpec(deriveKey(master, "encrypt"), "AES");
        this.blindIndexKeyBytes = deriveKey(master, "blind-index");
        LOG.info("EncryptionService initialized");
    }

    /**
     * Encrypts {@code plaintext} using AES-256-GCM with a fresh random IV. Returns a versioned envelope string, or
     * {@code null} if {@code plaintext} is {@code null}.
     */
    @Nullable
    public String encrypt(@Nullable final String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            final byte[] iv = new byte[GCM_IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            final Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, this.encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            final byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            final Base64.Encoder enc = Base64.getEncoder();
            return ENVELOPE_VERSION + "|" + enc.encodeToString(iv) + "|" + enc.encodeToString(ciphertext);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a versioned ciphertext envelope produced by {@link #encrypt}. Returns {@code null} if {@code envelope}
     * is {@code null}.
     */
    @Nullable
    public String decrypt(@Nullable final String envelope) {
        if (envelope == null) {
            return null;
        }
        try {
            final String[] parts = envelope.split("\\|", 3);
            if (parts.length != 3 || !ENVELOPE_VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Unrecognized ciphertext envelope version: " + parts[0]);
            }
            final Base64.Decoder dec = Base64.getDecoder();
            final byte[] iv = dec.decode(parts[1]);
            final byte[] ciphertext = dec.decode(parts[2]);
            final Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, this.encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /**
     * Generates a deterministic HMAC-SHA256 blind index for the given plaintext. Input is lowercased before hashing to
     * support case-insensitive equality lookups (e.g., email).
     */
    @Nullable
    public String generateBlindIndex(@Nullable final String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(this.blindIndexKeyBytes, HMAC_SHA256));
            final byte[] hmac = mac.doFinal(plaintext.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Blind index generation failed", e);
        }
    }

    private static byte[] deriveKey(final byte[] master, final String label) {
        try {
            final Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(master, HMAC_SHA256));
            return mac.doFinal(label.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Key derivation failed for label: " + label, e);
        }
    }
}
