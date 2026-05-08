package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.microprofile.faulttolerance.Retry;

import de.vptr.aimathtutor.service.ai.NonRetryableAiProviderException;

/**
 * Verifies that a service method has the expected {@code @Retry} annotation with values matching {@link AppConstants}.
 */
public final class RetryAnnotationVerifier {

    private RetryAnnotationVerifier() {
    }

    /**
     * Verifies the retry annotation on the given method.
     *
     * @param serviceClass
     *            the service class
     * @param methodName
     *            the method name
     * @param paramTypes
     *            the method parameter types
     * @throws NoSuchMethodException
     *             if the method does not exist
     */
    public static void verifyRetryAnnotation(final Class<?> serviceClass, final String methodName,
            final Class<?>... paramTypes) throws NoSuchMethodException {
        final var method = serviceClass.getMethod(methodName, paramTypes);
        final Retry retry = method.getAnnotation(Retry.class);

        assertNotNull(retry, methodName + " should be annotated with @Retry");
        assertEquals(AppConstants.RETRY_MAX_RETRIES, retry.maxRetries());
        assertEquals(AppConstants.RETRY_DELAY_MS, retry.delay());
        assertEquals(AppConstants.RETRY_JITTER_MS, retry.jitter());
        assertEquals(1, retry.abortOn().length);
        assertSame(NonRetryableAiProviderException.class, retry.abortOn()[0], "Permanent failures must abort retry");
    }
}
