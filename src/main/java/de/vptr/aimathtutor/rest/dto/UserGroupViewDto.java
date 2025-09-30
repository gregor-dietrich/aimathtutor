package de.vptr.aimathtutor.rest.dto;

import de.vptr.aimathtutor.rest.entity.UserGroupEntity;

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

    public UserGroupDto toUserGroupDto() {
        final var dto = new UserGroupDto();
        dto.id = this.id;
        dto.name = this.name;
        return dto;
    }
}
