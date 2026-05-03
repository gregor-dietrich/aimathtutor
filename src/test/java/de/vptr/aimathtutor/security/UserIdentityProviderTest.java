package de.vptr.aimathtutor.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.LoginAttemptService;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class UserIdentityProviderTest {

    @Inject
    UserIdentityProvider identityProvider;

    @Inject
    LoginAttemptService loginAttemptService;

    @Inject
    UserRepository userRepository;

    @BeforeEach
    void setUp() {
        this.loginAttemptService.recordSuccessfulLogin("admin");
        this.loginAttemptService.recordSuccessfulLogin("student1");
        this.loginAttemptService.recordSuccessfulLogin("student2");
        this.loginAttemptService.recordSuccessfulLogin("nonexistent");
    }

    @Test
    @DisplayName("Should reject null username")
    @TestTransaction
    void shouldRejectNullUsername() {
        assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser(null, "password"));
    }

    @Test
    @DisplayName("Should reject empty username")
    @TestTransaction
    void shouldRejectEmptyUsername() {
        assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("", "password"));
    }

    @Test
    @DisplayName("Should reject null password")
    @TestTransaction
    void shouldRejectNullPassword() {
        assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("admin", null));
    }

    @Test
    @DisplayName("Should reject empty password")
    @TestTransaction
    void shouldRejectEmptyPassword() {
        assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("admin", ""));
    }

    @Test
    @DisplayName("Should reject locked out user")
    @TestTransaction
    void shouldRejectLockedOutUser() {
        for (int i = 0; i < 5; i++) {
            this.loginAttemptService.recordFailedAttempt("student1");
        }
        assertTrue(this.loginAttemptService.isLockedOut("student1"));

        final var ex = assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("student1", "student1"));
        assertTrue(ex.getMessage().contains("Too many failed attempts"),
                "Expected lockout message but got: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject non-existent user")
    @TestTransaction
    void shouldRejectNonExistentUser() {
        final var ex = assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("nonexistent", "password"));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject wrong password")
    @TestTransaction
    void shouldRejectWrongPassword() {
        final var ex = assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("admin", "wrongpassword"));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject banned user")
    @TestTransaction
    void shouldRejectBannedUser() {
        final UserEntity user = this.userRepository.findByUsername("student1");
        user.banned = true;

        final var ex = assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("student1", "student1"));
        assertEquals("User is banned", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject inactive user")
    @TestTransaction
    void shouldRejectInactiveUser() {
        final UserEntity user = this.userRepository.findByUsername("student2");
        user.activated = false;

        final var ex = assertThrows(AuthenticationFailedException.class,
                () -> this.identityProvider.authenticateUser("student2", "student2"));
        assertEquals("User is not activated", ex.getMessage());
    }

    @Test
    @DisplayName("Should return security identity for valid credentials")
    @TestTransaction
    void shouldReturnSecurityIdentityForValidCredentials() {
        final var identity = this.identityProvider.authenticateUser("admin", "admin");

        assertNotNull(identity);
        assertNotNull(identity.getPrincipal());
        assertEquals("admin", identity.getPrincipal().getName());
    }
}
