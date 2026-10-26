package de.vptr.aimathtutor.service.ai;

import static de.vptr.aimathtutor.service.ai.AiConfigTestSupport.defaults;
import static de.vptr.aimathtutor.service.ai.AiConfigTestSupport.setStoredValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    @BeforeEach
    @AfterEach
    void clearCache() {
        this.aiConfigService.clearCache();
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
        setStoredValue(this.aiConfigRepository, AiConfigKeys.OLLAMA_MODEL, "stored-model");
        this.entityManager.flush();
        assertEquals("stored-model", this.aiConfigService.getEffectiveConfig().get(AiConfigKeys.OLLAMA_MODEL));

        this.entityManager.remove(this.aiConfigRepository.findByConfigKey(AiConfigKeys.OLLAMA_MODEL).orElseThrow());
        this.entityManager.flush();

        assertTrue(this.aiConfigRepository.findByConfigKey(AiConfigKeys.OLLAMA_MODEL).isEmpty());
        assertEquals(defaults().get(AiConfigKeys.OLLAMA_MODEL),
                this.aiConfigService.getEffectiveConfig().get(AiConfigKeys.OLLAMA_MODEL));
    }

    @Test
    @DisplayName("A stored value is returned, a malformed or NaN temperature or token limit becomes its default")
    @TestTransaction
    void storedValueIsReturnedAndMalformedNumbersFallBack() {
        setStoredValue(this.aiConfigRepository, AiConfigKeys.OPENAI_MODEL, "stored-model");
        setStoredValue(this.aiConfigRepository, AiConfigKeys.GOOGLE_TEMPERATURE, "warm");
        setStoredValue(this.aiConfigRepository, AiConfigKeys.GOOGLE_MAX_TOKENS, "many");
        setStoredValue(this.aiConfigRepository, AiConfigKeys.OPENAI_TEMPERATURE, "NaN");
        this.entityManager.flush();

        final var config = this.aiConfigService.getEffectiveConfig();

        assertEquals("stored-model", config.get(AiConfigKeys.OPENAI_MODEL));
        assertEquals(defaults().get(AiConfigKeys.GOOGLE_TEMPERATURE), config.get(AiConfigKeys.GOOGLE_TEMPERATURE));
        assertEquals(defaults().get(AiConfigKeys.GOOGLE_MAX_TOKENS), config.get(AiConfigKeys.GOOGLE_MAX_TOKENS));
        assertEquals(defaults().get(AiConfigKeys.OPENAI_TEMPERATURE), config.get(AiConfigKeys.OPENAI_TEMPERATURE));
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
