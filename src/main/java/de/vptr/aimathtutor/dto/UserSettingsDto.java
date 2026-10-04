package de.vptr.aimathtutor.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

/**
 * DTO for user settings: avatar customization. Password changes go through {@code UserService.changePassword}.
 */
public class UserSettingsDto {
    @Size(max = 10, message = "User avatar emoji must not exceed 10 characters")
    @Nullable
    public String userAvatarEmoji;

    @Size(max = 10, message = "Tutor avatar emoji must not exceed 10 characters")
    @Nullable
    public String tutorAvatarEmoji;

    public UserSettingsDto() {
    }

    public UserSettingsDto(final String userAvatarEmoji, final String tutorAvatarEmoji) {
        this.userAvatarEmoji = userAvatarEmoji;
        this.tutorAvatarEmoji = tutorAvatarEmoji;
    }
}
