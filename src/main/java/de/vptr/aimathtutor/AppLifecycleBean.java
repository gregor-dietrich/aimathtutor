package de.vptr.aimathtutor;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import de.vptr.aimathtutor.service.UserService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Application lifecycle bean used for startup/shutdown hooks and initialization tasks.
 */
@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

    private static final String BOOTSTRAP_DONE_HINT =
            "APP_BOOTSTRAP_ADMIN_PASSWORD is no longer needed and can be removed from the configuration.";

    private final LaunchMode launchMode;

    private final String dbPassword;

    private final UserService userService;

    private final String bootstrapAdminUsername;

    @Nullable
    private final String bootstrapAdminPassword;

    @Inject
    AppLifecycleBean(final LaunchMode launchMode,
            @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "") final String dbPassword,
            final UserService userService,
            @ConfigProperty(name = "app.bootstrap.admin-username",
                    defaultValue = "admin") final String bootstrapAdminUsername,
            @ConfigProperty(name = "app.bootstrap.admin-password") final Optional<String> bootstrapAdminPassword) {
        this.launchMode = launchMode;
        this.dbPassword = dbPassword;
        this.userService = userService;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword.orElse(null);
    }

    /**
     * ASCII art for the application logo. This is displayed in the console when the application starts.
     * 
     * https://www.asciiart.eu/text-to-ascii-art Font: Standard, Horizontal Layout: Squeezed, Border: Cats
     */
    private static final String ASCII_ART = """
             /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\
            ( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )
             > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <
             /\\_/\\       _    ___   __  __       _   _       _____      _                 /\\_/\\
            ( o.o )     / \\  |_ _| |  \\/  | __ _| |_| |__   |_   __   _| |_ ___  _ __    ( o.o )
             > ^ <     / _ \\  | |  | |\\/| |/ _` | __| '_ \\    | || | | | __/ _ \\| '__|    > ^ <
             /\\_/\\    / ___ \\ | |  | |  | | (_| | |_| | | |   | || |_| | || (_) | |       /\\_/\\
            ( o.o )  /_/   \\_|___| |_|  |_|\\__,_|\\__|_| |_|   |_| \\__,_|\\__\\___/|_|      ( o.o )
             > ^ <                                                                        > ^ <
             /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\
            ( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )
             > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <""";

    void onStart(@Observes final StartupEvent ev) {
        LOG.infof("\n\n%s\n", ASCII_ART);
        if (launchMode == LaunchMode.NORMAL && "changeit".equals(dbPassword)) {
            LOG.error("FATAL: Default database password 'changeit' is in use in the production environment!");
            LOG.error("Please set the QUARKUS_DATASOURCE_PASSWORD environment variable to a strong password.");
            throw new IllegalStateException("Default database password 'changeit' in use in production");
        }
        if (launchMode == LaunchMode.NORMAL) {
            bootstrapAdmin();
            deactivateSeededDemoAccounts();
        }
    }

    /**
     * Creates the initial admin from configuration when no user exists, and replaces the published password of the
     * admin seeded by releases before 5.0.0. Fails startup when either is needed but no password is configured.
     */
    private void bootstrapAdmin() {
        final String password = bootstrapAdminPassword;
        if (password == null) {
            if (!userService.hasUsers()) {
                failBootstrap("No user account exists, so there is no admin to log in with.");
            }
            if (userService.hasSeededAdminPassword()) {
                failBootstrap("The seeded 'admin' account still accepts its published password 'admin'.");
            }
        } else if (userService.createInitialAdmin(bootstrapAdminUsername, password)) {
            LOG.infof("Created the initial admin account '%s'. %s", bootstrapAdminUsername, BOOTSTRAP_DONE_HINT);
        } else if (userService.replaceSeededAdminPassword(password)) {
            LOG.warnf("Replaced the published password of the seeded 'admin' account. %s", BOOTSTRAP_DONE_HINT);
        }
    }

    /**
     * Deactivates the demo accounts seeded by releases before 5.0.0 while they still accept their published passwords.
     */
    private void deactivateSeededDemoAccounts() {
        final var usernames = userService.deactivateSeededDemoAccounts();
        if (!usernames.isEmpty()) {
            LOG.warnf("Deactivated the seeded accounts %s because they still accept their published passwords. "
                    + "Set new passwords before reactivating them.", usernames);
        }
    }

    private static void failBootstrap(final String reason) {
        LOG.errorf("FATAL: %s", reason);
        LOG.error("Please set the APP_BOOTSTRAP_ADMIN_PASSWORD environment variable to a strong password.");
        throw new IllegalStateException(reason);
    }

    void onStop(@Observes final ShutdownEvent ev) {
        LOG.info("AI Math Tutor is shutting down. Goodbye! o/");
    }
}
