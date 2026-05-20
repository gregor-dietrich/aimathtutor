package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.annotation.Nullable;

/**
 * DTO used to present a user in list and detail views. Contains non-sensitive fields intended for UI consumption.
 */
public class UserViewDto {
    @Nullable
    public String publicId;
    @Nullable
    public String username;
    // Note: password is never exposed in responses for security
    @Nullable
    public String email;
    @Nullable
    public String rankPublicId;
    @Nullable
    public String rankName;
    @Nullable
    public Boolean banned;
    @Nullable
    public Boolean activated;
    // activationKey is sensitive and not exposed in normal responses
    @Nullable
    public LocalDateTime created;
    @Nullable
    public Long exercisesCount;
    @Nullable
    public Long commentsCount;
    @Nullable
    public String userAvatarEmoji;
    @Nullable
    public String tutorAvatarEmoji;

    public UserViewDto() {
    }

    /**
     * Constructs a UserViewDto from a UserEntity.
     */
    public UserViewDto(@Nullable final UserEntity entity) {
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
            this.exercisesCount = entity.exercisesCount != null ? entity.exercisesCount : 0L;
            this.commentsCount = entity.commentsCount != null ? entity.commentsCount : 0L;
            this.userAvatarEmoji = entity.userAvatarEmoji;
            this.tutorAvatarEmoji = entity.tutorAvatarEmoji;
        }
    }

    /**
     * Convert this view DTO to a minimal editable {@link UserDto} instance. Sensitive fields like password are not
     * transferred and must be handled separately.
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
