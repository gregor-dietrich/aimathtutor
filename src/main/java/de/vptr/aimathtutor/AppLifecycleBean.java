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
     * Font: Big Money-sw, Horizontal Layout: Squeezed, Border: Cats
     */
    private static final String asciiArt = """

             /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\
            ( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )
             > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <
             /\\_/\\   __       __ __       __                           ______  __    __ ______   /\\_/\\
            ( o.o ) /  \\     /  /  |     /  |                         /      \\/  |  /  /      | ( o.o )
             > ^ <  $$  \\   /$$ $$/  ____$$ | ______   _______       /$$$$$$  $$ |  $$ $$$$$$/   > ^ <
             /\\_/\\  $$$  \\ /$$$ /  |/    $$ |/      \\ /       |      $$ | _$$/$$ |  $$ | $$ |    /\\_/\\
            ( o.o ) $$$$  /$$$$ $$ /$$$$$$$ |$$$$$$  /$$$$$$$/       $$ |/    $$ |  $$ | $$ |   ( o.o )
             > ^ <  $$ $$ $$/$$ $$ $$ |  $$ |/    $$ $$      \\       $$ |$$$$ $$ |  $$ | $$ |    > ^ <
             /\\_/\\  $$ |$$$/ $$ $$ $$ \\__$$ /$$$$$$$ |$$$$$$  |      $$ \\__$$ $$ \\__$$ |_$$ |_   /\\_/\\
            ( o.o ) $$ | $/  $$ $$ $$    $$ $$    $$ /     $$/       $$    $$/$$    $$// $$   | ( o.o )
             > ^ <  $$/      $$/$$/ $$$$$$$/ $$$$$$$/$$$$$$$/         $$$$$$/  $$$$$$/ $$$$$$/   > ^ <
             /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\  /\\_/\\
            ( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )( o.o )
             > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ <  > ^ < """;

    void onStart(@Observes final StartupEvent ev) {
        LOG.info(asciiArt);
        LOG.info("Welcome to Midas GUI! o/");
    }

    void onStop(@Observes final ShutdownEvent ev) {
        LOG.info("Midas GUI is shutting down. Goodbye! \\o");
    }
}
