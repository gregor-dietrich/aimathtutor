package de.vptr.aimathtutor.service.ai;

import java.io.IOException;

/**
 * Unified runtime exception thrown by AI provider services
 * (OpenAI, Gemini, Ollama). Carries the provider name and an optional HTTP
 * status code so callers can react uniformly without depending on
 * provider-specific exception hierarchies.
 *
 * <p>
 * This base type represents a transient failure and is eligible for retry.
 * Permanent failures (missing API key, blocked content, misconfiguration)
 * are signalled with {@link NonRetryableAiProviderException}; the
 * {@code @Retry(abortOn = NonRetryableAiProviderException.class)} annotation
 * on the calling methods skips retries in those cases.
 */
public class AiProviderException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Sentinel used when the failure did not originate from an HTTP response. */
    public static final int NO_HTTP_STATUS = 0;

    private final String providerName;
    private final int httpStatus;

    /**
     * Build an exception backed by an HTTP failure.
     */
    public static AiProviderException httpFailure(final String providerName, final int httpStatus,
            final String message) {
        return new AiProviderException(providerName, message, httpStatus, null);
    }

    /**
     * Build an exception wrapping a transport-level cause
     * (e.g. {@link IOException}).
     */
    public static AiProviderException transportFailure(final String providerName, final String message,
            final Throwable cause) {
        return new AiProviderException(providerName, message, NO_HTTP_STATUS, cause);
    }

    protected AiProviderException(final String providerName, final String message, final int httpStatus,
            final Throwable cause) {
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
     * @return the HTTP status, or {@link #NO_HTTP_STATUS} if the failure did not
     *         originate from an HTTP response.
     */
    public int getHttpStatus() {
        return this.httpStatus;
    }
}
