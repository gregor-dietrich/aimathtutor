package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.repository.AiConfigRepository;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/** The settings snapshot the AI settings page loads: stored values, defaults for what is missing or malformed. */
@QuarkusTest
@DisplayName("AiConfigService effective config")
class AiConfigEffectiveConfigTest {

    @Inject
    AiConfigService aiConfigService;

    @InjectSpy
    AiConfigRepository aiConfigRepository;

    @Inject
    EntityManager entityManager;

    private static Map<String, String> defaults() {
        return AiConfigService.defaultUpdates().stream()
                .collect(Collectors.toMap(update -> update.configKey, update -> update.configValue));
    }

    @BeforeEach
    @AfterEach
    void clearCache() {
        this.aiConfigService.clearCache();
    }

    private void setStoredValue(final String key, final String value) {
        final var entity = this.aiConfigRepository.findByConfigKey(key).orElseThrow();
        entity.configValue = value;
        this.aiConfigRepository.update(entity);
    }

    @Test
    @DisplayName("The map holds exactly the keys of the built-in defaults")
    void keysAreTheDefaultKeys() {
        assertEquals(defaults().keySet(), this.aiConfigService.getEffectiveConfig().keySet());
    }

    @Test
    @DisplayName("A row missing from ai_config yields its default")
    @TestTransaction
    void missingRowYieldsDefault() {
        this.aiConfigRepository.findByConfigKey(AiConfigKeys.OLLAMA_MODEL).ifPresent(this.entityManager::remove);
        this.entityManager.flush();

        final var config = this.aiConfigService.getEffectiveConfig();

        assertEquals(defaults().get(AiConfigKeys.OLLAMA_MODEL), config.get(AiConfigKeys.OLLAMA_MODEL));
    }

    @Test
    @DisplayName("A stored value is returned, a malformed temperature or token limit becomes its default")
    @TestTransaction
    void storedValueIsReturnedAndMalformedNumbersFallBack() {
        this.setStoredValue(AiConfigKeys.OPENAI_MODEL, "stored-model");
        this.setStoredValue(AiConfigKeys.GOOGLE_TEMPERATURE, "warm");
        this.setStoredValue(AiConfigKeys.GOOGLE_MAX_TOKENS, "many");
        this.entityManager.flush();

        final var config = this.aiConfigService.getEffectiveConfig();

        assertEquals("stored-model", config.get(AiConfigKeys.OPENAI_MODEL));
        assertEquals(defaults().get(AiConfigKeys.GOOGLE_TEMPERATURE), config.get(AiConfigKeys.GOOGLE_TEMPERATURE));
        assertEquals(defaults().get(AiConfigKeys.GOOGLE_MAX_TOKENS), config.get(AiConfigKeys.GOOGLE_MAX_TOKENS));
    }

    @Test
    @DisplayName("Without a transaction the first call fills the cache, so a second reads no row")
    void secondCallReadsNoRow() {
        final var first = this.aiConfigService.getEffectiveConfig();
        clearInvocations(this.aiConfigRepository);

        final var second = this.aiConfigService.getEffectiveConfig();

        assertEquals(first, second);
        verify(this.aiConfigRepository, never()).findByConfigKey(anyString());
    }
}
