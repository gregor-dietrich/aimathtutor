package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.LaunchMode;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.interceptor.Interceptor;

class ProductionProfileGuardTest {

    private static final Object EVENT = new Object();

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "dev", "test" })
    @DisplayName("A production launch with a dev or test profile, alone or as a parent, is refused")
    void productionLaunchRefusesNonProductionProfile(final String profile) {
        final var guard = new ProductionProfileGuard(LaunchMode.NORMAL, List.of("staging", profile));
        assertThrows(IllegalStateException.class, () -> guard.checkProfiles(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "prod", "staging", "devops", "testing", "Dev" })
    @DisplayName("A production launch with another profile starts; profile names match exactly")
    void productionLaunchAcceptsOtherProfiles(final String profile) {
        final var guard = new ProductionProfileGuard(LaunchMode.NORMAL, List.of(profile));
        assertDoesNotThrow(() -> guard.checkProfiles(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({ "DEVELOPMENT, dev", "TEST, test" })
    @DisplayName("Dev and test launches are not refused")
    void devAndTestLaunchesAreNotRefused(final LaunchMode launchMode, final String profile) {
        final var guard = new ProductionProfileGuard(launchMode, List.of(profile));
        assertDoesNotThrow(() -> guard.checkProfiles(EVENT));
    }

    @Test
    @DisplayName("The guard observes container initialization, which runs before Hibernate, not StartupEvent")
    void guardRunsBeforeHibernate() throws NoSuchMethodException {
        final var parameter =
                ProductionProfileGuard.class.getDeclaredMethod("checkProfiles", Object.class).getParameters()[0];
        assertNotNull(parameter.getAnnotation(Observes.class));
        final var initialized = parameter.getAnnotation(Initialized.class);
        assertNotNull(initialized);
        assertEquals(ApplicationScoped.class, initialized.value());
        final var priority = parameter.getAnnotation(Priority.class);
        assertNotNull(priority);
        assertEquals(Interceptor.Priority.PLATFORM_BEFORE, priority.value());
    }
}
