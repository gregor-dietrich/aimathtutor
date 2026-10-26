package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;

import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/**
 * Base class for JAX-RS based AI service tests.
 */
public abstract class AbstractJaxRsAiServiceTest extends AbstractAiServiceTest {

    protected Client mockClient;
    protected WebTarget mockTarget;
    protected Invocation.Builder mockBuilder;
    protected Response mockResponse;

    @BeforeEach
    void setUp() {
        this.mockClient = mock(Client.class);
        this.mockTarget = mock(WebTarget.class);
        this.mockBuilder = mock(Invocation.Builder.class);
        this.mockResponse = mock(Response.class);

        when(this.mockClient.target(anyString())).thenReturn(this.mockTarget);
        when(this.mockTarget.path(anyString())).thenReturn(this.mockTarget);
        when(this.mockTarget.request(anyString())).thenReturn(this.mockBuilder);
        when(this.mockBuilder.header(anyString(), any())).thenReturn(this.mockBuilder);
        when(this.mockBuilder.post(any(Entity.class))).thenReturn(this.mockResponse);

        setupClient();
        mockCommonConfig();
    }

    protected void assertPermanentHttpFailure(final int status, final Executable request) {
        when(this.mockResponse.getStatus()).thenReturn(status);
        when(this.mockResponse.readEntity(String.class)).thenReturn("Request rejected");
        final var failure = assertThrowsExactly(NonRetryableProviderException.class, request);
        assertEquals(status, failure.getHttpStatus());
    }

    protected void assertRejectedUrlStopsRequest(final AiConfigService.ProviderType provider,
            final Executable request) {
        doThrow(new IllegalArgumentException("URL not allowed")).when(this.aiConfigService)
                .validateProviderApiUrl(anyString(), eq(provider));
        assertThrowsExactly(NonRetryableProviderException.class, request);
        verify(this.mockClient, never()).target(anyString());
    }

    /**
     * Set the mock client on the service being tested.
     */
    protected abstract void setupClient();
}
