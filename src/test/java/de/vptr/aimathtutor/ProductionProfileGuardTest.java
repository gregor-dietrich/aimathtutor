package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.LaunchMode;

class ProductionProfileGuardTest {

    private static final Object EVENT = new Object();

    @ParameterizedTest
    @ValueSource(strings = { "dev", "test" })
    void productionLaunchRefusesNonProductionProfile(final String profile) {
        final var guard = new ProductionProfileGuard(LaunchMode.NORMAL, List.of("staging", profile));
        assertThrows(IllegalStateException.class, () -> guard.refuse(EVENT));
    }

    @Test
    void productionLaunchAcceptsProductionProfile() {
        final var guard = new ProductionProfileGuard(LaunchMode.NORMAL, List.of("prod"));
        assertDoesNotThrow(() -> guard.refuse(EVENT));
    }

    @ParameterizedTest
    @CsvSource({ "DEVELOPMENT, dev", "TEST, test" })
    void devAndTestLaunchesKeepTheirProfile(final LaunchMode launchMode, final String profile) {
        final var guard = new ProductionProfileGuard(launchMode, List.of(profile));
        assertDoesNotThrow(() -> guard.refuse(EVENT));
    }
}
