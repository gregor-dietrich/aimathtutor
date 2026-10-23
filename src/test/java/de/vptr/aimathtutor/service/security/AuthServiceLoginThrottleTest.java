package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.aimathtutor.dto.AuthResultDto;
import de.vptr.aimathtutor.util.ClientIpUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

// The subject of this test is IP address literals
@QuarkusTest
@SuppressWarnings({ "NullAway", "PMD.AvoidUsingHardCodedIP" })
class AuthServiceLoginThrottleTest {

    @Inject
    private AuthService authService;

    @Inject
    private LoginAttemptService loginAttemptService;

    private static final String PASSWORD = "P@ssw0rd1";

    private static String randomHextet() {
        return Integer.toHexString(ThreadLocalRandom.current().nextInt(0x1000, 0xffff));
    }

    static String randomDocumentationIpv6() {
        return "2001:db8:" + randomHextet() + ":" + randomHextet() + "::" + randomHextet();
    }

    @Test
    @DisplayName("a successful sign-in in between does not reset the IP bucket for spraying other usernames")
    void successfulLoginDoesNotResetIpBucket() {
        final var client = randomDocumentationIpv6();
        final var clientKey = ClientIpUtil.loginClientKey(client);
        final var own = "student1";
        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class);
                MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class);
                MockedStatic<VaadinService> mockedService = mockStatic(VaadinService.class)) {
            final VaadinRequest mockReq = mock(VaadinRequest.class);
            when(mockReq.getRemoteAddr()).thenReturn(client);
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(mockReq);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(mock(VaadinSession.class));
            mockedService.when(() -> VaadinService.reinitializeSession(any())).thenAnswer(i -> null);
            for (var i = 0; i < 4; i++) {
                final var other = UUID.randomUUID().toString();
                assertEquals("Invalid username or password",
                        this.authService.authenticate(other, "wrong-" + PASSWORD).getMessage());
                if (i == 1) {
                    assertTrue(this.authService.authenticate(own, own).isSuccess());
                }
            }
            assertTrue(this.authService.authenticate(own, own).isSuccess());
            assertFalse(this.loginAttemptService.isLockedOut(clientKey));

            final var last = UUID.randomUUID().toString();
            this.authService.authenticate(last, "wrong-" + PASSWORD);
            assertTrue(this.loginAttemptService.isLockedOut(clientKey), "5 failures in total lock the IP bucket");
        }
    }

    @Test
    @DisplayName("two IPv6 addresses in one /64 share the IP bucket")
    void ipv6AddressesOfOnePrefixShareTheIpBucket() {
        final var prefix = "2001:db8:" + randomHextet() + ":" + randomHextet();
        final var clientKey = ClientIpUtil.loginClientKey(prefix + "::1");
        for (var i = 0; i < 5; i++) {
            this.authenticateFrom(UUID.randomUUID().toString(), "wrong-" + PASSWORD, prefix + "::" + (i + 1));
        }
        assertTrue(this.loginAttemptService.isLockedOut(clientKey));
        final var result =
                this.authenticateFrom(UUID.randomUUID().toString(), "wrong-" + PASSWORD, prefix + ":0:0:0:ffff");
        assertTrue(result.getMessage().contains("Too many failed attempts"), result.getMessage());
    }

    @Test
    @DisplayName("two IPv6 addresses in one /64 share the username+IP bucket")
    void ipv6AddressesOfOnePrefixShareTheUsernameBucket() {
        final var prefix = "2001:db8:" + randomHextet() + ":" + randomHextet();
        final var username = UUID.randomUUID().toString();
        final var usernameIpKey = username + ":" + ClientIpUtil.loginClientKey(prefix + "::1");
        for (var i = 0; i < 5; i++) {
            this.authenticateFrom(username, "wrong-" + PASSWORD, prefix + "::" + (i + 1));
        }
        assertTrue(this.loginAttemptService.isLockedOut(usernameIpKey), "five addresses, one bucket");
        final var result = this.authenticateFrom(username, "wrong-" + PASSWORD, prefix + ":0:0:0:ffff");
        assertTrue(result.getMessage().contains("Too many failed attempts"), result.getMessage());
    }

    @Test
    @DisplayName("a second account signing in from the client forgives exactly one other failure")
    void secondAccountForgivesOneFailure() {
        final var client = randomDocumentationIpv6();
        final var clientKey = ClientIpUtil.loginClientKey(client);
        this.failFromUnknownUsers(client, 4);
        assertTrue(this.authenticateFrom("student1", "student1", client).isSuccess());
        assertTrue(this.authenticateFrom("student2", "student2", client).isSuccess());

        this.assertLocksOnSecondFailure(client, clientKey);
    }

    @Test
    @DisplayName("a sign-in subtracts that account's own failures from the client bucket")
    void signInSubtractsOwnFailures() {
        final var client = randomDocumentationIpv6();
        final var clientKey = ClientIpUtil.loginClientKey(client);
        this.authenticateFrom("student1", "wrong-" + PASSWORD, client);
        this.failFromUnknownUsers(client, 3);
        assertTrue(this.authenticateFrom("student1", "student1", client).isSuccess());

        this.assertLocksOnSecondFailure(client, clientKey);
    }

    private void failFromUnknownUsers(final String client, final int times) {
        for (var i = 0; i < times; i++) {
            this.authenticateFrom(UUID.randomUUID().toString(), "wrong-" + PASSWORD, client);
        }
    }

    /** Asserts that the client bucket, left at 3, takes one more failure and is locked by the next. */
    private void assertLocksOnSecondFailure(final String client, final String clientKey) {
        this.failFromUnknownUsers(client, 1);
        assertFalse(this.loginAttemptService.isLockedOut(clientKey), "4 failures do not lock");
        this.failFromUnknownUsers(client, 1);
        assertTrue(this.loginAttemptService.isLockedOut(clientKey), "5 failures lock");
    }

    private AuthResultDto authenticateFrom(final String username, final String password, final String remoteAddr) {
        return authenticateFrom(this.authService, mock(VaadinSession.class), username, password, remoteAddr);
    }

    /** Authenticates from the given address with the Vaadin statics stubbed, so a success reaches {@code session}. */
    static AuthResultDto authenticateFrom(final AuthService authService, final VaadinSession session,
            final String username, final String password, final String remoteAddr) {
        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class);
                MockedStatic<VaadinSession> mockedSession = mockStatic(VaadinSession.class);
                MockedStatic<VaadinService> mockedService = mockStatic(VaadinService.class)) {
            final VaadinRequest mockReq = mock(VaadinRequest.class);
            when(mockReq.getRemoteAddr()).thenReturn(remoteAddr);
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(mockReq);
            mockedSession.when(VaadinSession::getCurrent).thenReturn(session);
            mockedService.when(() -> VaadinService.reinitializeSession(any())).thenAnswer(i -> null);
            return authService.authenticate(username, password);
        }
    }
}
