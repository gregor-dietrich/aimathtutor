package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.UserEntity;

/**
 * DTO used to present a user in list and detail views. Contains non-sensitive
 * fields intended for UI consumption.
 */
public class UserViewDto {

    public String publicId;
    public String username;
    // Note: password is never exposed in responses for security
    public String email;
    public String rankPublicId;
    public String rankName;
    public Boolean banned;
    public Boolean activated;
    // activationKey is sensitive and not exposed in normal responses
    public LocalDateTime created;
    public Long exercisesCount;
    public Long commentsCount;
    public String userAvatarEmoji;
    public String tutorAvatarEmoji;

    public UserViewDto() {
    }

    /**
     * Constructs a UserViewDto from a UserEntity.
     */
    public UserViewDto(final UserEntity entity) {
        if (entity != null) {
            this.publicId = entity.publicId;
            this.username = entity.username;
            // password is NEVER exposed
            this.email = entity.email;
            this.rankPublicId = entity.rank != null ? entity.rank.publicId : null;
            this.rankName = entity.rank != null ? entity.rank.name : null;
            this.banned = entity.banned;
            this.activated = entity.activated;
            // activationKey is not exposed for security
            this.created = entity.created;
            this.exercisesCount = entity.exercises != null ? (long) entity.exercises.size() : 0L;
            this.commentsCount = entity.comments != null ? (long) entity.comments.size() : 0L;
            this.userAvatarEmoji = entity.userAvatarEmoji;
            this.tutorAvatarEmoji = entity.tutorAvatarEmoji;
        }
    }

    /**
     * Convert this view DTO to a minimal editable {@link UserDto} instance.
     * Sensitive fields like password are not transferred and must be handled
     * separately.
     *
     * @return a new UserDto populated from view fields
     */
    public UserDto toUserDto() {
        final var dto = new UserDto();
        dto.publicId = this.publicId;
        dto.username = this.username;
        dto.email = this.email;
        dto.rankPublicId = this.rankPublicId;
        dto.banned = this.banned;
        dto.activated = this.activated;
        // password is not included - must be set separately if updating password
        return dto;
    }
}
