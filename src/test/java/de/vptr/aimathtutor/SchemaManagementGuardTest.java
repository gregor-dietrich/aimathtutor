package de.vptr.aimathtutor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest(name = "{0}")
    @ValueSource(
            strings = { "drop-and-create", "create", "drop", "update", "create-drop", "unknown", "DROP-AND-CREATE" })
    @DisplayName("A production launch refuses any schema action but none or validate")
    void productionLaunchRefusesSchemaAction(final String value) {
        final var refused = guard(LaunchMode.NORMAL, Map.of("quarkus.hibernate-orm.schema-management.strategy", value));
        assertThrows(IllegalStateException.class, () -> refused.checkStrategy(EVENT));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("everyKey")
    @DisplayName("A production launch refuses drop-and-create under every name Hibernate takes the schema action from")
    void productionLaunchRefusesEveryKey(final String key) {
        final var refused = guard(LaunchMode.NORMAL, Map.of(key, "drop-and-create"));
        assertThrows(IllegalStateException.class, () -> refused.checkStrategy(EVENT));
    }

    /** Spelled out rather than read from the guard, so a key dropped from the guard fails here. */
    static Stream<String> everyKey() {
        final var settings = List.of("schema-management.strategy", "database.generation",
                "unsupported-properties.\"" + AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION + "\"",
                "unsupported-properties.\"" + AvailableSettings.HBM2DDL_AUTO + "\"");
        return Stream
                .of("quarkus.hibernate-orm.", "quarkus.hibernate-orm.\"<default>\".",
                        "quarkus.hibernate-orm.<default>.")
                .flatMap(prefix -> settings.stream().map(setting -> prefix + setting));
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
