package de.vptr.aimathtutor.service.ai;

/**
 * Permanent AI provider failure: missing API key, invalid configuration,
 * content blocked by safety filters, or empty/truncated response that retrying
 * would not fix. Used as the {@code abortOn} target for {@code @Retry} on the
 * provider services so the call fails fast.
 */
public class NonRetryableAiProviderException extends AiProviderException {

    private static final long serialVersionUID = 1L;

    public NonRetryableAiProviderException(final String providerName, final String message) {
        super(providerName, message, NO_HTTP_STATUS, null);
    }

    public NonRetryableAiProviderException(final String providerName, final String message, final Throwable cause) {
        super(providerName, message, NO_HTTP_STATUS, cause);
    }
}
