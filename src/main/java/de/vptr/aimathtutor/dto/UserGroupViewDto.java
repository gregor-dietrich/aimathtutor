package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.entity.UserGroupEntity;

/**
 * DTO for presenting a user group in listings. Contains computed user count
 * and conversion helper to the editable UserGroupDto.
 */
public class UserGroupViewDto {
    public Long id;
    public String name;
    public Long userCount;

    public UserGroupViewDto() {
    }

    public UserGroupViewDto(final UserGroupEntity entity) {
        this.id = entity.id;
        this.name = entity.name;
        this.userCount = entity.getUserCount();
    }

    /**
     * Convert this view DTO to a persistent/editable {@link UserGroupDto}.
     *
     * @return new UserGroupDto populated from this view
     */
    public UserGroupDto toUserGroupDto() {
        final var dto = new UserGroupDto();
        dto.id = this.id;
        dto.name = this.name;
        return dto;
    }
}
