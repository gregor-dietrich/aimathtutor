package de.vptr.aimathtutor.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for user settings including password change and avatar customization.
 */
public class UserSettingsDto {

    @Size(min = 4, max = 100, message = "Current password must be between 4 and 100 characters")
    public String currentPassword;

    @Size(min = 4, max = 100, message = "New password must be between 4 and 100 characters")
    public String newPassword;

    @Size(max = 10, message = "User avatar emoji must not exceed 10 characters")
    public String userAvatarEmoji;

    @Size(max = 10, message = "Tutor avatar emoji must not exceed 10 characters")
    public String tutorAvatarEmoji;

    public UserSettingsDto() {
    }

    public UserSettingsDto(final String userAvatarEmoji, final String tutorAvatarEmoji) {
        this.userAvatarEmoji = userAvatarEmoji;
        this.tutorAvatarEmoji = tutorAvatarEmoji;
    }
}
