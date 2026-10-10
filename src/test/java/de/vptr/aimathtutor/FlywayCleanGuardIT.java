package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Pins that Quarkus discovers {@link FlywayCleanGuard} as a Flyway customizer; the unit test covers only its logic.
 */
@QuarkusTest
class FlywayCleanGuardIT {

    @Inject
    @Any
    Instance<FlywayConfigurationCustomizer> customizers;

    @Test
    void guardIsRegisteredAsFlywayCustomizer() {
        assertTrue(this.customizers.stream().anyMatch(FlywayCleanGuard.class::isInstance));
    }
}
