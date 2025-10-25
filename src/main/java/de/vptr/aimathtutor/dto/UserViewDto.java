package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.UserEntity;

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
    public LocalDateTime created;
    public LocalDateTime lastLogin;
    public Long exercisesCount;
    public Long commentsCount;
    public String userAvatarEmoji;
    public String tutorAvatarEmoji;

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
            this.created = entity.created;
            this.lastLogin = entity.lastLogin;
            this.exercisesCount = entity.exercises != null ? (long) entity.exercises.size() : 0L;
            this.commentsCount = entity.comments != null ? (long) entity.comments.size() : 0L;
            this.userAvatarEmoji = entity.userAvatarEmoji;
            this.tutorAvatarEmoji = entity.tutorAvatarEmoji;
        }
    }

    public UserDto toUserDto() {
        final var dto = new UserDto();
        dto.id = this.id;
        dto.username = this.username;
        dto.email = this.email;
        dto.rankId = this.rankId;
        dto.banned = this.banned;
        dto.activated = this.activated;
        // password is not included - must be set separately if updating password
        return dto;
    }
}
