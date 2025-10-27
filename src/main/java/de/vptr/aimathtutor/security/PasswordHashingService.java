package de.vptr.aimathtutor.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordHashingService {

    private static final int PBKDF2_ITERATIONS = 100000; // OWASP recommended minimum
    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits
    private static final int HASH_LENGTH = 8 * SALT_LENGTH; // 256 bits

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generates a random salt
     * 
     * @return Base64 encoded salt
     */
    public String generateSalt() {
        final var salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2 with SHA-256
     * 
     * @param password the plain text password
     * @param salt     the salt as a Base64 encoded string
     * @return the hashed password as a Base64 encoded string
     */
    public String hashPassword(final String password, final String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var saltBytes = Base64.getDecoder().decode(salt);

        final var spec = new PBEKeySpec(
                password.toCharArray(),
                saltBytes,
                PBKDF2_ITERATIONS,
                HASH_LENGTH);

        final var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        final var hash = factory.generateSecret(spec).getEncoded();

        return Base64.getEncoder().encodeToString(hash);
    }

    /**
     * Verifies a password against a stored hash
     * 
     * @param providedPassword the plain text password to verify
     * @param storedHash       the stored password hash
     * @param salt             the salt used for hashing
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(final String providedPassword, final String storedHash, final String salt) {
        if (providedPassword == null || storedHash == null || salt == null) {
            return false;
        }

        if (providedPassword.isEmpty() || storedHash.isEmpty() || salt.isEmpty()) {
            return false;
        }

        try {
            final var hashedProvidedPassword = this.hashPassword(providedPassword, salt);
            return hashedProvidedPassword.equals(storedHash);
        } catch (final Exception e) {
            return false;
        }
    }
}
