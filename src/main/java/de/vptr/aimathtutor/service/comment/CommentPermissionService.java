package de.vptr.aimathtutor.service.comment;

import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.service.PermissionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

/**
 * Service for comment ownership checks.
 * Rank-based permissions (e.g. commentEdit, commentDelete) are enforced
 * by {@link PermissionService} in the service layer.
 * This class only verifies that a user is the author of a comment.
 */
@ApplicationScoped
public class CommentPermissionService {

    /**
     * Checks if the given user is the author of the comment.
     *
     * @param comment the comment to check
     * @param user    the user to check
     * @return true if the user authored the comment
     */
    public boolean isAuthor(final CommentEntity comment, final UserEntity user) {
        return user != null && comment.user != null && comment.user.id.equals(user.id);
    }

    /**
     * Verifies that the requester is the author of the comment.
     * Throws {@link WebApplicationException} with FORBIDDEN status if not.
     *
     * @param comment   the comment to check
     * @param requester the user requesting the action
     * @throws WebApplicationException if the requester is not the author
     */
    public void verifyIsAuthorOrThrow(final CommentEntity comment, final UserEntity requester) {
        if (!this.isAuthor(comment, requester)) {
            throw new WebApplicationException("Not authorized: you are not the author of this comment",
                    Status.FORBIDDEN);
        }
    }
}
