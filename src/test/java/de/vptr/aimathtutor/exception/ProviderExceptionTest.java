package de.vptr.aimathtutor.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProviderExceptionTest {

    @Test
    @DisplayName("httpFailure sets provider name, status, and message correctly")
    void testHttpFailure() {
        final var ex = ProviderException.httpFailure("TestProvider", 500, "Internal error");
        assertEquals("TestProvider", ex.getProviderName());
        assertEquals(500, ex.getHttpStatus());
        final String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("500"));
        assertTrue(msg.contains("TestProvider"));
    }

    @Test
    @DisplayName("transportFailure sets NO_HTTP_STATUS and wraps cause")
    void testTransportFailure() {
        final var cause = new RuntimeException("network failure");
        final var ex = ProviderException.transportFailure("TestProvider", "Transport failed", cause);
        assertEquals("TestProvider", ex.getProviderName());
        assertEquals(ProviderException.NO_HTTP_STATUS, ex.getHttpStatus());
        assertEquals(cause, ex.getCause());
        final String msg = ex.getMessage();
        assertNotNull(msg);
        assertTrue(msg.contains("TestProvider"));
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
        assertTrue(ex instanceof ProviderException);
    }
}
