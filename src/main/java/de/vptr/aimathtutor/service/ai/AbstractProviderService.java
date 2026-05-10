package de.vptr.aimathtutor.service.ai;

import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Base class for AI provider services (OpenAI, Google, Ollama). Provides shared logic for config-driven model lookup,
 * API key validation, and empty-response checks.
 */
public abstract class AbstractProviderService {

    @Inject
    protected AiConfigService aiConfigService;

    /**
     * The configuration key prefix for this provider (e.g. "openai", "google", "ollama").
     */
    protected abstract String getConfigPrefix();

    /**
     * The default model name returned when no override is configured.
     */
    protected abstract String getDefaultModel();

    /**
     * Human-readable provider name used in error messages and logs.
     */
    protected abstract String getProviderName();

    /**
     * Whether the provider is fully configured and usable.
     */
    public abstract boolean isConfigured();

    /**
     * The currently configured model name.
     */
    @Nullable
    public String getModel() {
        return this.aiConfigService.getConfigValue(this.getConfigPrefix() + AiConfigKeys.SUFFIX_MODEL,
                this.getDefaultModel());
    }

    /**
     * Returns true if the API key is non-null, non-blank, and not an unresolved placeholder (e.g.
     * {@code ${OPENAI_API_KEY}}).
     */
    protected static boolean isApiKeyConfigured(@Nullable final String apiKey) {
        return apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("${");
    }

    /**
     * Throws {@link NonRetryableProviderException} when the API key is missing.
     *
     * @param apiKey
     *            the resolved API key value
     * @param envVarName
     *            the environment variable users should set
     */
    protected void requireApiKey(@Nullable final String apiKey, final String envVarName) {
        if (!isApiKeyConfigured(apiKey)) {
            throw new NonRetryableProviderException(this.getProviderName(),
                    "API key not configured. Please set " + envVarName + " environment variable");
        }
    }

    /**
     * Throws {@link NonRetryableProviderException} when the response content is empty.
     */
    protected String requireNonEmptyContent(@Nullable final String content) {
        if (content == null || content.isBlank()) {
            throw new NonRetryableProviderException(this.getProviderName(), "Empty response");
        }
        return content;
    }

    /**
     * Throws {@link NonRetryableProviderException} when a required dynamic configuration value is missing.
     */
    protected void requireConfigured(@Nullable final String value, final String settingDescription) {
        if (value == null || value.isBlank()) {
            throw new NonRetryableProviderException(this.getProviderName(),
                    settingDescription + " not configured. Please configure via admin settings.");
        }
    }
}
