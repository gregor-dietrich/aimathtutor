package de.vptr.aimathtutor.util;

import de.vptr.aimathtutor.security.PasswordHashingService;

/**
 * Small CLI utility to generate a salt and hash for a password using the
 * project's PasswordHashingService. Intended for local/dev use to create
 * seeded passwords for `init.sql`.
 */
public final class PasswordUtility {

    private static final PasswordHashingService hashingService = new PasswordHashingService();

    private PasswordUtility() {
    }

    /**
     * TODO: Document main().
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
                System.err.println("Unknown command: " + cmd);
                printUsage();
                System.exit(2);
            }
        }
    }

    private static void handleGenerate(final String[] args) {
        final var password = args[1];
        try {
            final var salt = hashingService.generateSalt();
            final var hash = hashingService.hashPassword(password, salt);

            System.out.println("salt=" + salt);
            System.out.println("hash=" + hash);
            System.out.println();
            System.out.println("SQL snippet (example):");
            System.out.println("INSERT INTO users (username, password, salt, rank_id, activated) VALUES ('newuser', '"
                    + hash + "', '" + salt + "', 3, TRUE);");
        } catch (final Exception e) {
            System.err.println("Failed to generate hash: " + e.getMessage());
            System.exit(3);
        }
    }

    private static void printUsage() {
        System.out.println("PasswordUtility - small helper to generate salt+hash for local dev");
        System.out.println("Usage:");
        System.out.println("  java -cp target/classes de.vptr.aimathtutor.util.PasswordUtility generate <password>");
        System.out.println("Example:");
        System.out.println(
                "  mvn -q -Dexec.mainClass=\"de.vptr.aimathtutor.util.PasswordUtility\" -Dexec.args=\"generate admin\" exec:java");
    }
}
