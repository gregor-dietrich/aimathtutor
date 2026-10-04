package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;

/**
 * Pins the timing contract of the startup guards: they must observe container initialization, which Quarkus fires
 * during static init, before Hibernate's schema management, and not {@code StartupEvent}, which fires after it.
 */
final class StartupGuardAssertions {

    private StartupGuardAssertions() {
    }

    static void assertRunsBeforeHibernate(final Class<?> guard, final String observerMethod)
            throws NoSuchMethodException {
        final var parameter = guard.getDeclaredMethod(observerMethod, Object.class).getParameters()[0];
        assertNotNull(parameter.getAnnotation(Observes.class));
        final var initialized = parameter.getAnnotation(Initialized.class);
        assertNotNull(initialized);
        assertEquals(ApplicationScoped.class, initialized.value());
        final var priority = parameter.getAnnotation(Priority.class);
        assertNotNull(priority);
        assertEquals(Interceptor.Priority.PLATFORM_BEFORE, priority.value());
    }
}
