package de.vptr.aimathtutor.util;

import java.util.Objects;
import java.util.UUID;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.service.security.AuthService;

/**
 * Builds the session credentials a signed-in session of a user would hold, for tests that pass them to a service.
 */
public final class TestSessionCredentials {

    private TestSessionCredentials() {
    }

    /**
     * Builds credentials whose stamp matches the user's current password hash, with a fresh throttle key.
     *
     * @param user
     *            the user, with its public ID and password hash set
     * @return the credentials
     */
    public static SessionCredentials of(final UserEntity user) {
        return of(user, UUID.randomUUID().toString());
    }

    /**
     * Builds credentials whose stamp matches the user's current password hash, keeping a session's throttle key.
     *
     * @param user
     *            the user, with its public ID and password hash set
     * @param throttleKey
     *            the session's throttle key
     * @return the credentials
     */
    public static SessionCredentials of(final UserEntity user, final String throttleKey) {
        return new SessionCredentials(Objects.requireNonNull(user.publicId, "User public ID"),
                Objects.requireNonNull(AuthService.credentialStamp(user.password), "User password hash"), throttleKey);
    }
}
