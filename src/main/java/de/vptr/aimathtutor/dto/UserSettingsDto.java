package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.validation.constraints.Size;

/**
 * DTO for user settings including password change and avatar customization.
 */
public class UserSettingsDto {

    @Size(min = AppConstants.PASSWORD_MIN_LENGTH, max = AppConstants.PASSWORD_MAX_LENGTH, message = "Password must be between {min} and {max} characters")
    public String currentPassword;

    // Note: message must match PASSWORD_MIN_LENGTH (currently 8)
    @Size(min = AppConstants.PASSWORD_MIN_LENGTH, max = AppConstants.PASSWORD_MAX_LENGTH, message = "Password must be between {min} and {max} characters")
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
