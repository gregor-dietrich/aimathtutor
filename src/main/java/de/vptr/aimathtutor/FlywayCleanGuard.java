package de.vptr.aimathtutor;

import org.flywaydb.core.api.configuration.FluentConfiguration;

import io.quarkus.flyway.FlywayConfigurationCustomizer;
import io.quarkus.runtime.LaunchMode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Disables Flyway's clean on a production launch. The {@code %dev,test} profile enables clean-at-start, and profiles
 * are chosen at runtime: a production jar started with {@code QUARKUS_PROFILE=dev} would otherwise wipe the production
 * schema before any startup check runs. With clean disabled, that start fails instead. An unqualified customizer
 * applies to the default datasource only; a named datasource would need its own {@code @FlywayDataSource} one.
 */
@Singleton
public class FlywayCleanGuard implements FlywayConfigurationCustomizer {

    private final LaunchMode launchMode;

    @Inject
    FlywayCleanGuard(final LaunchMode launchMode) {
        this.launchMode = launchMode;
    }

    @Override
    public void customize(final FluentConfiguration configuration) {
        if (this.launchMode == LaunchMode.NORMAL) {
            configuration.cleanDisabled(true);
        }
    }
}
