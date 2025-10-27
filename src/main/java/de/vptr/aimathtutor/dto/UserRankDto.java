package de.vptr.aimathtutor.dto;

import jakarta.validation.constraints.Size;

/**
 * TODO: Class documentation.
 */
public class UserRankDto {

    public Long id;

    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
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
