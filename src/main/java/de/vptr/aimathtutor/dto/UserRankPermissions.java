package de.vptr.aimathtutor.dto;

import jakarta.annotation.Nullable;

/**
 * Shared identity, version and permission fields for {@link UserRankDto} and {@link UserRankViewDto}. Extracted to
 * eliminate CPD duplication.
 */
public abstract class UserRankPermissions {

    protected UserRankPermissions() {
    }

    @Nullable
    public String publicId;

    /**
     * The stored version; on an update DTO, a write is refused when it differs from the stored one (null skips the
     * check).
     */
    @Nullable
    public Long version;

    // View permissions
    @Nullable
    public Boolean adminView;

    // Exercise permissions
    @Nullable
    public Boolean exerciseAdd;
    @Nullable
    public Boolean exerciseDelete;
    @Nullable
    public Boolean exerciseEdit;

    // Lesson permissions
    @Nullable
    public Boolean lessonAdd;
    @Nullable
    public Boolean lessonDelete;
    @Nullable
    public Boolean lessonEdit;

    // Comment permissions
    @Nullable
    public Boolean commentAdd;
    @Nullable
    public Boolean commentDelete;
    @Nullable
    public Boolean commentEdit;

    // User permissions
    @Nullable
    public Boolean userAdd;
    @Nullable
    public Boolean userDelete;
    @Nullable
    public Boolean userEdit;

    // User group permissions
    @Nullable
    public Boolean userGroupAdd;
    @Nullable
    public Boolean userGroupDelete;
    @Nullable
    public Boolean userGroupEdit;

    // User rank permissions
    @Nullable
    public Boolean userRankAdd;
    @Nullable
    public Boolean userRankDelete;
    @Nullable
    public Boolean userRankEdit;

    // AI configuration permissions
    @Nullable
    public Boolean aiConfigEdit;
}
