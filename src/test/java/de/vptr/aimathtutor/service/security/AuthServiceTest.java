package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class AuthServiceTest {

    @Inject
    private AuthService authService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private LoginAttemptService loginAttemptService;

    private static final String USERNAME_KEY = AppConstants.SESSION_KEY_USERNAME;
    private static final String AUTHENTICATED_KEY = "authenticated.status";
    private static final String LAST_DB_CHECK_KEY = "authenticated.lastDbCheck";

    @Test
    @DisplayName("Should return invalid input when username is null")
    void shouldReturnInvalidInputWhenUsernameIsNull() {
        final var result = this.authService.authenticate(null, "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when username is empty")
    void shouldReturnInvalidInputWhenUsernameIsEmpty() {
        final var result = this.authService.authenticate("", "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when username is whitespace")
    void shouldReturnInvalidInputWhenUsernameIsWhitespace() {
        final var result = this.authService.authenticate("   ", "password");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is null")
    void shouldReturnInvalidInputWhenPasswordIsNull() {
        final var result = this.authService.authenticate("username", null);
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is empty")
    void shouldReturnInvalidInputWhenPasswordIsEmpty() {
        final var result = this.authService.authenticate("username", "");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should return invalid input when password is whitespace")
    void shouldReturnInvalidInputWhenPasswordIsWhitespace() {
        final var result = this.authService.authenticate("username", "   ");
        assertFalse(result.isSuccess());
        assertEquals("Username and password are required", result.getMessage());
    }

    @Test
    @DisplayName("Should authenticate valid seeded user")
    @TestTransaction
    void shouldAuthenticateValidSeededUser() {
        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class);
                MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class);
                MockedStatic<VaadinService> mockedService = mockStatic(VaadinService.class)) {

            final VaadinRequest mockReq = mock(VaadinRequest.class);
            final String loopback = AppConstants.BLOCKED_HOST_LOOPBACK_IPV4;
            when(mockReq.getRemoteAddr()).thenReturn(loopback);
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(mockReq);

            final VaadinSession mockSess = mock(VaadinSession.class);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            // Stub reinitializeSession to do nothing and avoid internal Vaadin logic
            mockedService.when(() -> VaadinService.reinitializeSession(any())).thenAnswer(i -> null);

            final AuthResultDto result = this.authService.authenticate("admin", "admin");
            assertTrue(result.isSuccess(), "Expected success but got: " + result.getMessage());
            assertEquals("Authentication successful", result.getMessage());
        }
    }

    @Test
    @DisplayName("Should reject wrong password")
    @TestTransaction
    void shouldRejectWrongPassword() {
        final AuthResultDto result = this.authService.authenticate("admin", "wrongpassword");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should reject non-existent user")
    @TestTransaction
    void shouldRejectNonExistentUser() {
        final AuthResultDto result = this.authService.authenticate("nonexistent", "password");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should reject banned user")
    @TestTransaction
    void shouldRejectBannedUser() {
        final var student = this.userRepository.findByUsername("student1");
        assertNotNull(student, "Seeded student1 must exist");
        student.banned = true;
        this.userRepository.persist(student);

        final AuthResultDto result = this.authService.authenticate("student1", "student1");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("Should reject non-activated user")
    @TestTransaction
    void shouldRejectNonActivatedUser() {
        final var student = this.userRepository.findByUsername("student2");
        assertNotNull(student, "Seeded student2 must exist");
        student.activated = false;
        this.userRepository.persist(student);

        final AuthResultDto result = this.authService.authenticate("student2", "student2");
        assertFalse(result.isSuccess());
        assertEquals("Invalid username or password", result.getMessage());
    }

    @Test
    @DisplayName("logout should clear session")
    void testLogout_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class);
                MockedStatic<VaadinService> mockedService = mockStatic(VaadinService.class)) {

            final VaadinSession mockSess = mock(VaadinSession.class);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            mockedService.when(() -> VaadinService.reinitializeSession(any())).thenAnswer(i -> null);

            this.authService.logout();

            // Verify session clearing calls with correct keys
            verify(mockSess).setAttribute(USERNAME_KEY, null);
            verify(mockSess).setAttribute(AUTHENTICATED_KEY, false);
            verify(mockSess).setAttribute(LAST_DB_CHECK_KEY, null);
        }
    }

    @Test
    @DisplayName("isAuthenticated returns true when session has username and valid check")
    void testIsAuthenticated_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn("admin");
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(System.currentTimeMillis());
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertTrue(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("getUsername returns username from session")
    void testGetUsername_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn("admin");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertEquals("admin", this.authService.getUsername());
        }
    }

    @Test
    @DisplayName("getUserId returns userId from session user")
    @TestTransaction
    void testGetUserId_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn("admin");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            final Long userId = this.authService.getUserId();
            assertNotNull(userId);
            final var user = this.userRepository.findByUsername("admin");
            assertEquals(user.id, userId);
        }
    }

    @Test
    @DisplayName("getCurrentUserEntity returns entity for session user")
    @TestTransaction
    void testGetCurrentUserEntity_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn("admin");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            final var user = this.authService.getCurrentUserEntity();
            assertNotNull(user);
            assertEquals("admin", user.username);
        }
    }

    @Test
    @DisplayName("logout returns silently when no session exists")
    void testLogout_noSession() {
        this.authService.logout();
        assertFalse(this.authService.isAuthenticated());
    }

    @Test
    @DisplayName("isAuthenticated returns false when no session exists")
    void testIsAuthenticated_noSession() {
        assertFalse(this.authService.isAuthenticated(), "isAuthenticated should return false when no VaadinSession");
    }

    @Test
    @DisplayName("getUsername returns null when no session exists")
    void testGetUsername_noSession() {
        assertNull(this.authService.getUsername(), "getUsername should return null when no VaadinSession");
    }

    @Test
    @DisplayName("getUserId returns null when no session exists")
    void testGetUserId_noSession() {
        assertNull(this.authService.getUserId(), "getUserId should return null when no VaadinSession");
    }

    @Test
    @DisplayName("getCurrentUserEntity returns null when no session exists")
    void testGetCurrentUserEntity_noSession() {
        assertNull(this.authService.getCurrentUserEntity(),
                "getCurrentUserEntity should return null when no VaadinSession");
    }

    @Test
    @DisplayName("isAuthenticated returns false when authenticated flag is null in session")
    void testIsAuthenticated_nullAuthenticatedFlag_returnsFalse() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertFalse(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("isAuthenticated returns false when authenticated flag is false in session")
    void testIsAuthenticated_falseAuthenticatedFlag_returnsFalse() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(false);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertFalse(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("isAuthenticated returns false when username is null despite authenticated flag")
    void testIsAuthenticated_nullUsername_returnsFalse() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertFalse(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("isAuthenticated performs DB check and returns true when cache is stale")
    @TestTransaction
    void testIsAuthenticated_staleCacheNull_validUser_returnsTrue() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.buildStaleCacheSession("admin");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertTrue(this.authService.isAuthenticated());
            verify(mockSess).setAttribute(eq(LAST_DB_CHECK_KEY), any(Long.class));
        }
    }

    @Test
    @DisplayName("isAuthenticated performs DB check and returns false when user not found")
    void testIsAuthenticated_staleCacheNull_noUser_returnsFalse() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.buildStaleCacheSession("no_such_user_xyz");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertFalse(this.authService.isAuthenticated());
            verify(mockSess).setAttribute(LAST_DB_CHECK_KEY, null);
        }
    }

    private VaadinSession buildStaleCacheSession(final String username) {
        final VaadinSession mockSess = mock(VaadinSession.class);
        when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
        when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(null);
        when(mockSess.getAttribute(USERNAME_KEY)).thenReturn(username);
        return mockSess;
    }

    @Test
    @DisplayName("getUserId returns null when username exists in session but user is not in DB")
    void testGetUserId_unknownUsername_returnsNull() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn("no_such_user_xyz");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertNull(this.authService.getUserId());
        }
    }

    @Test
    @DisplayName("Should throttle after too many failed attempts")
    void shouldThrottleAfterTooManyFailedAttempts() {
        final String uniqueKey = "throttle_test_" + UUID.randomUUID().toString().substring(0, 8);
        final String compositeKey = uniqueKey + ":unknown";
        for (int i = 0; i < 5; i++) {
            this.loginAttemptService.recordFailedAttempt(compositeKey);
        }
        assertTrue(this.loginAttemptService.isLockedOut(compositeKey));

        final AuthResultDto result = this.authService.authenticate(uniqueKey, "anypassword");
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Too many failed attempts"),
                "Expected throttle message, got: " + result.getMessage());

        this.loginAttemptService.recordSuccessfulLogin(compositeKey);
    }

    @Test
    @DisplayName("isAuthenticated should re-validate against DB after cache eviction")
    @TestTransaction
    void testIsAuthenticated_afterEviction() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            final String username = "admin";
            final long now = System.currentTimeMillis();

            when(mockSess.getAttribute(USERNAME_KEY)).thenReturn(username);
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            // Valid cache (within TTL)
            when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(now);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            // 1. Initially should be authenticated via cache
            assertTrue(this.authService.isAuthenticated());

            // 2. Evict cache for this user
            this.authService.evictCache(username);

            // 3. Should now hit the DB even though lastCheck is recent
            assertTrue(this.authService.isAuthenticated());
            // Verify DB hit caused a new lastCheck to be set
            verify(mockSess).setAttribute(eq(LAST_DB_CHECK_KEY), any(Long.class));
        }
    }
}
