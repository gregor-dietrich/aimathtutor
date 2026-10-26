package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.util.TestSessionCredentials;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;

/**
 * The SSRF checks of {@link AiConfigService} on what a host resolves to: the mocked {@link HostResolver} returns
 * non-public addresses, or fails, without any real DNS lookup.
 */
@QuarkusTest
@DisplayName("AiConfigService host resolution checks")
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
class AiConfigHostResolutionTest {

    @Inject
    AiConfigService aiConfigService;

    @Inject
    UserRepository userRepository;

    @InjectMock
    HostResolver hostResolver;

    private SessionCredentials credentials;

    @BeforeEach
    @SuppressWarnings("NullAway")
    void setUp() throws UnknownHostException {
        this.aiConfigService.clearCache();
        when(this.hostResolver.resolveAll(anyString())).thenCallRealMethod();

        // The seeded admin, so the session credentials' stamp matches its stored password hash
        final var admin =
                Objects.requireNonNull(this.userRepository.findByUsername("admin"), "Seeded admin should exist");
        this.credentials = TestSessionCredentials.of(admin);
    }

    @AfterEach
    void tearDown() {
        this.aiConfigService.clearCache();
    }

    @ParameterizedTest(name = "resolver returning {0} is rejected")
    @ValueSource(strings = { "127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254", "0.0.0.0",
            "224.0.0.1", "::1", "fe80::1", "fd00::1", "100.64.0.1", "100.127.255.254", "198.18.0.1", "198.19.255.254",
            "192.0.0.8", "192.0.2.1", "192.88.99.2", "198.51.100.1", "203.0.113.1", "240.0.0.1", "255.255.255.255",
            "0.1.2.3", "64:ff9b:1::1", "100::1", "100:0:0:1::1", "2001:db8::1", "2001:2::1", "3fff::1", "5f00::1",
            "64:ff9b::a00:1", "2002:a00:1::1", "::ffff:10.0.0.1" })
    @DisplayName("validateProviderApiUrl - allow-listed host resolving to a non-public address is rejected")
    void testValidateProviderApiUrlRejectsNonPublicResolution(final String address) throws UnknownHostException {
        when(this.hostResolver.resolveAll("generativelanguage.googleapis.com"))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(address) });

        assertGoogleUrlRejected("non-public address");
    }

    @ParameterizedTest(name = "resolver returning {0} is accepted")
    @ValueSource(strings = { "100.63.255.255", "100.128.0.0", "198.17.255.255", "198.20.0.0", "192.88.99.1", "8.8.8.8",
            "2001:4860:4860::8888", "64:ff9b::808:808", "2002:808:808::1", "::ffff:8.8.8.8" })
    @DisplayName("validateProviderApiUrl - allow-listed host resolving to a public address is accepted")
    void testValidateProviderApiUrlAcceptsPublicResolution(final String address) throws UnknownHostException {
        when(this.hostResolver.resolveAll("generativelanguage.googleapis.com"))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(address) });

        assertDoesNotThrow(() -> this.aiConfigService.validateProviderApiUrl(
                "https://generativelanguage.googleapis.com", AiConfigService.ProviderType.GOOGLE));
    }

    @Test
    @DisplayName("InetAddress.ofLiteral - an IPv4-mapped literal is an Inet4Address")
    void testMappedLiteralIsInet4Address() {
        assertInstanceOf(Inet4Address.class, InetAddress.ofLiteral("::ffff:10.0.0.1"));
    }

    @Test
    @DisplayName("validateProviderApiUrl - one non-public address among public ones is rejected")
    void testValidateProviderApiUrlRejectsMixedResolution() throws UnknownHostException {
        final var publicAddress = InetAddress.ofLiteral(TestHostResolver.PUBLIC_ADDRESS);
        // The public address alone passes, so the rejection below is the private one's
        when(this.hostResolver.resolveAll("api.openai.com")).thenReturn(new InetAddress[] { publicAddress });
        this.aiConfigService.validateProviderApiUrl("https://api.openai.com/v1", AiConfigService.ProviderType.OPENAI);

        when(this.hostResolver.resolveAll("api.openai.com"))
                .thenReturn(new InetAddress[] { publicAddress, InetAddress.ofLiteral("10.0.0.1") });

        final var ex = assertThrows(IllegalArgumentException.class, () -> this.aiConfigService
                .validateProviderApiUrl("https://api.openai.com/v1", AiConfigService.ProviderType.OPENAI));
        assertMessageContains(ex, "non-public address");
    }

    @Test
    @DisplayName("validateProviderApiUrl - unresolvable allow-listed host is rejected")
    void testValidateProviderApiUrlRejectsUnresolvableHost() throws UnknownHostException {
        when(this.hostResolver.resolveAll("generativelanguage.googleapis.com"))
                .thenThrow(new UnknownHostException("test"));

        assertGoogleUrlRejected("could not be resolved");
    }

    private void assertGoogleUrlRejected(final String message) {
        final var ex = assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("https://generativelanguage.googleapis.com",
                        AiConfigService.ProviderType.GOOGLE));
        assertMessageContains(ex, message);
    }

    private static void assertMessageContains(final Exception ex, final String expected) {
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains(expected), ex.getMessage());
    }

    @Test
    @DisplayName("updateConfig - generic URL key whose host resolves to a private address is refused")
    @TestTransaction
    void testUpdateConfigGenericUrlKeyRejectsPrivateResolution() throws UnknownHostException {
        when(this.hostResolver.resolveAll("custom.example"))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral("10.0.0.1") });

        assertGenericUrlRefused("Private IP addresses are not allowed");
    }

    @Test
    @DisplayName("updateConfig - generic URL key whose host does not resolve is refused")
    @TestTransaction
    void testUpdateConfigGenericUrlKeyRejectsUnresolvableHost() throws UnknownHostException {
        when(this.hostResolver.resolveAll("custom.example")).thenThrow(new UnknownHostException("test"));

        assertGenericUrlRefused("URL host must resolve to a public address");
    }

    @Test
    @DisplayName("updateConfig - generic URL key whose host resolves to a public address is saved")
    @TestTransaction
    void testUpdateConfigGenericUrlKeyAcceptsPublicResolution() throws UnknownHostException {
        when(this.hostResolver.resolveAll("custom.example"))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(TestHostResolver.PUBLIC_ADDRESS) });

        this.aiConfigService.updateConfig("custom.url", "https://custom.example/", this.credentials);

        assertEquals("https://custom.example/", this.aiConfigService.getConfigValue("custom.url", null));
    }

    private void assertGenericUrlRefused(final String message) {
        final var ex = assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("custom.url", "https://custom.example/", this.credentials));
        assertMessageContains(ex, message);
    }

    @Test
    @DisplayName("validateProviderApiUrl - an Ollama URL never reaches the resolver")
    void testValidateProviderApiUrlOllamaSkipsResolver() {
        this.aiConfigService.validateProviderApiUrl("http://ollama:11434", AiConfigService.ProviderType.OLLAMA);

        verifyNoInteractions(this.hostResolver);
    }
}
