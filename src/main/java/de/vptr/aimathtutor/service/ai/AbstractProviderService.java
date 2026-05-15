package de.vptr.aimathtutor.service.ai;

import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

/**
 * Base class for AI provider services (OpenAI, Google, Ollama). Provides shared logic for config-driven model lookup,
 * API key validation, and empty-response checks.
 */
public abstract class AbstractProviderService {

    private static final Logger LOG = Logger.getLogger(AbstractProviderService.class);

    @Inject
    protected AiConfigService aiConfigService;

    @Nullable
    private volatile Client client;

    /**
     * Get or create the JAX-RS Client with thread-safe double-checked locking.
     */
    protected Client getClient() {
        Client localClient = this.client;
        if (localClient == null) {
            synchronized (this) {
                localClient = this.client;
                if (localClient == null) {
                    this.client = localClient =
                            ClientBuilder.newBuilder().connectTimeout(this.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                                    .readTimeout(this.getReadTimeoutSeconds(), TimeUnit.SECONDS).build();
                    LOG.debugf("Created %s JAX-RS Client (connectTimeout=%ss, readTimeout=%ss)", this.getProviderName(),
                            this.getConnectTimeoutSeconds(), this.getReadTimeoutSeconds());
                }
            }
        }
        return localClient;
    }

    /**
     * Set a custom JAX-RS client (primarily for testing).
     */
    protected void setClient(final Client client) {
        synchronized (this) {
            this.client = client;
        }
    }

    /**
     * Default connection timeout in seconds. Override if necessary.
     */
    protected int getConnectTimeoutSeconds() {
        return 10;
    }

    /**
     * Default read timeout in seconds. Override if necessary.
     */
    protected int getReadTimeoutSeconds() {
        return 60;
    }

    /**
     * Clean up JAX-RS client resources when the bean is destroyed.
     */
    @PreDestroy
    public void cleanup() {
        final Client localClient;
        synchronized (this) {
            localClient = this.client;
            this.client = null;
        }
        if (localClient != null) {
            localClient.close();
            LOG.debugf("Closed %s JAX-RS Client", this.getProviderName());
        }
    }

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
