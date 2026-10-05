package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;

/**
 * Pins the timing contract of the startup guards: they must observe container initialization, which Quarkus fires
 * during static init, before Hibernate's schema management, and not {@code StartupEvent}, which fires after it.
 */
final class StartupGuardAssertions {

    private StartupGuardAssertions() {
    }

    static void assertRunsBeforeHibernate(final Class<?> guard, final String observerMethod, final int priority)
            throws NoSuchMethodException {
        final var parameter = guard.getDeclaredMethod(observerMethod, Object.class).getParameters()[0];
        assertNotNull(parameter.getAnnotation(Observes.class), "the guard must be an observer");
        final var initialized = parameter.getAnnotation(Initialized.class);
        assertNotNull(initialized, "the guard must observe @Initialized, not StartupEvent");
        assertEquals(ApplicationScoped.class, initialized.value());
        final var observerPriority = parameter.getAnnotation(Priority.class);
        assertNotNull(observerPriority, "the guard must set its @Priority");
        assertEquals(priority, observerPriority.value());
    }
}
