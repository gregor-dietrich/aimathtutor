package de.vptr.aimathtutor.util;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.security.PasswordHashingService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Small CLI utility to generate a bcrypt hash for a password using the
 * project's PasswordHashingService. Intended for local/dev use to create
 * seeded passwords for `init.sql`.
 */
public final class PasswordUtility {

    private static final Logger LOG = Logger.getLogger(PasswordUtility.class);

    private static PasswordHashingService getHashingService() {
        try {
            return CDI.current().select(PasswordHashingService.class).get();
        } catch (final IllegalStateException e) {
            // CDI not available in CLI context — instantiate directly
            return new PasswordHashingService();
        }
    }

    private PasswordUtility() {
    }

    /**
     * Entry point for the password hashing utility CLI.
     * Accepts command-line arguments to generate password hashes.
     * Supports "generate" command with password as argument.
     *
     * @param args command-line arguments (command name and parameters)
     */
    public static void main(final String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        final var cmd = args[0];
        switch (cmd) {
            case "generate" -> handleGenerate(args);
            default -> {
                LOG.errorf("Unknown command: %s",  cmd);
                printUsage();
                System.exit(2);
            }
        }
    }

    private static void handleGenerate(final String[] args) {
        final var password = args[1];
        try {
            final var hash = getHashingService().hashPassword(password);

            System.out.println("hash=" + hash);
            System.out.println();
            System.out.println("SQL snippet (example):");
            System.out.println("INSERT INTO users (username, password, rank_id, activated) VALUES ('newuser', '"
                    + hash + "', 3, TRUE);");
        } catch (final Exception e) {
            LOG.error("Failed to generate hash", e);
            System.exit(3);
        }
    }

    private static void printUsage() {
        final String className = PasswordUtility.class.getName();
        System.out.println(className + " - small helper to generate bcrypt hash for local dev");
        System.out.println("Usage:");
        System.out.println("  java -cp target/classes " + className + " generate <password>");
        System.out.println("Example:");
        System.out.println("  mvn -q -Dexec.mainClass=\"" + className + "\" -Dexec.args=\"generate admin\" exec:java");
    }
}
