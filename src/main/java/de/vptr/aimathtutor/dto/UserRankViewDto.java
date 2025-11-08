package de.vptr.aimathtutor.dto;

import de.vptr.aimathtutor.entity.UserRankEntity;

/**
 * DTO representing a user rank/permission set for UI display.
 */
public class UserRankViewDto {

    public Long id;
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

    // Computed fields
    public Long usersCount;

    public UserRankViewDto() {
    }

    /**
     * Constructs a UserRankViewDto from a UserRankEntity.
     */
    public UserRankViewDto(final UserRankEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.name = entity.name;

            // View permissions
            this.adminView = entity.adminView;

            // Exercise permissions
            this.exerciseAdd = entity.exerciseAdd;
            this.exerciseDelete = entity.exerciseDelete;
            this.exerciseEdit = entity.exerciseEdit;

            // Lesson permissions
            this.lessonAdd = entity.lessonAdd;
            this.lessonDelete = entity.lessonDelete;
            this.lessonEdit = entity.lessonEdit;

            // Comment permissions
            this.commentAdd = entity.commentAdd;
            this.commentDelete = entity.commentDelete;
            this.commentEdit = entity.commentEdit;

            // User permissions
            this.userAdd = entity.userAdd;
            this.userDelete = entity.userDelete;
            this.userEdit = entity.userEdit;

            // User group permissions
            this.userGroupAdd = entity.userGroupAdd;
            this.userGroupDelete = entity.userGroupDelete;
            this.userGroupEdit = entity.userGroupEdit;

            // User rank permissions
            this.userRankAdd = entity.userRankAdd;
            this.userRankDelete = entity.userRankDelete;
            this.userRankEdit = entity.userRankEdit;

            // Computed fields
            this.usersCount = entity.users != null ? (long) entity.users.size() : 0L;
        }
    }

    // Helper methods for permission checking

    /**
     * Whether this rank grants access to the admin view features.
     */
    public boolean canAdminView() {
        return Boolean.TRUE.equals(this.adminView);
    }

    /**
     * Whether this rank has any exercise-related permissions (add/edit/delete).
     */
    public boolean hasAnyExercisePermission() {
        return Boolean.TRUE.equals(this.exerciseAdd) || Boolean.TRUE.equals(this.exerciseEdit)
                || Boolean.TRUE.equals(this.exerciseDelete);
    }

    /**
     * Whether this rank has any lesson-related permissions (add/edit/delete).
     */
    public boolean hasAnyLessonPermission() {
        return Boolean.TRUE.equals(this.lessonAdd) || Boolean.TRUE.equals(this.lessonEdit)
                || Boolean.TRUE.equals(this.lessonDelete);
    }

    /**
     * Whether this rank has any comment-related permissions (add/edit/delete).
     */
    public boolean hasAnyCommentPermission() {
        return Boolean.TRUE.equals(this.commentAdd) || Boolean.TRUE.equals(this.commentEdit)
                || Boolean.TRUE.equals(this.commentDelete);
    }

    /**
     * Whether this rank has any user-management permissions.
     */
    public boolean hasAnyUserPermission() {
        return Boolean.TRUE.equals(this.userAdd) || Boolean.TRUE.equals(this.userEdit)
                || Boolean.TRUE.equals(this.userDelete);
    }

    /**
     * Whether this rank has any user-group management permissions.
     */
    public boolean hasAnyUserGroupPermission() {
        return Boolean.TRUE.equals(this.userGroupAdd) || Boolean.TRUE.equals(this.userGroupEdit)
                || Boolean.TRUE.equals(this.userGroupDelete);
    }

    /**
     * Whether this rank has any user-rank management permissions.
     */
    public boolean hasAnyUserRankPermission() {
        return Boolean.TRUE.equals(this.userRankAdd) || Boolean.TRUE.equals(this.userRankEdit)
                || Boolean.TRUE.equals(this.userRankDelete);
    }

    /**
     * Convert this view into a mutable {@link UserRankDto} used for editing
     * and persistence.
     */
    public UserRankDto toUserRankDto() {
        final var dto = new UserRankDto();
        dto.id = this.id;
        dto.name = this.name;
        dto.adminView = this.adminView;
        dto.exerciseAdd = this.exerciseAdd;
        dto.exerciseDelete = this.exerciseDelete;
        dto.exerciseEdit = this.exerciseEdit;
        dto.lessonAdd = this.lessonAdd;
        dto.lessonDelete = this.lessonDelete;
        dto.lessonEdit = this.lessonEdit;
        dto.commentAdd = this.commentAdd;
        dto.commentDelete = this.commentDelete;
        dto.commentEdit = this.commentEdit;
        dto.userAdd = this.userAdd;
        dto.userDelete = this.userDelete;
        dto.userEdit = this.userEdit;
        dto.userGroupAdd = this.userGroupAdd;
        dto.userGroupDelete = this.userGroupDelete;
        dto.userGroupEdit = this.userGroupEdit;
        dto.userRankAdd = this.userRankAdd;
        dto.userRankDelete = this.userRankDelete;
        dto.userRankEdit = this.userRankEdit;
        return dto;
    }
}
