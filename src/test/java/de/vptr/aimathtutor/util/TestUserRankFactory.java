package de.vptr.aimathtutor.util;

import java.util.UUID;

import de.vptr.aimathtutor.dto.UserRankDto;
import de.vptr.aimathtutor.dto.UserRankPermissions;
import de.vptr.aimathtutor.entity.UserRankEntity;

/**
 * Factory for creating test rank DTOs by permission flag name, for tests that cover every flag.
 */
public final class TestUserRankFactory {

    private TestUserRankFactory() {
    }

    /**
     * Builds a rank DTO with a fresh name that grants every permission except {@code missing}, or all of them for
     * {@code "none"}.
     *
     * @param missing
     *            the flag to leave off, or {@code "none"}
     * @return the rank DTO
     */
    public static UserRankDto rankWithout(final String missing) {
        if (!"none".equals(missing) && !UserRankEntity.PERMISSION_FIELDS.contains(missing)) {
            throw new IllegalArgumentException("No permission flag " + missing);
        }
        final var dto = new UserRankDto("Rank_" + UUID.randomUUID().toString().substring(0, 8));
        UserRankEntity.PERMISSION_FIELDS.forEach(flag -> setFlag(dto, flag, !flag.equals(missing)));
        return dto;
    }

    /**
     * Sets one permission flag of a rank DTO by name.
     *
     * @param dto
     *            the DTO to change
     * @param flag
     *            the flag name, from {@link UserRankEntity#PERMISSION_FIELDS}
     * @param value
     *            the value to set
     */
    public static void setFlag(final UserRankPermissions dto, final String flag, final boolean value) {
        try {
            UserRankPermissions.class.getField(flag).set(dto, value);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalArgumentException("No permission flag " + flag, e);
        }
    }
}
