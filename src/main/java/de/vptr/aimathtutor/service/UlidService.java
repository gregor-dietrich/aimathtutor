package de.vptr.aimathtutor.service;

import com.github.f4b6a3.ulid.UlidCreator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

@ApplicationScoped
public class UlidService {

    private static final String ULID_REGEX = "^[0-7][0-9A-HJKMNP-TV-Z]{25}$";

    private UlidService() {
        // Utility class
    }

    public static String generate() {
        return UlidCreator.getUlid().toString();
    }

    public static boolean isValid(final String ulid) {
        return ulid != null && ulid.matches(ULID_REGEX);
    }

    public static void requireValid(final String ulid) {
        if (!isValid(ulid)) {
            throw new BadRequestException("Invalid ULID format: " + ulid);
        }
    }
}
