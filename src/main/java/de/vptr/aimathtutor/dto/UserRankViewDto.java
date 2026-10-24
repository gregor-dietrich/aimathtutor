package de.vptr.aimathtutor.dto;

import java.util.Set;

import de.vptr.aimathtutor.entity.UserRankEntity;
import jakarta.annotation.Nullable;

/**
 * DTO representing a user rank/permission set for UI display.
 */
public class UserRankViewDto extends UserRankPermissions {

    @Nullable
    public String name;

    public UserRankViewDto() {
    }

    /**
     * Constructs a UserRankViewDto from a UserRankEntity.
     */
    public UserRankViewDto(final UserRankEntity entity) {
        if (entity != null) {
            this.publicId = entity.publicId;
            this.version = entity.version;
            this.name = entity.name;
            for (final var permission : Permission.values()) {
                this.set(permission, entity.has(permission));
            }
        }
    }

    /**
     * Whether this rank holds any permission of a group.
     *
     * @param group
     *            the group to test
     * @return true if at least one of the group's permissions is granted
     */
    public boolean hasAny(final Permission.Group group) {
        return group.permissions().stream().anyMatch(this::has);
    }

    /**
     * Whether this rank grants a permission that {@code ceiling} lacks.
     *
     * @param ceiling
     *            the permissions to compare against
     * @return true if some permission is granted here but not in {@code ceiling}
     */
    public boolean grantsBeyond(final Set<Permission> ceiling) {
        return !ceiling.containsAll(this.granted());
    }

    /**
     * Convert this view into a mutable {@link UserRankDto} used for editing and persistence.
     */
    public UserRankDto toUserRankDto() {
        final var dto = new UserRankDto();
        dto.publicId = this.publicId;
        dto.version = this.version;
        dto.name = this.name;
        for (final var permission : Permission.values()) {
            dto.set(permission, this.get(permission));
        }
        return dto;
    }
}
