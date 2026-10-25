package de.vptr.aimathtutor.service.security;

import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.service.UserRankService;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.ValidationException;

/**
 * Service for checking user permissions based on their rank. Throws {@link PermissionDeniedException} when the
 * permission is missing or false. The session checks (the {@code require*} methods without a parameter, and
 * {@link #requireAdminView(SessionCredentials)} and {@link #requireAiConfigEdit(SessionCredentials)}) throw
 * {@link ValidationException} with the session-ended message when the session has ended (none, revoked credentials, or
 * a banned, deactivated or deleted account).
 */
@ApplicationScoped
public class PermissionService {

    @Inject
    UserRankService userRankService;

    @Inject
    AuthService authService;

    private void require(final boolean permission, final String action) {
        if (!permission) {
            throw new PermissionDeniedException("You do not have permission to " + action);
        }
    }

    private UserRankEntity getCurrentRank() {
        final var rank = this.authService.requireCurrentUser().rank;
        if (rank == null) {
            throw new PermissionDeniedException("You do not have permission to perform this action");
        }
        return rank;
    }

    /**
     * Returns the current user's rank entity, the ceiling for the users and ranks they may change, assign or grant.
     * This only delegates to {@link UserRankService#getCurrentUserRankEntity()}. It exists as the seam that service
     * tests stub, since they mock this service. The session checks resolve the caller through
     * {@link AuthService#requireCurrentUser()} instead. It joins the caller's transaction, so it returns the same
     * managed rank the caller may be about to edit.
     *
     * @return the current user's {@link UserRankEntity}, or null if no user is authenticated, the user no longer
     *         exists, has no rank, is banned or not activated, or the session's credentials are revoked
     */
    @Nullable
    public UserRankEntity findCurrentUserRank() {
        return this.userRankService.getCurrentUserRankEntity();
    }

    /**
     * Requires that the captured session still belongs to an active user whose rank may view the administration. Any
     * thread; it joins the caller's transaction, which it requires.
     *
     * @param credentials
     *            the credentials captured by {@link AuthService#currentSessionCredentials()}, or null
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the user's rank lacks the permission
     */
    @Transactional(TxType.MANDATORY)
    public void requireAdminView(@Nullable final SessionCredentials credentials) {
        final var user = this.authService.requireSessionUser(credentials);
        this.require(user.rank != null && user.rank.adminView, "view the administration");
    }

    // Exercise permissions

    public void requireExerciseAdd() {
        this.require(this.getCurrentRank().exerciseAdd, "add exercises");
    }

    public void requireExerciseEdit() {
        this.require(this.getCurrentRank().exerciseEdit, "edit exercises");
    }

    public void requireExerciseDelete() {
        this.require(this.getCurrentRank().exerciseDelete, "delete exercises");
    }

    // Lesson permissions

    public void requireLessonAdd() {
        this.require(this.getCurrentRank().lessonAdd, "add lessons");
    }

    public void requireLessonEdit() {
        this.require(this.getCurrentRank().lessonEdit, "edit lessons");
    }

    public void requireLessonDelete() {
        this.require(this.getCurrentRank().lessonDelete, "delete lessons");
    }

    // Comment permissions

    public void requireCommentAdd() {
        this.require(this.getCurrentRank().commentAdd, "add comments");
    }

    public void requireCommentEdit() {
        this.require(this.getCurrentRank().commentEdit, "edit comments");
    }

    public void requireCommentDelete() {
        this.require(this.getCurrentRank().commentDelete, "delete comments");
    }

    // User permissions

    public void requireUserAdd() {
        this.require(this.getCurrentRank().userAdd, "add users");
    }

    public void requireUserEdit() {
        this.require(this.getCurrentRank().userEdit, "edit users");
    }

    public void requireUserDelete() {
        this.require(this.getCurrentRank().userDelete, "delete users");
    }

    // User group permissions

    public void requireUserGroupAdd() {
        this.require(this.getCurrentRank().userGroupAdd, "add user groups");
    }

    public void requireUserGroupEdit() {
        this.require(this.getCurrentRank().userGroupEdit, "edit user groups");
    }

    public void requireUserGroupDelete() {
        this.require(this.getCurrentRank().userGroupDelete, "delete user groups");
    }

    // AI config permissions

    /**
     * Requires that the captured session still belongs to an active user whose rank may edit the AI configuration, and
     * returns that user for the audit trail. Any thread; it joins the caller's transaction, which it requires.
     *
     * @param credentials
     *            the credentials captured by {@link AuthService#currentSessionCredentials()}, or null
     * @return the resolved, managed {@link UserEntity}
     * @throws ValidationException
     *             if the session has ended
     * @throws PermissionDeniedException
     *             if the user's rank lacks the permission
     */
    @Transactional(TxType.MANDATORY)
    public UserEntity requireAiConfigEdit(@Nullable final SessionCredentials credentials) {
        final var user = this.authService.requireSessionUser(credentials);
        // Read only the flag, never the rank's lazy users collection.
        this.require(user.rank != null && user.rank.aiConfigEdit, "edit AI configuration");
        return user;
    }

    // User rank permissions

    public void requireUserRankAdd() {
        this.require(this.getCurrentRank().userRankAdd, "add user ranks");
    }

    public void requireUserRankEdit() {
        this.require(this.getCurrentRank().userRankEdit, "edit user ranks");
    }

    public void requireUserRankDelete() {
        this.require(this.getCurrentRank().userRankDelete, "delete user ranks");
    }
}
