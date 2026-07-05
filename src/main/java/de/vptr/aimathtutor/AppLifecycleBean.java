package de.vptr.aimathtutor;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Application lifecycle bean used for startup/shutdown hooks and initialization tasks.
 */
@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

    private final LaunchMode launchMode;

    private final String dbPassword;

    @Inject
    AppLifecycleBean(final LaunchMode launchMode,
            @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "") final String dbPassword) {
        this.launchMode = launchMode;
        this.dbPassword = dbPassword;
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
    }

    void onStop(@Observes final ShutdownEvent ev) {
        LOG.info("AI Math Tutor is shutting down. Goodbye! o/");
    }
}
