package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.entity.UserGroupEntity;
import jakarta.annotation.Nullable;

/**
 * DTO for presenting a user group in listings. Contains computed user count and conversion helper to the editable
 * UserGroupDto.
 */
public class UserGroupViewDto {
    @Nullable
    public String publicId;
    @Nullable
    public String name;
    @Nullable
    public Long userCount;

    public UserGroupViewDto() {
    }

    /**
     * Constructs a UserGroupViewDto from a UserGroupEntity.
     */
    public UserGroupViewDto(@Nullable final UserGroupEntity entity) {
        if (entity != null) {
            this.publicId = entity.publicId;
            this.name = entity.name;
            this.userCount = entity.getUserCount();
        }
    }

    /**
     * Convert this view DTO to a persistent/editable {@link UserGroupDto}.
     *
     * @return new UserGroupDto populated from this view
     */
    public UserGroupDto toUserGroupDto() {
        final var dto = new UserGroupDto();
        dto.publicId = this.publicId;
        dto.name = this.name;
        return dto;
    }
}
