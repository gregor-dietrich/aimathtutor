package de.vptr.aimathtutor;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import java.util.Locale;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Refuses a production launch whose Hibernate schema management would change the schema. A production deployment can
 * override {@code validate} with a single environment variable, such as
 * {@code QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=drop-and-create}, which drops and recreates every table under
 * the {@code prod} profile, out of {@link ProductionProfileGuard}'s reach. The deprecated
 * {@code quarkus.hibernate-orm.database.generation} key does the same and is not visible through the new key, so both
 * are checked. Runs at the same point as {@link ProductionProfileGuard}, before Hibernate starts; see there for why it
 * is not a {@code StartupEvent} observer.
 */
@ApplicationScoped
public class SchemaManagementGuard {

    private static final Logger LOG = Logger.getLogger(SchemaManagementGuard.class);

    static final String STRATEGY_KEY = "quarkus.hibernate-orm.schema-management.strategy";

    static final String LEGACY_KEY = "quarkus.hibernate-orm.database.generation";

    private static final Set<String> ALLOWED = Set.of("none", "validate");

    private final LaunchMode launchMode;

    @Nullable
    private final String strategy;

    @Nullable
    private final String legacyStrategy;

    @Inject
    SchemaManagementGuard(final LaunchMode launchMode, final SmallRyeConfig config) {
        this(launchMode, config.getOptionalValue(STRATEGY_KEY, String.class).orElse(null),
                config.getOptionalValue(LEGACY_KEY, String.class).orElse(null));
    }

    SchemaManagementGuard(final LaunchMode launchMode, @Nullable final String strategy,
            @Nullable final String legacyStrategy) {
        this.launchMode = launchMode;
        this.strategy = strategy;
        this.legacyStrategy = legacyStrategy;
    }

    void checkStrategy(@Observes @Initialized(ApplicationScoped.class) @Priority(PLATFORM_BEFORE) final Object event) {
        if (this.launchMode == LaunchMode.NORMAL && !(isAllowed(this.strategy) && isAllowed(this.legacyStrategy))) {
            LOG.fatalf("A production build is running with Hibernate schema management %s=%s, %s=%s, which would "
                    + "change or drop the database tables. Nothing was changed. Only none or validate is allowed; "
                    + "remove the override (e.g. QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY) from the "
                    + "configuration.", STRATEGY_KEY, this.strategy, LEGACY_KEY, this.legacyStrategy);
            throw new IllegalStateException(
                    "Production build started with schema management " + this.strategy + " / " + this.legacyStrategy);
        }
    }

    private static boolean isAllowed(@Nullable final String value) {
        return value == null || ALLOWED.contains(value.trim().toLowerCase(Locale.ROOT));
    }
}
