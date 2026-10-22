package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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

    private static String randomDocumentationIpv6() {
        return "2001:db8:" + randomHextet() + ":" + randomHextet() + "::" + randomHextet();
    }

    @Test
    @DisplayName("a successful sign-in in between does not reset the IP bucket for spraying other usernames")
    void successfulLoginDoesNotResetIpBucket() {
        final var client = randomDocumentationIpv6();
        final var clientKey = ClientIpUtil.throttleKey(client);
        final var own = "student1";
        final var ownKey = own + ":" + clientKey;
        final var keys = new ArrayList<String>(List.of(ownKey));
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
                keys.add(other + ":" + clientKey);
                assertEquals("Invalid username or password",
                        this.authService.authenticate(other, "wrong-" + PASSWORD).getMessage());
                if (i == 1) {
                    assertTrue(this.authService.authenticate(own, own).isSuccess());
                }
            }
            assertTrue(this.authService.authenticate(own, own).isSuccess());
            assertFalse(this.loginAttemptService.isLockedOut(clientKey));

            final var last = UUID.randomUUID().toString();
            keys.add(last + ":" + clientKey);
            this.authService.authenticate(last, "wrong-" + PASSWORD);
            assertTrue(this.loginAttemptService.isLockedOut(clientKey), "5 failures in total lock the IP bucket");
        } finally {
            keys.forEach(key -> this.loginAttemptService.recordSuccessfulLogin(key, clientKey));
            this.loginAttemptService.recordSuccessfulLogin(clientKey, null);
        }
    }

    @Test
    @DisplayName("two IPv6 addresses in one /64 share the IP bucket")
    void ipv6AddressesOfOnePrefixShareTheIpBucket() {
        final var prefix = "2001:db8:" + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x1000, 0xffff)) + ":"
                + Integer.toHexString(ThreadLocalRandom.current().nextInt(0x1000, 0xffff));
        final var clientKey = ClientIpUtil.throttleKey(prefix + "::1");
        try {
            for (var i = 0; i < 5; i++) {
                this.authenticateFrom(UUID.randomUUID().toString(), prefix + "::" + (i + 1));
            }
            assertTrue(this.loginAttemptService.isLockedOut(clientKey));
            final var result = this.authenticateFrom(UUID.randomUUID().toString(), prefix + ":0:0:0:ffff");
            assertTrue(result.getMessage().contains("Too many failed attempts"), result.getMessage());
        } finally {
            this.loginAttemptService.recordSuccessfulLogin(clientKey, null);
        }
    }

    private AuthResultDto authenticateFrom(final String username, final String remoteAddr) {
        try (MockedStatic<VaadinRequest> mockedRequest = mockStatic(VaadinRequest.class)) {
            final VaadinRequest mockReq = mock(VaadinRequest.class);
            when(mockReq.getRemoteAddr()).thenReturn(remoteAddr);
            mockedRequest.when(VaadinRequest::getCurrent).thenReturn(mockReq);
            return this.authService.authenticate(username, "wrong-" + PASSWORD);
        }
    }
}
