package de.vptr.aimathtutor.service.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;

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

    /**
     * Set the mock client on the service being tested.
     */
    protected abstract void setupClient();
}
