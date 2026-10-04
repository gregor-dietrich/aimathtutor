package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.LaunchMode;

class SchemaManagementGuardTest {

    private static final Object EVENT = new Object();

    @ParameterizedTest(name = "strategy={0}")
    @ValueSource(strings = { "drop-and-create", "create", "drop", "update", "unknown" })
    @DisplayName("A production launch refuses a strategy other than none or validate")
    void productionLaunchRefusesStrategy(final String value) {
        final var guard = new SchemaManagementGuard(LaunchMode.NORMAL, value, null);
        assertThrows(IllegalStateException.class, () -> guard.checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "database.generation={0}")
    @ValueSource(strings = { "drop-and-create", "update" })
    @DisplayName("A production launch refuses the deprecated key even when the new key says validate")
    void productionLaunchRefusesLegacyKey(final String value) {
        final var guard = new SchemaManagementGuard(LaunchMode.NORMAL, "validate", value);
        assertThrows(IllegalStateException.class, () -> guard.checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "{0} / {1}")
    @CsvSource(value = { "validate, NULL", "none, NULL", "NULL, NULL", "VALIDATE, none", "' validate ', validate" },
            nullValues = "NULL")
    @DisplayName("A production launch accepts none, validate or an absent key")
    void productionLaunchAcceptsSafeStrategies(final String strategy, final String legacy) {
        final var guard = new SchemaManagementGuard(LaunchMode.NORMAL, strategy, legacy);
        assertDoesNotThrow(() -> guard.checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "DEVELOPMENT", "TEST" })
    @DisplayName("Dev and test launches may use drop-and-create")
    void devAndTestLaunchesAreNotRefused(final LaunchMode launchMode) {
        final var guard = new SchemaManagementGuard(launchMode, "drop-and-create", "drop-and-create");
        assertDoesNotThrow(() -> guard.checkStrategy(EVENT));
    }

    @Test
    @DisplayName("The guard observes container initialization, which runs before Hibernate, not StartupEvent")
    void guardRunsBeforeHibernate() throws NoSuchMethodException {
        StartupGuardAssertions.assertRunsBeforeHibernate(SchemaManagementGuard.class, "checkStrategy");
    }
}
