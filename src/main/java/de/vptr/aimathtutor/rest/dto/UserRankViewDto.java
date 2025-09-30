package de.vptr.aimathtutor.rest.dto;

import de.vptr.aimathtutor.rest.entity.UserRankEntity;

public class UserRankViewDto {

    public Long id;
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

    // Computed fields
    public Long usersCount;

    public UserRankViewDto() {
    }

    public UserRankViewDto(final UserRankEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.name = entity.name;

            // View permissions
            this.adminView = entity.adminView;

            // Page permissions
            this.pageAdd = entity.pageAdd;
            this.pageDelete = entity.pageDelete;
            this.pageEdit = entity.pageEdit;

            // Post permissions
            this.postAdd = entity.postAdd;
            this.postDelete = entity.postDelete;
            this.postEdit = entity.postEdit;

            // Post category permissions
            this.postCategoryAdd = entity.postCategoryAdd;
            this.postCategoryDelete = entity.postCategoryDelete;
            this.postCategoryEdit = entity.postCategoryEdit;

            // Post comment permissions
            this.postCommentAdd = entity.postCommentAdd;
            this.postCommentDelete = entity.postCommentDelete;
            this.postCommentEdit = entity.postCommentEdit;

            // User permissions
            this.userAdd = entity.userAdd;
            this.userDelete = entity.userDelete;
            this.userEdit = entity.userEdit;

            // User group permissions
            this.userGroupAdd = entity.userGroupAdd;
            this.userGroupDelete = entity.userGroupDelete;
            this.userGroupEdit = entity.userGroupEdit;

            // Account permissions
            this.userAccountAdd = entity.userAccountAdd;
            this.userAccountDelete = entity.userAccountDelete;
            this.userAccountEdit = entity.userAccountEdit;

            // User rank permissions
            this.userRankAdd = entity.userRankAdd;
            this.userRankDelete = entity.userRankDelete;
            this.userRankEdit = entity.userRankEdit;

            // Computed fields
            this.usersCount = entity.users != null ? (long) entity.users.size() : 0L;
        }
    }

    // Helper methods for permission checking
    public boolean canAdminView() {
        return Boolean.TRUE.equals(this.adminView);
    }

    public boolean hasAnyPostPermission() {
        return Boolean.TRUE.equals(this.postAdd) || Boolean.TRUE.equals(this.postEdit)
                || Boolean.TRUE.equals(this.postDelete);
    }

    public boolean hasAnyPostCategoryPermission() {
        return Boolean.TRUE.equals(this.postCategoryAdd) || Boolean.TRUE.equals(this.postCategoryEdit)
                || Boolean.TRUE.equals(this.postCategoryDelete);
    }

    public boolean hasAnyPostCommentPermission() {
        return Boolean.TRUE.equals(this.postCommentAdd) || Boolean.TRUE.equals(this.postCommentEdit)
                || Boolean.TRUE.equals(this.postCommentDelete);
    }

    public boolean hasAnyUserPermission() {
        return Boolean.TRUE.equals(this.userAdd) || Boolean.TRUE.equals(this.userEdit)
                || Boolean.TRUE.equals(this.userDelete);
    }

    public boolean hasAnyUserGroupPermission() {
        return Boolean.TRUE.equals(this.userGroupAdd) || Boolean.TRUE.equals(this.userGroupEdit)
                || Boolean.TRUE.equals(this.userGroupDelete);
    }

    public boolean hasAnyUserRankPermission() {
        return Boolean.TRUE.equals(this.userRankAdd) || Boolean.TRUE.equals(this.userRankEdit)
                || Boolean.TRUE.equals(this.userRankDelete);
    }

    public UserRankDto toUserRankDto() {
        final var dto = new UserRankDto();
        dto.id = this.id;
        dto.name = this.name;
        dto.adminView = this.adminView;
        dto.pageAdd = this.pageAdd;
        dto.pageDelete = this.pageDelete;
        dto.pageEdit = this.pageEdit;
        dto.postAdd = this.postAdd;
        dto.postDelete = this.postDelete;
        dto.postEdit = this.postEdit;
        dto.postCategoryAdd = this.postCategoryAdd;
        dto.postCategoryDelete = this.postCategoryDelete;
        dto.postCategoryEdit = this.postCategoryEdit;
        dto.postCommentAdd = this.postCommentAdd;
        dto.postCommentDelete = this.postCommentDelete;
        dto.postCommentEdit = this.postCommentEdit;
        dto.userAdd = this.userAdd;
        dto.userDelete = this.userDelete;
        dto.userEdit = this.userEdit;
        dto.userGroupAdd = this.userGroupAdd;
        dto.userGroupDelete = this.userGroupDelete;
        dto.userGroupEdit = this.userGroupEdit;
        dto.userAccountAdd = this.userAccountAdd;
        dto.userAccountDelete = this.userAccountDelete;
        dto.userAccountEdit = this.userAccountEdit;
        dto.userRankAdd = this.userRankAdd;
        dto.userRankDelete = this.userRankDelete;
        dto.userRankEdit = this.userRankEdit;
        return dto;
    }
}
