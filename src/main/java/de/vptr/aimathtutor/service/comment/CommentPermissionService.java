package de.vptr.aimathtutor.service.comment;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

/**
 * Service for comment permission checks.
 */
@ApplicationScoped
public class CommentPermissionService {

    /**
     * Checks if a user has moderator privileges.
     *
     * @param user the user to check
     * @return true if the user is a moderator or admin
     */
    public boolean isModerator(final UserEntity user) {
        return user != null && user.rank != null
                && (Boolean.TRUE.equals(user.rank.exerciseEdit)
                        || Boolean.TRUE.equals(user.rank.adminView));
    }

    /**
     * Verifies that the requester is allowed to delete a comment.
     *
     * @param comment    the comment to delete
     * @param requester  the user requesting deletion
     * @param softDelete true for soft delete, false for hard delete
     * @throws WebApplicationException if not authorized
     */
    public void verifyCanDelete(final CommentEntity comment, final UserEntity requester, final boolean softDelete) {
        if (requester == null) {
            throw new WebApplicationException("Not authorized to delete this comment", Status.FORBIDDEN);
        }
        final boolean isAuthor = comment.user != null && comment.user.id.equals(requester.id);
        final boolean isModerator = this.isModerator(requester);

        if (!isAuthor && !isModerator) {
            throw new WebApplicationException("Not authorized to delete this comment", Status.FORBIDDEN);
        }

        if (!softDelete && !isModerator) {
            throw new WebApplicationException("Only moderators can permanently delete", Status.FORBIDDEN);
        }
    }

    /**
     * Verifies that the editor is allowed to edit a comment.
     *
     * @param comment the comment to edit
     * @param editor  the user requesting the edit
     * @throws WebApplicationException if not authorized
     */
    public void verifyCanEdit(final CommentEntity comment, final UserEntity editor) {
        if (editor == null) {
            throw new WebApplicationException("Not authorized to edit this comment", Status.FORBIDDEN);
        }
        final boolean isAuthor = comment.user != null && comment.user.id.equals(editor.id);
        final boolean isModerator = this.isModerator(editor);

        if (!isAuthor && !isModerator) {
            throw new WebApplicationException("Not authorized to edit this comment", Status.FORBIDDEN);
        }
    }
}
