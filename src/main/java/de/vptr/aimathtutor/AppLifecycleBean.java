package de.vptr.aimathtutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = LoggerFactory.getLogger(AppLifecycleBean.class);

    /**
     * ASCII art for the application logo.
     * This is displayed in the console when the application starts.
     * 
     * https://www.asciiart.eu/text-to-ascii-art
     * Font: Standard, Horizontal Layout: Squeezed, Border: Cats
     */
    private static final String asciiArt = """
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
        LOG.info("\n\n{}\n", asciiArt);
    }

    void onStop(@Observes final ShutdownEvent ev) {
        LOG.info("AI Math Tutor is shutting down. Goodbye! o/");
    }
}
