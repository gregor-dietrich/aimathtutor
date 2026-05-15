package de.vptr.aimathtutor.service.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.quarkus.test.InjectMock;

/**
 * Base class for AI service tests to share common setup.
 */
@SuppressWarnings("NullAway")
public abstract class AbstractAiServiceTest {

    @InjectMock
    protected AiConfigService aiConfigService;

    /**
     * Mocks common configuration values used by AI services.
     */
    protected void mockCommonConfig() {
        when(this.aiConfigService.getConfigValue(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(this.aiConfigService.getClampedTemperature(anyString(), any(Double.class))).thenReturn(0.7);
        when(this.aiConfigService.getClampedTokens(anyString(), any(Integer.class))).thenReturn(2000);
    }
}
