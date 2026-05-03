package de.vptr.aimathtutor.security;

import io.quarkus.elytron.security.common.BcryptUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for secure password hashing and verification using bcrypt.
 * Delegates to the official Quarkus {@link BcryptUtil} for constant-time
 * hashing and verification.
 */
@ApplicationScoped
public class PasswordHashingService {

    /**
     * Hashes a plain-text password using bcrypt with a random salt.
     *
     * @param password the plain text password
     * @return the bcrypt hash (includes embedded salt and cost factor)
     * @throws IllegalArgumentException if password is null or empty
     */
    public String hashPassword(final String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        return BcryptUtil.bcryptHash(password);
    }

    /**
     * Verifies a plain-text password against a stored bcrypt hash.
     *
     * @param password   the plain text password to verify
     * @param storedHash the stored bcrypt password hash
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(final String password, final String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }

        if (password.isEmpty() || storedHash.isEmpty()) {
            return false;
        }

        try {
            return BcryptUtil.matches(password, storedHash);
        } catch (final RuntimeException e) {
            return false;
        }
    }
}
