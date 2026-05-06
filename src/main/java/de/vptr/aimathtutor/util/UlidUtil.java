package de.vptr.aimathtutor.util;

import com.github.f4b6a3.ulid.UlidCreator;

import jakarta.ws.rs.BadRequestException;

/**
 * Utility class for generating and validating ULIDs.
 */
public final class UlidUtil {

    private static final String ULID_REGEX = "^[0-7][0-9A-HJKMNP-TV-Z]{25}$";

    private UlidUtil() {
        // Utility class
    }

    public static String generate() {
        return UlidCreator.getUlid().toString();
    }

    public static boolean isValid(final String ulid) {
        return ulid != null && ulid.matches(ULID_REGEX);
    }

    /**
     * Validates the given ULID and throws an exception if the format is invalid.
     *
     * @param ulid the ULID string to validate
     * @throws BadRequestException if the ULID format is invalid
     */
    public static void requireValid(final String ulid) {
        if (!isValid(ulid)) {
            throw new BadRequestException("Invalid ULID format: " + ulid);
        }
    }
}
