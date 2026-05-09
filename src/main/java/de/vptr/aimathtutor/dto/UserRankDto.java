package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for user ranks. Contains role information and permission flags for different operations on
 * exercises, lessons, comments, and administrative functions.
 */
public class UserRankDto extends UserRankPermissions {

    @Size(min = AppConstants.USERRANK_NAME_MIN_LENGTH, max = AppConstants.USERRANK_NAME_MAX_LENGTH,
            message = "Name must be between {min} and {max} characters")
    @Nullable
    public String name;

    public UserRankDto() {
    }

    public UserRankDto(final String name) {
        this.name = name;
    }
}
