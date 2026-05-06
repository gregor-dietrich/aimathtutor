package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.service.GeminiService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AbstractAiProviderServiceTest {

    @Inject
    GeminiService geminiService;

    // Minimal concrete subclass to test protected instance methods directly.
    // Not a CDI bean — aiConfigService will be null, but none of the tested
    // methods use it.
    private static final class TestProvider extends AbstractAiProviderService {
        @Override
        protected String getConfigPrefix() {
            return "test";
        }

        @Override
        protected String getDefaultModel() {
            return "test-model";
        }

        @Override
        protected String getProviderName() {
            return "Test Provider";
        }

        @Override
        public boolean isConfigured() {
            return false;
        }

        public void callRequireApiKey(final String key, final String envVar) {
            this.requireApiKey(key, envVar);
        }

        public String callRequireNonEmptyContent(final String content) {
            return this.requireNonEmptyContent(content);
        }

        public void callRequireConfigured(final String value, final String desc) {
            this.requireConfigured(value, desc);
        }
    }

    private static final TestProvider PROVIDER = new TestProvider();

    @Test
    @DisplayName("isApiKeyConfigured returns false for null")
    void testIsApiKeyConfigured_null() {
        assertFalse(AbstractAiProviderService.isApiKeyConfigured(null));
    }

    @Test
    @DisplayName("isApiKeyConfigured returns false for blank string")
    void testIsApiKeyConfigured_blank() {
        assertFalse(AbstractAiProviderService.isApiKeyConfigured("   "));
    }

    @Test
    @DisplayName("isApiKeyConfigured returns false for unresolved placeholder")
    void testIsApiKeyConfigured_placeholder() {
        assertFalse(AbstractAiProviderService.isApiKeyConfigured("${GEMINI_API_KEY}"));
    }

    @Test
    @DisplayName("isApiKeyConfigured returns true for valid non-placeholder key")
    void testIsApiKeyConfigured_validKey() {
        assertTrue(AbstractAiProviderService.isApiKeyConfigured("sk-abc123"));
    }

    @Test
    @DisplayName("requireApiKey throws NonRetryableAiProviderException for null key")
    void testRequireApiKey_throwsWhenKeyNull() {
        assertThrows(NonRetryableAiProviderException.class,
                () -> PROVIDER.callRequireApiKey(null, "TEST_API_KEY"));
    }

    @Test
    @DisplayName("requireApiKey throws NonRetryableAiProviderException for placeholder key")
    void testRequireApiKey_throwsWhenKeyPlaceholder() {
        assertThrows(NonRetryableAiProviderException.class,
                () -> PROVIDER.callRequireApiKey("${TEST_KEY}", "TEST_API_KEY"));
    }

    @Test
    @DisplayName("requireNonEmptyContent throws NonRetryableAiProviderException for null content")
    void testRequireNonEmptyContent_throwsWhenNull() {
        assertThrows(NonRetryableAiProviderException.class,
                () -> PROVIDER.callRequireNonEmptyContent(null));
    }

    @Test
    @DisplayName("requireNonEmptyContent throws NonRetryableAiProviderException for blank content")
    void testRequireNonEmptyContent_throwsWhenBlank() {
        assertThrows(NonRetryableAiProviderException.class,
                () -> PROVIDER.callRequireNonEmptyContent("   "));
    }

    @Test
    @DisplayName("requireNonEmptyContent returns content unchanged when non-empty")
    void testRequireNonEmptyContent_returnsWhenValid() {
        final String content = PROVIDER.callRequireNonEmptyContent("valid content");
        assertNotNull(content);
        assertTrue(content.contains("valid"));
    }

    @Test
    @DisplayName("requireConfigured throws NonRetryableAiProviderException for null value")
    void testRequireConfigured_throwsWhenNull() {
        assertThrows(NonRetryableAiProviderException.class,
                () -> PROVIDER.callRequireConfigured(null, "model setting"));
    }

    @Test
    @DisplayName("requireConfigured throws NonRetryableAiProviderException for blank value")
    void testRequireConfigured_throwsWhenBlank() {
        assertThrows(NonRetryableAiProviderException.class,
                () -> PROVIDER.callRequireConfigured("", "model setting"));
    }

    @Test
    @DisplayName("getModel returns non-null string via GeminiService")
    void testGetModel_returnsNonNull() {
        assertNotNull(this.geminiService.getModel());
    }

    @Test
    @DisplayName("isConfigured is callable and returns a boolean")
    void testIsConfigured_returnsBoolean() {
        assertDoesNotThrow(() -> this.geminiService.isConfigured(),
                "isConfigured must be callable without throwing");
    }
}
