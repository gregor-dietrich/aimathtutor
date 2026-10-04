package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.EnvConfigSource;
import io.smallrye.config.SmallRyeConfigBuilder;
import jakarta.interceptor.Interceptor;

class SchemaManagementGuardTest {

    private static final Object EVENT = new Object();

    private static SchemaManagementGuard guard(final LaunchMode launchMode, final Map<String, String> settings) {
        return new SchemaManagementGuard(launchMode, key -> Optional.ofNullable(settings.get(key)));
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({ "schema-management.strategy, drop-and-create", "schema-management.strategy, create",
            "schema-management.strategy, drop", "schema-management.strategy, update",
            "schema-management.strategy, create-drop", "schema-management.strategy, unknown",
            "schema-management.strategy, DROP-AND-CREATE", "database.generation, drop-and-create",
            "'\"<default>\".schema-management.strategy', drop-and-create",
            "'\"<default>\".database.generation', drop-and-create" })
    @DisplayName("A production launch refuses any schema action but none or validate, under every key")
    void productionLaunchRefusesSchemaAction(final String key, final String value) {
        final var refused = guard(LaunchMode.NORMAL, Map.of("quarkus.hibernate-orm." + key, value));
        assertThrows(IllegalStateException.class, () -> refused.checkStrategy(EVENT));
    }

    @Test
    @DisplayName("A production launch refuses Jakarta's database action passed through unsupported-properties")
    void productionLaunchRefusesUnsupportedDatabaseAction() {
        final var refused = guard(LaunchMode.NORMAL, Map.of("quarkus.hibernate-orm.unsupported-properties.\""
                + AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION + "\"", "drop-and-create"));
        assertThrows(IllegalStateException.class, () -> refused.checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "none", "validate", "VALIDATE", " validate " })
    @DisplayName("A production launch accepts none or validate")
    void productionLaunchAcceptsSafeStrategies(final String value) {
        final var accepted = guard(LaunchMode.NORMAL, Map.of("quarkus.hibernate-orm.schema-management.strategy", value,
                "quarkus.hibernate-orm.database.generation", value));
        assertDoesNotThrow(() -> accepted.checkStrategy(EVENT));
    }

    @Test
    @DisplayName("A production launch with no schema-management setting starts")
    void productionLaunchAcceptsNoSetting() {
        assertDoesNotThrow(() -> guard(LaunchMode.NORMAL, Map.of()).checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "DEVELOPMENT", "TEST" })
    @DisplayName("Dev and test launches may use drop-and-create")
    void devAndTestLaunchesAreNotRefused(final LaunchMode launchMode) {
        final var allowed = guard(launchMode, Map.of("quarkus.hibernate-orm.schema-management.strategy",
                "drop-and-create", "quarkus.hibernate-orm.database.generation", "drop-and-create"));
        assertDoesNotThrow(() -> allowed.checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { "QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY",
            "QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION", "QUARKUS_HIBERNATE_ORM___DEFAULT___SCHEMA_MANAGEMENT_STRATEGY",
            "QUARKUS_HIBERNATE_ORM___DEFAULT___DATABASE_GENERATION" })
    @DisplayName("The injected guard reads each setting from its environment variable")
    void injectedGuardReadsEnvironmentVariables(final String variable) {
        final var config = new SmallRyeConfigBuilder()
                .withSources(new EnvConfigSource(Map.of(variable, "drop-and-create"), 300)).build();
        final var injected = new SchemaManagementGuard(LaunchMode.NORMAL, config);
        assertThrows(IllegalStateException.class, () -> injected.checkStrategy(EVENT));
    }

    @Test
    @DisplayName("The guard observes container initialization after the profile guard, not StartupEvent")
    void guardRunsBeforeHibernate() throws NoSuchMethodException {
        StartupGuardAssertions.assertRunsBeforeHibernate(SchemaManagementGuard.class, "checkStrategy",
                Interceptor.Priority.PLATFORM_BEFORE + 1);
    }
}
