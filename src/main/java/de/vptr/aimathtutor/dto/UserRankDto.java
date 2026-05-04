package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.util.AppConstants;
import jakarta.validation.constraints.Size;

/**
 * Data transfer object for user ranks.
 * Contains role information and permission flags for different operations
 * on exercises, lessons, comments, and administrative functions.
 */
public class UserRankDto {

    public Long id;

    @Size(min = AppConstants.USERNAME_MIN_LENGTH, max = AppConstants.USERNAME_MAX_LENGTH, message = "Name must be between "
            + AppConstants.USERNAME_MIN_LENGTH + " and " + AppConstants.USERNAME_MAX_LENGTH + " characters")
    public String name;

    // View permissions
    public Boolean adminView;

    // Exercise permissions
    public Boolean exerciseAdd;
    public Boolean exerciseDelete;
    public Boolean exerciseEdit;

    // Lesson permissions
    public Boolean lessonAdd;
    public Boolean lessonDelete;
    public Boolean lessonEdit;

    // Comment permissions
    public Boolean commentAdd;
    public Boolean commentDelete;
    public Boolean commentEdit;

    // User permissions
    public Boolean userAdd;
    public Boolean userDelete;
    public Boolean userEdit;

    // User group permissions
    public Boolean userGroupAdd;
    public Boolean userGroupDelete;
    public Boolean userGroupEdit;

    // User rank permissions
    public Boolean userRankAdd;
    public Boolean userRankDelete;
    public Boolean userRankEdit;

    public UserRankDto() {
    }

    public UserRankDto(final String name) {
        this.name = name;
    }
}
