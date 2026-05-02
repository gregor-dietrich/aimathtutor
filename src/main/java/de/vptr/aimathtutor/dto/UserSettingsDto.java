package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.validation.constraints.Size;

/**
 * DTO for user settings including password change and avatar customization.
 */
public class UserSettingsDto {

    @Size(max = 100, message = "Current password must not exceed 100 characters")
    public String currentPassword;

    // Note: message must match PASSWORD_MIN_LENGTH (currently 8)
    @Size(min = AppConstants.PASSWORD_MIN_LENGTH, max = 100, message = "New password must be between 8 and 100 characters")
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
