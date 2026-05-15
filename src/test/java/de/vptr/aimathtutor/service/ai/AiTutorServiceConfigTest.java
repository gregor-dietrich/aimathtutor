package de.vptr.aimathtutor.service.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.AiFeedbackDto;
import de.vptr.aimathtutor.dto.ConversationContextDto;
import de.vptr.aimathtutor.dto.GraspableEventDto;
import de.vptr.aimathtutor.exception.NonRetryableProviderException;
import de.vptr.aimathtutor.service.ai.provider.GoogleProvider;
import de.vptr.aimathtutor.service.ai.provider.OllamaProvider;
import de.vptr.aimathtutor.service.ai.provider.OpenAiProvider;
import de.vptr.aimathtutor.service.security.RateLimitService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class AiTutorServiceConfigTest {

    @Inject
    AiTutorService aiTutorService;

    @Nullable
    @InjectMock
    AiConfigService aiConfigService;

    @Nullable
    @InjectMock
    GoogleProvider googleAiProvider;

    @Nullable
    @InjectMock
    OpenAiProvider openAiProvider;

    @Nullable
    @InjectMock
    OllamaProvider ollamaAiProvider;

    @Nullable
    @InjectMock
    RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        when(this.aiConfigService.getConfigValueAsBoolean(AiConfigKeys.AI_TUTOR_ENABLED, true)).thenReturn(true);
        when(this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_PROVIDER, "mock")).thenReturn("mock");
        when(this.rateLimitService.tryConsume(any())).thenReturn(true);
    }

    private GraspableEventDto buildSignificantEvent() {
        final var event = new GraspableEventDto();
        event.eventType = "simplify";
        event.expressionBefore = "2x + 3x";
        event.expressionAfter = "5x";
        event.correct = true;
        return event;
    }

    private void configureProvider(final String provider, final boolean rateLimitAllows) {
        when(this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_PROVIDER, "mock")).thenReturn(provider);
        when(this.rateLimitService.tryConsume(any())).thenReturn(rateLimitAllows);
    }

    @Test
    @DisplayName("analyzeMathAction returns null when AI is disabled")
    void analyzeMathAction_returnsNullWhenAiDisabled() {
        when(this.aiConfigService.getConfigValueAsBoolean(AiConfigKeys.AI_TUTOR_ENABLED, true)).thenReturn(false);
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNull(result);
    }

    @Test
    @DisplayName("answerQuestion returns offline message when AI is disabled")
    void answerQuestion_returnsOfflineMessageWhenAiDisabled() {
        when(this.aiConfigService.getConfigValueAsBoolean(AiConfigKeys.AI_TUTOR_ENABLED, true)).thenReturn(false);
        final var result = this.aiTutorService.answerQuestion("What is x?", null, null, null, "sid",
                new ConversationContextDto(), "u1");
        assertNotNull(result);
        assertNotNull(result.message);
    }

    @Test
    @DisplayName("analyzeMathAction returns rate limit hint when effective user ID is null")
    void analyzeMathAction_returnsRateLimitHintWhenUserIdNull() {
        configureProvider("google", true);
        final var event = this.buildSignificantEvent();
        // userIdStr=null and event.studentId=null → effectiveUserId=null → checkAiRateLimit(null)=false
        final var result = this.aiTutorService.analyzeMathAction(event, new ConversationContextDto(), null);
        assertNotNull(result);
        assertTrue(result.message.contains("requests") || result.message.contains("wait"));
    }

    @Test
    @DisplayName("answerQuestion returns rate limit message when rate limit exceeded")
    void answerQuestion_returnsRateLimitMessageWhenExceeded() {
        configureProvider("google", false);
        final var result = this.aiTutorService.answerQuestion("What is x?", null, null, null, "sid",
                new ConversationContextDto(), null);
        assertNotNull(result);
        assertTrue(result.message.contains("requests") || result.message.contains("wait"));
    }

    @Test
    @DisplayName("analyzeMathAction delegates to Google provider when configured and available")
    void analyzeMathAction_usesGoogleProvider() {
        configureProvider("google", true);
        when(this.googleAiProvider.isAvailable()).thenReturn(true);
        when(this.googleAiProvider.analyzeMathAction(any(), any())).thenReturn(AiFeedbackDto.positive("Google ok"));
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNotNull(result);
    }

    @Test
    @DisplayName("analyzeMathAction delegates to OpenAI provider when configured and available")
    void analyzeMathAction_usesOpenAiProvider() {
        configureProvider("openai", true);
        when(this.openAiProvider.isAvailable()).thenReturn(true);
        when(this.openAiProvider.analyzeMathAction(any(), any())).thenReturn(AiFeedbackDto.positive("OpenAI ok"));
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNotNull(result);
    }

    @Test
    @DisplayName("analyzeMathAction delegates to Ollama provider when configured and available")
    void analyzeMathAction_usesOllamaProvider() {
        configureProvider("ollama", true);
        when(this.ollamaAiProvider.isAvailable()).thenReturn(true);
        when(this.ollamaAiProvider.analyzeMathAction(any(), any())).thenReturn(AiFeedbackDto.positive("Ollama ok"));
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNotNull(result);
    }

    @Test
    @DisplayName("analyzeMathAction falls back to mock when Google provider is unavailable")
    void analyzeMathAction_fallsBackToMockWhenProviderUnavailable() {
        configureProvider("google", true);
        when(this.googleAiProvider.isAvailable()).thenReturn(false);
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNotNull(result);
    }

    @Test
    @DisplayName("analyzeMathAction rethrows NonRetryableProviderException from provider")
    void analyzeMathAction_rethrowsNonRetryableException() {
        configureProvider("google", true);
        when(this.googleAiProvider.isAvailable()).thenReturn(true);
        when(this.googleAiProvider.analyzeMathAction(any(), any()))
                .thenThrow(new NonRetryableProviderException("google", "quota exceeded"));
        assertThrows(NonRetryableProviderException.class, () -> this.aiTutorService
                .analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1"));
    }

    @Test
    @DisplayName("analyzeMathAction falls back to mock on transient RuntimeException from provider")
    void analyzeMathAction_fallsBackToMockOnRuntimeException() {
        configureProvider("google", true);
        when(this.googleAiProvider.isAvailable()).thenReturn(true);
        when(this.googleAiProvider.analyzeMathAction(any(), any()))
                .thenThrow(new RuntimeException("transient failure"));
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNotNull(result);
    }

    @Test
    @DisplayName("analyzeMathAction returns null for autosimp (insignificant) action")
    void analyzeMathAction_returnsNullForAutosimpAction() {
        final var event = new GraspableEventDto();
        event.eventType = "autosimp";
        event.expressionBefore = "x + 0";
        event.expressionAfter = "x";
        assertNull(this.aiTutorService.analyzeMathAction(event, new ConversationContextDto(), "u1"));
    }

    @Test
    @DisplayName("analyzeMathAction returns non-null feedback for simplify (significant) action via mock provider")
    void analyzeMathAction_returnsNonNullForSignificantAction() {
        final var result =
                this.aiTutorService.analyzeMathAction(this.buildSignificantEvent(), new ConversationContextDto(), "u1");
        assertNotNull(result);
    }
}
