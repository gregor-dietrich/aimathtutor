package de.vptr.aimathtutor.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProviderExceptionTest {

    @ParameterizedTest
    @ValueSource(ints = { 400, 401, 403, 404, 407, 409, 422, 499 })
    @DisplayName("httpFailure preserves metadata for permanent client errors")
    void testHttpFailurePermanent(final int status) {
        final var ex = ProviderException.httpFailure("TestProvider", status, "Request rejected");
        assertEquals(NonRetryableProviderException.class, ex.getClass());
        assertEquals("TestProvider", ex.getProviderName());
        assertEquals(status, ex.getHttpStatus());
        assertEquals("TestProvider API error (HTTP " + status + "): Request rejected", ex.getMessage());
        assertNull(ex.getCause());
    }

    @ParameterizedTest
    @ValueSource(ints = { 408, 429, 500, 502, 503, 504 })
    @DisplayName("httpFailure keeps timeouts, rate limits and server errors retryable with metadata")
    void testHttpFailureRetryable(final int status) {
        final var ex = ProviderException.httpFailure("TestProvider", status, "Try again later");
        assertEquals(ProviderException.class, ex.getClass());
        assertEquals("TestProvider", ex.getProviderName());
        assertEquals(status, ex.getHttpStatus());
        assertEquals("TestProvider API error (HTTP " + status + "): Try again later", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @DisplayName("transportFailure sets NO_HTTP_STATUS and wraps cause")
    void testTransportFailure() {
        final var cause = new RuntimeException("network failure");
        final var ex = ProviderException.transportFailure("TestProvider", "Transport failed", cause);
        assertEquals(ProviderException.class, ex.getClass());
        assertEquals("TestProvider", ex.getProviderName());
        assertEquals(ProviderException.NO_HTTP_STATUS, ex.getHttpStatus());
        assertEquals(cause, ex.getCause());
        assertEquals("TestProvider: Transport failed", ex.getMessage());
    }

    @Test
    @DisplayName("NonRetryableProviderException two-arg constructor sets fields correctly")
    void testNonRetryable_twoArgs() {
        final var ex = new NonRetryableProviderException("MyProvider", "Key missing");
        assertEquals("MyProvider", ex.getProviderName());
        assertEquals(ProviderException.NO_HTTP_STATUS, ex.getHttpStatus());
        final String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("MyProvider"));
    }

    @Test
    @DisplayName("NonRetryableProviderException three-arg constructor wraps cause")
    void testNonRetryable_threeArgs() {
        final var cause = new IllegalArgumentException("bad config");
        final var ex = new NonRetryableProviderException("MyProvider", "Config error", cause);
        assertEquals("MyProvider", ex.getProviderName());
        assertEquals(cause, ex.getCause());
    }
}
