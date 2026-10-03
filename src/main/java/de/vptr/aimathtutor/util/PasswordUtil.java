package de.vptr.aimathtutor.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;

import de.vptr.aimathtutor.service.security.PasswordHashingService;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Small CLI utility to generate a bcrypt hash for a password using the project's PasswordHashingService. Intended for
 * local/dev use to create seeded passwords for `db/demo/R__demo_data.sql`.
 */
public final class PasswordUtil {

    private static final Logger LOG = Logger.getLogger(PasswordUtil.class);

    private static PasswordHashingService getHashingService() {
        try {
            return CDI.current().select(PasswordHashingService.class).get();
        } catch (final IllegalStateException e) {
            // CDI not available in CLI context — instantiate directly
            return new PasswordHashingService();
        }
    }

    private PasswordUtil() {
    }

    /**
     * Entry point for the password hashing utility CLI. Supports the "generate" command, which reads the password from
     * standard input.
     *
     * @param args
     *            command-line arguments (command name and parameters)
     */
    public static void main(final String[] args) {
        if (args.length < 1) {
            printUsage();
            System.exit(1);
        }

        final var cmd = args[0];
        switch (cmd) {
            case "generate" -> handleGenerate();
            default -> {
                LOG.errorf("Unknown command: %s", cmd);
                printUsage();
                System.exit(2);
            }
        }
    }

    /**
     * Hashes the first line of {@code in}. The password is read from standard input rather than taken as an argument:
     * exec:java splits exec.args on whitespace, which kept only a password's first word, and an argument shows up on
     * the process command line.
     *
     * @param in
     *            the stream whose first line is the password
     * @return the bcrypt hash of the password
     * @throws IOException
     *             if reading the stream fails
     * @throws IllegalArgumentException
     *             if the stream holds no password
     */
    static String generate(final InputStream in) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            final var password = reader.readLine();
            if (password == null || password.isEmpty()) {
                throw new IllegalArgumentException("No password on standard input");
            }
            return getHashingService().hashPassword(password);
        }
    }

    private static void handleGenerate() {
        try {
            final var hash = generate(System.in);

            System.out.println("hash=" + hash);
            System.out.println();
            System.out.println("SQL snippet (example):");
            System.out.println("INSERT INTO users (username, password, rank_id, activated) VALUES ('newuser', '" + hash
                    + "', 3, TRUE);");
        } catch (final IllegalArgumentException e) {
            LOG.error(e.getMessage());
            printUsage();
            System.exit(1);
        } catch (final Exception e) {
            LOG.error("Failed to generate hash", e);
            System.exit(3);
        }
    }

    private static void printUsage() {
        final String className = PasswordUtil.class.getName();
        System.out.println(className + " - small helper to generate bcrypt hash for local dev");
        System.out.println("Usage:");
        System.out.println("  java -cp target/classes " + className + " generate < password-file");
        System.out.println("Example:");
        System.out.println(
                "  printf '%s' admin | mvn -q -Dexec.mainClass=\"" + className + "\" -Dexec.args=generate exec:java");
    }
}
