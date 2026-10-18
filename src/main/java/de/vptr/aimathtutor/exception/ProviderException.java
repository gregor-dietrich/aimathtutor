package de.vptr.aimathtutor.exception;

import java.io.IOException;

import jakarta.annotation.Nullable;

/**
 * Unified runtime exception thrown by AI provider services (OpenAI, Google, Ollama). Carries the provider name and an
 * optional HTTP status code so callers can react uniformly without depending on provider-specific exception
 * hierarchies.
 *
 * <p>
 * This base type represents a transient failure and is eligible for retry. Permanent failures (missing API key, blocked
 * content, misconfiguration, HTTP client errors other than 408 and 429) are signalled with
 * {@link NonRetryableProviderException}; the {@code @Retry(abortOn = NonRetryableProviderException.class)} annotation
 * on the calling methods skips retries in those cases.
 */
public class ProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sentinel used when the failure did not originate from an HTTP response. */
    public static final int NO_HTTP_STATUS = 0;

    private final String providerName;
    private final int httpStatus;

    /**
     * Build an exception backed by an HTTP failure. Client errors are permanent except for request timeouts (408) and
     * rate limits (429), which remain eligible for retry.
     */
    public static ProviderException httpFailure(final String providerName, final int httpStatus, final String message) {
        if (httpStatus >= 400 && httpStatus < 500 && httpStatus != 408 && httpStatus != 429) {
            return new NonRetryableProviderException(providerName, message, httpStatus);
        }
        return new ProviderException(providerName, message, httpStatus, null);
    }

    /**
     * Build an exception wrapping a transport-level cause (e.g. {@link IOException}).
     */
    public static ProviderException transportFailure(final String providerName, final String message,
            final Throwable cause) {
        return new ProviderException(providerName, message, NO_HTTP_STATUS, cause);
    }

    protected ProviderException(final String providerName, final String message, final int httpStatus,
            @Nullable final Throwable cause) {
        super(formatMessage(providerName, message, httpStatus), cause);
        this.providerName = providerName;
        this.httpStatus = httpStatus;
    }

    private static String formatMessage(final String providerName, final String message, final int httpStatus) {
        if (httpStatus > 0) {
            return providerName + " API error (HTTP " + httpStatus + "): " + message;
        }
        return providerName + ": " + message;
    }

    public String getProviderName() {
        return this.providerName;
    }

    /**
     * Returns the HTTP status carried by this failure.
     *
     * @return the HTTP status, or {@link #NO_HTTP_STATUS} if the failure did not originate from an HTTP response.
     */
    public int getHttpStatus() {
        return this.httpStatus;
    }
}
