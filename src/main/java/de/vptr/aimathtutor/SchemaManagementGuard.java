package de.vptr.aimathtutor;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.cfg.AvailableSettings;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Refuses a production launch whose Hibernate schema management is set to anything but {@code none} or
 * {@code validate}. A production deployment can override {@code validate} with a single setting, such as
 * {@code QUARKUS_HIBERNATE_ORM_SCHEMA_MANAGEMENT_STRATEGY=drop-and-create}, which drops and recreates every table under
 * the {@code prod} profile, out of {@link ProductionProfileGuard}'s reach. The same setting has several names that
 * Hibernate honours and that don't show through each other, so each one is checked: the deprecated
 * {@code database.generation}, the explicit {@code "<default>"} persistence-unit form of both, and Jakarta's
 * {@code schema-generation.database.action} passed through {@code unsupported-properties}. Their environment-variable
 * forms resolve to the same keys.
 * <p>
 * Runs at the same point as {@link ProductionProfileGuard}, before Hibernate starts; see there for why it is not a
 * {@code StartupEvent} observer. Its priority is one after that guard's, so a dev/test profile, which also sets
 * drop-and-create, is reported as the profile problem it is.
 */
@ApplicationScoped
public class SchemaManagementGuard {

    private static final Logger LOG = Logger.getLogger(SchemaManagementGuard.class);

    private static final String PREFIX = "quarkus.hibernate-orm.";

    private static final String DEFAULT_UNIT = "quarkus.hibernate-orm.\"<default>\".";

    private static final String DATABASE_ACTION =
            "unsupported-properties.\"" + AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION + "\"";

    private static final String HBM2DDL_AUTO = "unsupported-properties.\"" + AvailableSettings.HBM2DDL_AUTO + "\"";

    /** Every key that sets Hibernate's schema action for the default persistence unit. */
    static final List<String> KEYS =
            List.of("schema-management.strategy", "database.generation", DATABASE_ACTION, HBM2DDL_AUTO).stream()
                    .flatMap(key -> List.of(PREFIX + key, DEFAULT_UNIT + key).stream()).toList();

    private static final Set<String> ALLOWED_STRATEGIES = Set.of("none", "validate");

    private final LaunchMode launchMode;

    private final Function<String, Optional<String>> lookup;

    @Inject
    SchemaManagementGuard(final LaunchMode launchMode, final SmallRyeConfig config) {
        this(launchMode, key -> config.getOptionalValue(key, String.class));
    }

    SchemaManagementGuard(final LaunchMode launchMode, final Function<String, Optional<String>> lookup) {
        this.launchMode = launchMode;
        this.lookup = lookup;
    }

    void checkStrategy(
            @Observes @Initialized(ApplicationScoped.class) @Priority(PLATFORM_BEFORE + 1) final Object event) {
        if (this.launchMode != LaunchMode.NORMAL) {
            return;
        }
        final Map<String,
                String> refused = KEYS.stream()
                        .flatMap(key -> this.lookup.apply(key).filter(value -> !isAllowed(value))
                                .map(value -> Map.entry(key, value)).stream())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (!refused.isEmpty()) {
            LOG.fatalf("A production build is running with Hibernate schema management %s; only none or validate is "
                    + "allowed, anything else can change or drop the database tables. Nothing was changed. Remove "
                    + "these settings (or their QUARKUS_HIBERNATE_ORM_... environment variables) from the "
                    + "configuration.", refused);
            throw new IllegalStateException("Production build started with schema management " + refused);
        }
    }

    private static boolean isAllowed(final String value) {
        return ALLOWED_STRATEGIES.contains(value.trim().toLowerCase(Locale.ROOT));
    }
}
