package de.vptr.aimathtutor.service.security;

import static de.vptr.aimathtutor.service.security.AuthServiceTest.sessionFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.TestSessionCredentials;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

@QuarkusTest
class AuthServiceRequireUserTest {

    private static final String CREDENTIAL_STAMP_KEY = AppConstants.SESSION_KEY_CREDENTIAL_STAMP;

    @Inject
    private AuthService authService;

    @Inject
    private UserRepository userRepository;

    private void assertSessionEnded(final Executable call) {
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, assertThrows(ValidationException.class, call).getMessage());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireCurrentUser returns the active session user")
    @TestTransaction
    void testRequireCurrentUser_active() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            final var sess = sessionFor(admin);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(sess);

            assertSame(admin, this.authService.requireCurrentUser());
        }
    }

    @Test
    @DisplayName("requireCurrentUser refuses when there is no session")
    void testRequireCurrentUser_noSession() {
        this.assertSessionEnded(() -> this.authService.requireCurrentUser());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireCurrentUser refuses a stale credential stamp")
    @TestTransaction
    void testRequireCurrentUser_staleStamp() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            final var sess = sessionFor(admin);
            when(sess.getAttribute(CREDENTIAL_STAMP_KEY)).thenReturn("stale");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(sess);

            this.assertSessionEnded(() -> this.authService.requireCurrentUser());
        }
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireCurrentUser refuses a banned user")
    @TestTransaction
    void testRequireCurrentUser_banned() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            admin.banned = true;
            final var sess = sessionFor(admin);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(sess);

            this.assertSessionEnded(() -> this.authService.requireCurrentUser());
        }
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireCurrentUser refuses a deactivated user")
    @TestTransaction
    void testRequireCurrentUser_deactivated() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            admin.activated = false;
            final var sess = sessionFor(admin);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(sess);

            this.assertSessionEnded(() -> this.authService.requireCurrentUser());
        }
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireCurrentUser refuses an unknown public ID")
    void testRequireCurrentUser_unknownPublicId() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var sess = sessionFor(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(sess);

            this.assertSessionEnded(() -> this.authService.requireCurrentUser());
        }
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireSessionUser returns the user for valid credentials")
    @TestTransaction
    void testRequireSessionUser_valid() {
        final var admin = this.userRepository.findByUsername("admin");

        assertSame(admin, this.authService.requireSessionUser(TestSessionCredentials.of(admin)));
    }

    @Test
    @TestTransaction
    @DisplayName("requireSessionUser refuses null credentials")
    void testRequireSessionUser_null() {
        this.assertSessionEnded(() -> this.authService.requireSessionUser(null));
    }

    @Test
    @TestTransaction
    @DisplayName("requireSessionUser refuses an unknown public ID")
    void testRequireSessionUser_unknown() {
        final var credentials = new SessionCredentials("no-such-public-id", "stamp", UUID.randomUUID().toString());

        this.assertSessionEnded(() -> this.authService.requireSessionUser(credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireSessionUser refuses a stale credential stamp")
    @TestTransaction
    void testRequireSessionUser_staleStamp() {
        final var admin = this.userRepository.findByUsername("admin");
        final var credentials = new SessionCredentials(admin.publicId, "stale", UUID.randomUUID().toString());

        this.assertSessionEnded(() -> this.authService.requireSessionUser(credentials));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireSessionUser refuses a banned user")
    @TestTransaction
    void testRequireSessionUser_banned() {
        final var admin = this.userRepository.findByUsername("admin");
        admin.banned = true;

        this.assertSessionEnded(() -> this.authService.requireSessionUser(TestSessionCredentials.of(admin)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("requireSessionUser refuses a deactivated user")
    @TestTransaction
    void testRequireSessionUser_deactivated() {
        final var admin = this.userRepository.findByUsername("admin");
        admin.activated = false;

        this.assertSessionEnded(() -> this.authService.requireSessionUser(TestSessionCredentials.of(admin)));
    }
}
