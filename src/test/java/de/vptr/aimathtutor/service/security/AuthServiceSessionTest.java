package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.event.UserAccountChangedEvent;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

/** Tests of the session binding: credential stamps, session tokens and revocation. */
@QuarkusTest
@SuppressWarnings("NullAway")
class AuthServiceSessionTest {

    @Inject
    private AuthService authService;

    @Inject
    private UserRepository userRepository;

    private static final String USER_PUBLIC_ID_KEY = AppConstants.SESSION_KEY_USER_PUBLIC_ID;
    private static final String CREDENTIAL_STAMP_KEY = AppConstants.SESSION_KEY_CREDENTIAL_STAMP;
    private static final String SESSION_TOKEN_KEY = "authenticated.sessionToken";
    private static final String AUTHENTICATED_KEY = "authenticated.status";
    private static final String LAST_DB_CHECK_KEY = "authenticated.lastDbCheck";

    /** A logged-in session that validated against the DB just now. */
    private static VaadinSession cachedSessionFor(final UserEntity user) {
        final VaadinSession mockSess = AuthServiceTest.sessionFor(user);
        when(mockSess.getAttribute(AUTHENTICATED_KEY)).thenReturn(true);
        when(mockSess.getAttribute(LAST_DB_CHECK_KEY)).thenReturn(System.currentTimeMillis());
        return mockSess;
    }

    /** A session that holds a public ID but no credential stamp. */
    private static VaadinSession unstampedSessionFor(final UserEntity user) {
        final VaadinSession mockSess = mock(VaadinSession.class);
        when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn(user.publicId);
        return mockSess;
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
            final VaadinSession mockSess = cachedSessionFor(user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertTrue(this.authService.isAuthenticated());

            user.password = "$2a$10$aDifferentHashThatIsNotTheOriginalOneAtAllxxxxxxxxxxxxxxxxxx";
            this.userRepository.persist(user);
            this.authService.evictCache(user.publicId);

            assertFalse(this.authService.isAuthenticated());
        }
    }

    @Test
    @DisplayName("a stale public ID resolves to no one, even though a user with the old name and password exists")
    @TestTransaction
    void stalePublicIdDoesNotResolveToTheUserWhoTookTheName() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            // the session was issued to an account that was deleted; student1 now carries the name and password
            final var successor = this.userRepository.findByUsername("student1");
            final VaadinSession mockSess = AuthServiceTest.buildStaleCacheSession(successor);
            when(mockSess.getAttribute(USER_PUBLIC_ID_KEY)).thenReturn("01STALEPUBLICIDOFADELETEDUSER");
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertFalse(this.authService.isAuthenticated());
            assertNull(this.authService.getCurrentUserEntity());
            assertNull(this.authService.getUsername());
            assertNull(this.authService.getUserId());
        }
    }

    @TestTransaction
    void isAuthenticatedFalseForDeletedUserWithReusedName() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var admin = this.userRepository.findByUsername("admin");
            final VaadinSession mockSess = AuthServiceTest.sessionFor(admin);
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
            final VaadinSession mockSess = AuthServiceTest.buildStaleCacheSession(user);
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
            readTime.set(System.currentTimeMillis());
            Thread.sleep(10);
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
            assertTrue(stored.getValue() <= readTime.get(), "stored " + stored.getValue() + " read " + readTime.get());
        }
    }

    @Test
    @DisplayName("renewCredentialStamp sets the stamp only when it matches the DB")
    @TestTransaction
    void renewCredentialStampOnlyWhenMatching() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var user = this.userRepository.findByUsername("student1");
            final VaadinSession mockSess = unstampedSessionFor(user);
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
            final VaadinSession mockSess = cachedSessionFor(user);
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

    /** A session that really stores what is set on it. */
    private static VaadinSession statefulSession(final Map<String, Object> attributes) {
        final VaadinSession session = mock(VaadinSession.class);
        when(session.getAttribute(any(String.class)))
                .thenAnswer(invocation -> attributes.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> attributes.put(invocation.getArgument(0), invocation.getArgument(1))).when(session)
                .setAttribute(any(String.class), any());
        return session;
    }

    @Test
    @DisplayName("a failed re-validation clears the session, and a later unban does not revive it")
    @TestTransaction
    void failedRevalidationRevokesTheSessionForGood() {
        final var user = this.userRepository.findByUsername("student1");
        final var attributes = new HashMap<String, Object>();
        attributes.put(USER_PUBLIC_ID_KEY, user.publicId);
        attributes.put(CREDENTIAL_STAMP_KEY, AuthService.credentialStamp(user.password));
        attributes.put(SESSION_TOKEN_KEY, "token");
        attributes.put(AUTHENTICATED_KEY, true);
        final VaadinSession mockSess = statefulSession(attributes);

        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertTrue(this.authService.isAuthenticated());
            user.banned = true;
            this.authService.evictCache(user.publicId);

            assertFalse(this.authService.isAuthenticated());
            assertNull(attributes.get(USER_PUBLIC_ID_KEY));
            assertNull(attributes.get(CREDENTIAL_STAMP_KEY));
            assertNull(attributes.get(SESSION_TOKEN_KEY));
            assertEquals(false, attributes.get(AUTHENTICATED_KEY));
            assertNull(attributes.get(LAST_DB_CHECK_KEY));

            user.banned = false;
            assertFalse(this.authService.isAuthenticated(), "unbanning must not revive the session");
        }
    }

    @Test
    @DisplayName("login stores a random per-session token, and currentSessionCredentials reads the session only")
    @TestTransaction
    void currentSessionCredentialsCarryTheSessionToken() {
        final var attributes = new HashMap<String, Object>();
        final VaadinSession mockSess = statefulSession(attributes);

        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class);
                MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(null);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);
            assertNull(this.authService.currentSessionCredentials(), "not signed in yet");

            assertTrue(this.authService.authenticate("teacher", "teacher").isSuccess());
            final var first = this.authService.currentSessionCredentials();
            assertTrue(this.authService.authenticate("teacher", "teacher").isSuccess());
            final var second = this.authService.currentSessionCredentials();

            final var teacher = this.userRepository.findByUsername("teacher");
            assertEquals(teacher.publicId, first.userPublicId());
            assertEquals(AuthService.credentialStamp(teacher.password), first.credentialStamp());
            assertNotNull(first.throttleKey());
            assertNotEquals(first.throttleKey(), second.throttleKey());
        }
    }

    @Test
    @DisplayName("currentSessionCredentials is null without a session")
    void currentSessionCredentialsNullWithoutSession() {
        assertNull(this.authService.currentSessionCredentials());
    }

    @Test
    @DisplayName("renewCredentialStamp does nothing without a session")
    void renewCredentialStampWithoutSession() {
        assertDoesNotThrow(() -> this.authService.renewCredentialStamp("stamp"));
    }

    @Test
    @DisplayName("renewCredentialStamp sets nothing when the session has no public ID")
    void renewCredentialStampWithoutPublicId() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final VaadinSession mockSess = mock(VaadinSession.class);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            this.authService.renewCredentialStamp("stamp");

            verify(mockSess, never()).setAttribute(any(String.class), any());
        }
    }

    @Test
    @DisplayName("getCurrentUserEntity is null when the session has a public ID but no stamp")
    @TestTransaction
    void currentUserEntityNullWithoutStamp() {
        try (MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class)) {
            final var user = this.userRepository.findByUsername("student1");
            final VaadinSession mockSess = unstampedSessionFor(user);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mockSess);

            assertNull(this.authService.getCurrentUserEntity());
        }
    }
}
