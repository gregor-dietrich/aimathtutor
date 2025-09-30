package de.vptr.aimathtutor.rest.dto;

import jakarta.validation.constraints.Size;

public class UserRankDto {

    public Long id;

    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    public String name;

    // View permissions
    public Boolean adminView;

    // Page permissions
    public Boolean pageAdd;
    public Boolean pageDelete;
    public Boolean pageEdit;

    // Post permissions
    public Boolean postAdd;
    public Boolean postDelete;
    public Boolean postEdit;

    // Post category permissions
    public Boolean postCategoryAdd;
    public Boolean postCategoryDelete;
    public Boolean postCategoryEdit;

    // Post comment permissions
    public Boolean postCommentAdd;
    public Boolean postCommentDelete;
    public Boolean postCommentEdit;

    // User permissions
    public Boolean userAdd;
    public Boolean userDelete;
    public Boolean userEdit;

    // User group permissions
    public Boolean userGroupAdd;
    public Boolean userGroupDelete;
    public Boolean userGroupEdit;

    // Account permissions
    public Boolean userAccountAdd;
    public Boolean userAccountDelete;
    public Boolean userAccountEdit;

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
