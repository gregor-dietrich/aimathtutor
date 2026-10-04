package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.runtime.LaunchMode;

class FlywayCleanGuardTest {

    @ParameterizedTest
    @CsvSource({ "NORMAL, true", "DEVELOPMENT, false", "TEST, false" })
    void cleanIsDisabledOnlyForProductionLaunch(final LaunchMode launchMode, final boolean cleanDisabled) {
        final var configuration = new FluentConfiguration().cleanDisabled(false);
        new FlywayCleanGuard(launchMode).customize(configuration);
        assertEquals(cleanDisabled, configuration.isCleanDisabled());
    }
}
