package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.UserAccountChangedEvent;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusMock;
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

    private static final String USER_PUBLIC_ID_KEY = AppConstants.SESSION_KEY_USER_PUBLIC_ID;
    private static final String CREDENTIAL_STAMP_KEY = "authenticated.credentialStamp";
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
            verify(mockSess).setAttribute(USER_PUBLIC_ID_KEY, null);
            verify(mockSess).setAttribute(CREDENTIAL_STAMP_KEY, null);
            verify(mockSess).setAttribute(AUTHENTICATED_KEY, false);
            verify(mockSess).setAttribute(LAST_DB_CHECK_KEY, null);
        }
    }

    @Test
    @DisplayName("isAuthenticated returns true when session has a user public ID and a valid check")
    void testIsAuthenticated_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn("some-public-id");
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(System.currentTimeMillis());
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertTrue(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("getUsername returns the current username of the session's user")
    @TestTransaction
    void testGetUsername_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.sessionFor(this.userRepository.findByUsername("admin"));
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertEquals("admin", this.authService.getUsername());
        }
    }

    @Test
    @DisplayName("getUserId returns userId from session user")
    @TestTransaction
    void testGetUserId_withSession() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.sessionFor(this.userRepository.findByUsername("admin"));
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
            final VaadinSession mockSess = this.sessionFor(this.userRepository.findByUsername("admin"));
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
    @DisplayName("isAuthenticated returns false when the public ID is null despite authenticated flag")
    void testIsAuthenticated_nullUsername_returnsFalse() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertFalse(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("isAuthenticated performs DB check and returns true when cache is stale")
    @TestTransaction
    void testIsAuthenticated_staleCacheNull_validUser_returnsTrue() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.buildStaleCacheSession(this.userRepository.findByUsername("admin"));
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertTrue(this.authService.isAuthenticated());
            verify(mockSess).setAttribute(eq(LAST_DB_CHECK_KEY), any(Long.class));
        }
    }

    @Test
    @DisplayName("isAuthenticated performs DB check and returns false when user not found")
    void testIsAuthenticated_staleCacheNull_noUser_returnsFalse() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.buildStaleCacheSession(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertFalse(this.authService.isAuthenticated());
            verify(mockSess).setAttribute(LAST_DB_CHECK_KEY, null);
        }
    }

    /** A session logged in as the user, as {@code authenticate} leaves it; null stands for a public ID no user has. */
    private VaadinSession sessionFor(final UserEntity user) {
        final VaadinSession mockSess = mock(VaadinSession.class);
        when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn(user != null ? user.publicId : "no-such-public-id");
        when(mockSess.getAttribute(CREDENTIAL_STAMP_KEY))
                .thenReturn(user != null ? AuthService.credentialStamp(user.password) : null);
        return mockSess;
    }

    /** A logged-in session that validated against the DB just now. */
    private VaadinSession cachedSessionFor(final UserEntity user) {
        final VaadinSession mockSess = this.sessionFor(user);
        when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
        when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(System.currentTimeMillis());
        return mockSess;
    }

    private VaadinSession buildStaleCacheSession(final UserEntity user) {
        final VaadinSession mockSess = this.sessionFor(user);
        when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
        when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(null);
        return mockSess;
    }

    @Test
    @DisplayName("getUserId returns null when the public ID in the session is not in DB")
    void testGetUserId_unknownUsername_returnsNull() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = this.sessionFor(null);
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
            final var admin = this.userRepository.findByUsername("admin");
            final VaadinSession mockSess = this.sessionFor(admin);
            final long now = System.currentTimeMillis();

            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            // Valid cache (within TTL)
            when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(now);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            // 1. Initially should be authenticated via cache
            assertTrue(this.authService.isAuthenticated());

            // 2. Evict cache for this user
            this.authService.evictCache(admin.publicId);

            // 3. Should now hit the DB even though lastCheck is recent
            assertTrue(this.authService.isAuthenticated());
            // Verify DB hit caused a new lastCheck to be set
            verify(mockSess).setAttribute(eq(LAST_DB_CHECK_KEY), any(Long.class));
        }
    }

    @Test
    @DisplayName("login stores the public ID and credential stamp, not the username")
    @TestTransaction
    void authenticateStoresPublicIdAndStamp() {
        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class);
                MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(null);
            final VaadinSession mockSess = mock(VaadinSession.class);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertTrue(this.authService.authenticate("teacher", "teacher").isSuccess());

            final var teacher = this.userRepository.findByUsername("teacher");
            verify(mockSess).setAttribute(USER_PUBLIC_ID_KEY, teacher.publicId);
            verify(mockSess).setAttribute(CREDENTIAL_STAMP_KEY, AuthService.credentialStamp(teacher.password));
            verify(mockSess, never()).setAttribute(eq("authenticated.username"), any());
        }
    }

    @Test
    @DisplayName("credentialStamp is null for no hash and differs for different hashes")
    void credentialStampFollowsHash() {
        assertNull(AuthService.credentialStamp(null));
        assertEquals(AuthService.credentialStamp("hash-a"), AuthService.credentialStamp("hash-a"));
        assertNotEquals(AuthService.credentialStamp("hash-a"), AuthService.credentialStamp("hash-b"));
    }

    @Test
    @DisplayName("isAuthenticated is false once the password hash changed since login, after eviction")
    @TestTransaction
    void isAuthenticatedFalseAfterPasswordChange() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var user = this.userRepository.findByUsername("student1");
            final VaadinSession mockSess = this.cachedSessionFor(user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertTrue(this.authService.isAuthenticated());

            user.password = "$2a$10$aDifferentHashThatIsNotTheOriginalOneAtAllxxxxxxxxxxxxxxxxxx";
            this.userRepository.persist(user);
            this.authService.evictCache(user.publicId);

            assertFalse(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("isAuthenticated is false when the public ID no longer resolves, even if another user took the name")
    @TestTransaction
    void isAuthenticatedFalseForDeletedUserWithReusedName() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            final VaadinSession mockSess = this.sessionFor(admin);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn("01STALEPUBLICIDOFADELETEDUSER");
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertFalse(this.authService.isAuthenticated());
            assertNull(this.authService.getUsername());
        }
    }

    @Test
    @DisplayName("a renamed user with the same public ID and hash stays authenticated")
    @TestTransaction
    void renamedUserStaysAuthenticated() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var user = this.userRepository.findByUsername("student2");
            final VaadinSession mockSess = this.buildStaleCacheSession(user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            user.username = "student2_renamed";
            this.userRepository.persist(user);

            assertTrue(this.authService.isAuthenticated());
            assertEquals("student2_renamed", this.authService.getUsername());
        }
    }

    @Test
    @DisplayName("the DB check time is taken before the repository read")
    void lastDbCheckIsTakenBeforeTheRead() {
        final var repository = mock(UserRepository.class);
        QuarkusMock.installMockForType(repository, UserRepository.class);
        final var user = new UserEntity();
        user.publicId = "pid";
        user.password = "hash";
        user.activated = true;
        user.banned = false;
        final var readTime = new AtomicLong();
        when(repository.findByPublicId("pid")).thenAnswer(invocation -> {
            readTime.set(System.currentTimeMillis() + 5);
            return Optional.of(user);
        });

        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn("pid");
            when(mockSess.getAttribute(CREDENTIAL_STAMP_KEY)).thenReturn(AuthService.credentialStamp("hash"));
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertTrue(this.authService.isAuthenticated());

            final var stored = ArgumentCaptor.forClass(Long.class);
            verify(mockSess).setAttribute(eq(LAST_DB_CHECK_KEY), stored.capture());
            assertTrue(stored.getValue() < readTime.get(), "stored " + stored.getValue() + " read " + readTime.get());
        }
    }

    @Test
    @DisplayName("renewCredentialStamp sets the stamp only when it matches the DB")
    @TestTransaction
    void renewCredentialStampOnlyWhenMatching() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var user = this.userRepository.findByUsername("student1");
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn(user.publicId);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            this.authService.renewCredentialStamp("not-the-stamp");
            verify(mockSess, never()).setAttribute(eq(CREDENTIAL_STAMP_KEY), any());

            final var stamp = AuthService.credentialStamp(user.password);
            this.authService.renewCredentialStamp(stamp);
            verify(mockSess).setAttribute(CREDENTIAL_STAMP_KEY, stamp);
        }
    }

    @Test
    @DisplayName("the account-changed observer evicts and renews only the session of its own account")
    @TestTransaction
    void observerEvictsAndRenewsOnlyOwnAccount() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var user = this.userRepository.findByUsername("student1");
            final VaadinSession mockSess = this.cachedSessionFor(user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertTrue(this.authService.isAuthenticated());
            verify(mockSess, never()).setAttribute(eq(LAST_DB_CHECK_KEY), any());

            this.authService.onUserAccountChanged(new UserAccountChangedEvent("another-account", "other-stamp"));
            verify(mockSess, never()).setAttribute(eq(CREDENTIAL_STAMP_KEY), any());
            assertTrue(this.authService.isAuthenticated());
            verify(mockSess, never()).setAttribute(eq(LAST_DB_CHECK_KEY), any());

            this.authService.onUserAccountChanged(new UserAccountChangedEvent(user.publicId, "new-stamp"));
            verify(mockSess).setAttribute(CREDENTIAL_STAMP_KEY, "new-stamp");
            // the eviction forced a DB re-check inside the TTL
            this.authService.isAuthenticated();
            verify(mockSess).setAttribute(eq(LAST_DB_CHECK_KEY), any());
        }
    }

    @Test
    @DisplayName("the account-changed observer for a deletion renews nothing")
    void observerDeletionRenewsNothing() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn("pid");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            this.authService.onUserAccountChanged(new UserAccountChangedEvent("pid", null));

            verify(mockSess, never()).setAttribute(eq(CREDENTIAL_STAMP_KEY), any());
        }
    }
}
