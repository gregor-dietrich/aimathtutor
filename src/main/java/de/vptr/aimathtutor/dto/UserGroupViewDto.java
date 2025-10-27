package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.entity.UserGroupEntity;

/**
 * TODO: Class documentation.
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
     * TODO: Document toUserGroupDto().
     */
    public UserGroupDto toUserGroupDto() {
        final var dto = new UserGroupDto();
        dto.id = this.id;
        dto.name = this.name;
        return dto;
    }
}
