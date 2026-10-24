package de.vptr.aimathtutor.util;

import java.util.UUID;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.UserRankDto;
import jakarta.annotation.Nullable;

/**
 * Factory for creating test rank DTOs by permission, for tests that cover every permission.
 */
public final class TestUserRankFactory {

    private TestUserRankFactory() {
    }

    /**
     * Builds a rank DTO with a fresh name that grants every permission except {@code missing}, or all of them for null.
     *
     * @param missing
     *            the permission to leave off, or null
     * @return the rank DTO
     */
    public static UserRankDto rankWithout(@Nullable final Permission missing) {
        final var dto = new UserRankDto("Rank_" + UUID.randomUUID().toString().substring(0, 8));
        for (final var permission : Permission.values()) {
            dto.set(permission, permission != missing);
        }
        return dto;
    }
}
