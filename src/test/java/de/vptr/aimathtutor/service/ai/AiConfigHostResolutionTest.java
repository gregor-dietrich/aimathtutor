package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.security.AuthService;
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
        this.credentials = new SessionCredentials(admin.publicId, AuthService.credentialStamp(admin.password),
                UUID.randomUUID().toString());
    }

    @AfterEach
    void tearDown() {
        this.aiConfigService.clearCache();
    }

    @ParameterizedTest(name = "resolver returning {0} is rejected")
    @ValueSource(strings = { "127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254", "0.0.0.0",
            "224.0.0.1", "::1", "fe80::1", "fd00::1" })
    @DisplayName("validateProviderApiUrl - allow-listed host resolving to a non-public address is rejected")
    void testValidateProviderApiUrlRejectsNonPublicResolution(final String address) throws UnknownHostException {
        when(this.hostResolver.resolveAll("generativelanguage.googleapis.com"))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral(address) });

        final var ex = assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("https://generativelanguage.googleapis.com",
                        AiConfigService.ProviderType.GOOGLE));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("non-public address"), ex.getMessage());
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
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("non-public address"), ex.getMessage());
    }

    @Test
    @DisplayName("validateProviderApiUrl - unresolvable allow-listed host is rejected")
    void testValidateProviderApiUrlRejectsUnresolvableHost() throws UnknownHostException {
        when(this.hostResolver.resolveAll("generativelanguage.googleapis.com"))
                .thenThrow(new UnknownHostException("test"));

        final var ex = assertThrows(IllegalArgumentException.class,
                () -> this.aiConfigService.validateProviderApiUrl("https://generativelanguage.googleapis.com",
                        AiConfigService.ProviderType.GOOGLE));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("could not be resolved"), ex.getMessage());
    }

    @SuppressWarnings("NullAway")
    @Test
    @DisplayName("updateConfig - generic URL key whose host resolves to a private address is refused")
    @TestTransaction
    void testUpdateConfigGenericUrlKeyRejectsPrivateResolution() throws UnknownHostException {
        when(this.hostResolver.resolveAll("custom.example"))
                .thenReturn(new InetAddress[] { InetAddress.ofLiteral("10.0.0.1") });

        final var ex = assertThrows(ValidationException.class,
                () -> this.aiConfigService.updateConfig("custom.url", "https://custom.example/", this.credentials));
        assertTrue(Objects.requireNonNull(ex.getMessage()).contains("Private IP addresses are not allowed"),
                ex.getMessage());
    }

    @Test
    @DisplayName("validateProviderApiUrl - an Ollama URL never reaches the resolver")
    void testValidateProviderApiUrlOllamaSkipsResolver() {
        this.aiConfigService.validateProviderApiUrl("http://ollama:11434", AiConfigService.ProviderType.OLLAMA);

        verifyNoInteractions(this.hostResolver);
    }
}
