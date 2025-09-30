package de.vptr.aimathtutor.rest.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.rest.entity.UserEntity;

public class UserViewDto {

    public Long id;
    public String username;
    // Note: password and salt are never exposed in responses for security
    public String email;
    public Long rankId;
    public String rankName;
    public Boolean banned;
    public Boolean activated;
    // activationKey is sensitive and not exposed in normal responses
    public String lastIp;
    public LocalDateTime created;
    public LocalDateTime lastLogin;
    public Long postsCount;
    public Long commentsCount;

    public UserViewDto() {
    }

    public UserViewDto(final UserEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.username = entity.username;
            // password and salt are NEVER exposed
            this.email = entity.email;
            this.rankId = entity.rank != null ? entity.rank.id : null;
            this.rankName = entity.rank != null ? entity.rank.name : null;
            this.banned = entity.banned;
            this.activated = entity.activated;
            // activationKey is not exposed for security
            this.lastIp = entity.lastIp;
            this.created = entity.created;
            this.lastLogin = entity.lastLogin;
            this.postsCount = entity.posts != null ? (long) entity.posts.size() : 0L;
            this.commentsCount = entity.comments != null ? (long) entity.comments.size() : 0L;
        }
    }

    public UserDto toUserDto() {
        final var dto = new UserDto();
        dto.id = this.id;
        dto.username = this.username;
        dto.email = this.email;
        dto.rankId = this.rankId;
        dto.banned = this.banned;
        // password is not included - must be set separately if updating password
        return dto;
    }
}
