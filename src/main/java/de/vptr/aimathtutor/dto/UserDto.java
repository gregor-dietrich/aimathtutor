package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO representing a user used for API and UI binding.
 */
public class UserDto {
    @Nullable
    public String publicId;

    @Size(min = AppConstants.USER_USERNAME_MIN_LENGTH, max = AppConstants.USER_USERNAME_MAX_LENGTH,
            message = "Username must be between {min} and {max} characters")
    @Nullable
    public String username;

    @Size(min = AppConstants.PASSWORD_MIN_LENGTH, max = AppConstants.PASSWORD_MAX_LENGTH,
            message = "Password must be between {min} and {max} characters")
    @Nullable
    public String password;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Nullable
    public String email;

    @Nullable
    public String rankPublicId;

    @Nullable
    public Boolean banned;

    @Nullable
    public Boolean activated;

    public UserDto() {
    }

    /**
     * Constructs a UserDto with the specified parameters.
     */
    public UserDto(final String username, final String password, final String email, final String rankPublicId,
            final Boolean banned, final Boolean activated) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.rankPublicId = rankPublicId;
        this.banned = banned;
        this.activated = activated;
    }
}
