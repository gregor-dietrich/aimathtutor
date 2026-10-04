package de.vptr.aimathtutor;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

import java.util.List;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.smallrye.config.SmallRyeConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Refuses a production launch with the dev or test profile. Profiles are chosen at runtime, so a production jar started
 * with {@code QUARKUS_PROFILE=dev} would apply {@code %dev,test}'s drop-and-create and drop and recreate every table
 * Hibernate maps. Hibernate runs that DDL before {@code StartupEvent}, so this observes the container's initialization
 * instead, which Quarkus fires during static init, before Hibernate starts.
 * <p>
 * Every build logs a Quarkus warning that recommends {@code StartupEvent} for this observer. Do not follow it: a
 * {@code StartupEvent} observer runs after the tables are gone. In a native image, static init runs at image build
 * time, so this guard would not run at startup; the application ships as a JVM image only.
 */
@ApplicationScoped
public class ProductionProfileGuard {

    private static final Logger LOG = Logger.getLogger(ProductionProfileGuard.class);

    private static final List<String> NON_PRODUCTION_PROFILES = List.of("dev", "test");

    private final LaunchMode launchMode;

    private final List<String> profiles;

    @Inject
    ProductionProfileGuard(final LaunchMode launchMode, final SmallRyeConfig config) {
        this(launchMode, config.getProfiles());
    }

    ProductionProfileGuard(final LaunchMode launchMode, final List<String> profiles) {
        this.launchMode = launchMode;
        this.profiles = profiles;
    }

    void checkProfiles(@Observes @Initialized(ApplicationScoped.class) @Priority(PLATFORM_BEFORE) final Object event) {
        if (this.launchMode == LaunchMode.NORMAL
                && this.profiles.stream().anyMatch(NON_PRODUCTION_PROFILES::contains)) {
            LOG.fatalf(
                    "A production build is running with the profile(s) %s, which would drop and recreate the "
                            + "database tables. Nothing was changed. Remove quarkus.profile (QUARKUS_PROFILE) and "
                            + "quarkus.config.profile.parent (QUARKUS_CONFIG_PROFILE_PARENT) from the configuration.",
                    this.profiles);
            throw new IllegalStateException("Production build started with profile(s) " + this.profiles);
        }
    }
}
