package de.vptr.aimathtutor.service.security;

import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.UserRankService;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for checking user permissions based on their rank. All permission checks are enforced against the currently
 * authenticated user's rank. Throws {@link PermissionDeniedException} when the permission is missing or false.
 */
@ApplicationScoped
public class PermissionService {

    @Inject
    UserRankService userRankService;

    @Inject
    UserRepository userRepository;

    private void require(@Nullable final Boolean permission, final String action) {
        if (!Boolean.TRUE.equals(permission)) {
            throw new PermissionDeniedException("You do not have permission to " + action);
        }
    }

    private UserRankViewDto getCurrentRank() {
        final var rank = this.userRankService.getCurrentUserRank();
        if (rank == null) {
            throw new PermissionDeniedException("You do not have permission to perform this action");
        }
        return rank;
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

    // AI config permissions (userId-based, for use in async/non-session contexts)

    /**
     * Checks that the user identified by {@code userId} has permission to edit AI configuration. Intended for async
     * contexts where no Vaadin session is available. Returns the resolved {@link UserEntity} so callers can use it for
     * audit-trail purposes without a second DB lookup.
     *
     * @param userId
     *            the ID of the user to authorise
     * @return the resolved {@link UserEntity}
     * @throws PermissionDeniedException
     *             if the user does not exist, has no rank, or lacks the required permissions
     */
    public UserEntity requireAiConfigEdit(final Long userId) {
        final UserEntity user = this.userRepository.findById(userId);
        // Read the permission flag straight off the (eagerly fetched) rank rather than building a
        // UserRankViewDto: that DTO's constructor counts the lazy `users` collection, which throws a
        // LazyInitializationException when this runs outside a Hibernate session — e.g. the pre-DNS
        // authorization check in AiConfigService, which is intentionally non-transactional and may run on a
        // background thread.
        if (user == null || user.rank == null || !user.rank.aiConfigEdit) {
            throw new PermissionDeniedException("You do not have permission to edit AI configuration");
        }
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
