package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.SessionCredentials;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import de.vptr.aimathtutor.repository.UserRepository;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.util.AppConstants;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionalException;
import jakarta.validation.ValidationException;

@QuarkusTest
@SuppressWarnings("PMD.TooManyMethods")
class PermissionServiceTest {

    @InjectMock
    UserRankService userRankService;

    @InjectMock
    UserRepository userRepository;

    // A spy, because the requireAdminView tests below run the real AuthService against the mocked repository.
    @InjectSpy
    AuthService authService;

    @Inject
    PermissionService permissionService;

    private void stubCallerRank(@Nullable final UserRankEntity rank) {
        final var caller = new UserEntity();
        caller.rank = rank;
        doReturn(caller).when(this.authService).requireCurrentUser();
    }

    private UserRankEntity buildRank(final Permission... permissions) {
        final var rank = new UserRankEntity();
        for (final var permission : permissions) {
            UserRankService.setPermission(rank, permission, true);
        }
        return rank;
    }

    @Test
    @DisplayName("requireExerciseAdd throws when permission is false")
    void requireExerciseAddThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseAdd());
        assertEquals("You do not have permission to add exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseAdd succeeds when permission is true")
    void requireExerciseAddSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.EXERCISE_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseAdd());
    }

    @Test
    @DisplayName("requireExerciseEdit throws when permission is false")
    void requireExerciseEditThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseEdit());
        assertEquals("You do not have permission to edit exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseEdit succeeds when permission is true")
    void requireExerciseEditSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.EXERCISE_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseEdit());
    }

    @Test
    @DisplayName("requireExerciseDelete throws when permission is false")
    void requireExerciseDeleteThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseDelete());
        assertEquals("You do not have permission to delete exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseDelete succeeds when permission is true")
    void requireExerciseDeleteSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.EXERCISE_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseDelete());
    }

    @Test
    @DisplayName("requireLessonAdd throws when permission is false")
    void requireLessonAddThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireLessonAdd());
        assertEquals("You do not have permission to add lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonAdd succeeds when permission is true")
    void requireLessonAddSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.LESSON_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireLessonAdd());
    }

    @Test
    @DisplayName("requireCommentAdd throws when permission is false")
    void requireCommentAddThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireCommentAdd());
        assertEquals("You do not have permission to add comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentAdd succeeds when permission is true")
    void requireCommentAddSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.COMMENT_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireCommentAdd());
    }

    @Test
    @DisplayName("requireUserAdd throws when permission is false")
    void requireUserAddThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserAdd());
        assertEquals("You do not have permission to add users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserAdd succeeds when permission is true")
    void requireUserAddSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireUserAdd());
    }

    @Test
    @DisplayName("requireUserGroupAdd throws when permission is false")
    void requireUserGroupAddThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserGroupAdd());
        assertEquals("You do not have permission to add user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupAdd succeeds when permission is true")
    void requireUserGroupAddSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_GROUP_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupAdd());
    }

    @Test
    @DisplayName("requireUserRankAdd throws when permission is false")
    void requireUserRankAddThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserRankAdd());
        assertEquals("You do not have permission to add user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankAdd succeeds when permission is true")
    void requireUserRankAddSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_RANK_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankAdd());
    }

    @Test
    @DisplayName("requireLessonEdit throws when permission is false")
    void requireLessonEditThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireLessonEdit());
        assertEquals("You do not have permission to edit lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonEdit succeeds when permission is true")
    void requireLessonEditSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.LESSON_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireLessonEdit());
    }

    @Test
    @DisplayName("requireLessonDelete throws when permission is false")
    void requireLessonDeleteThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireLessonDelete());
        assertEquals("You do not have permission to delete lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonDelete succeeds when permission is true")
    void requireLessonDeleteSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.LESSON_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireLessonDelete());
    }

    @Test
    @DisplayName("requireCommentEdit throws when permission is false")
    void requireCommentEditThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireCommentEdit());
        assertEquals("You do not have permission to edit comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentEdit succeeds when permission is true")
    void requireCommentEditSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.COMMENT_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireCommentEdit());
    }

    @Test
    @DisplayName("requireCommentDelete throws when permission is false")
    void requireCommentDeleteThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireCommentDelete());
        assertEquals("You do not have permission to delete comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentDelete succeeds when permission is true")
    void requireCommentDeleteSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.COMMENT_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireCommentDelete());
    }

    @Test
    @DisplayName("requireUserEdit throws when permission is false")
    void requireUserEditThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserEdit());
        assertEquals("You do not have permission to edit users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserEdit succeeds when permission is true")
    void requireUserEditSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireUserEdit());
    }

    @Test
    @DisplayName("requireUserDelete throws when permission is false")
    void requireUserDeleteThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserDelete());
        assertEquals("You do not have permission to delete users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserDelete succeeds when permission is true")
    void requireUserDeleteSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireUserDelete());
    }

    @Test
    @DisplayName("requireUserGroupEdit throws when permission is false")
    void requireUserGroupEditThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserGroupEdit());
        assertEquals("You do not have permission to edit user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupEdit succeeds when permission is true")
    void requireUserGroupEditSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_GROUP_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupEdit());
    }

    @Test
    @DisplayName("requireUserGroupDelete throws when permission is false")
    void requireUserGroupDeleteThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserGroupDelete());
        assertEquals("You do not have permission to delete user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupDelete succeeds when permission is true")
    void requireUserGroupDeleteSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_GROUP_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupDelete());
    }

    @Test
    @DisplayName("requireUserRankEdit throws when permission is false")
    void requireUserRankEditThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserRankEdit());
        assertEquals("You do not have permission to edit user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankEdit succeeds when permission is true")
    void requireUserRankEditSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_RANK_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankEdit());
    }

    @Test
    @DisplayName("requireUserRankDelete throws when permission is false")
    void requireUserRankDeleteThrowsWhenFalse() {
        this.stubCallerRank(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserRankDelete());
        assertEquals("You do not have permission to delete user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankDelete succeeds when permission is true")
    void requireUserRankDeleteSucceedsWhenTrue() {
        this.stubCallerRank(this.buildRank(Permission.USER_RANK_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankDelete());
    }

    @Test
    @DisplayName("throws when the resolved caller has no rank")
    void throwsWhenUserRankIsNull() {
        this.stubCallerRank(null);
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseAdd());
        assertEquals("You do not have permission to perform this action", ex.getMessage());
    }

    @Test
    @DisplayName("require checks let an ended session's ValidationException through")
    void requireRefusesEndedSession() {
        doThrow(new ValidationException(AppConstants.SESSION_ENDED_MESSAGE)).when(this.authService)
                .requireCurrentUser();
        final var ex = assertThrows(ValidationException.class, () -> this.permissionService.requireExerciseAdd());
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        verifyNoInteractions(this.userRankService);
    }

    @Test
    @DisplayName("Session permission checks never traverse rank membership")
    void requirePermissionsDoesNotInitializeLazyUsers() {
        @SuppressWarnings("unchecked")
        final List<UserEntity> detachedUsers = mock(List.class);
        when(detachedUsers.size()).thenThrow(new LazyInitializationException("simulated detached users collection"));
        final var rank = this.buildRank(Permission.EXERCISE_ADD);
        rank.users = detachedUsers;
        this.stubCallerRank(rank);

        assertDoesNotThrow(() -> this.permissionService.requireExerciseAdd());
        assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseEdit());
        verifyNoInteractions(detachedUsers);
    }

    @Test
    @TestTransaction
    @DisplayName("requireAiConfigEdit returns the user without touching the lazy users collection")
    void requireAiConfigEditDoesNotInitializeLazyUsers() {
        // Regression: building a UserRankViewDto here used to read the lazy `users` collection and fail with a
        // LazyInitializationException on a detached rank. The check now runs in a transaction, but must still read
        // only the flag. Simulate the detached collection and assert the check does not touch it.
        @SuppressWarnings("unchecked")
        final List<UserEntity> detachedUsers = mock(List.class);
        when(detachedUsers.size()).thenThrow(new LazyInitializationException("simulated detached users collection"));

        final var rank = this.buildRank(Permission.AI_CONFIG_EDIT);
        rank.users = detachedUsers;
        final var user = this.stubUser(rank);

        final var result = assertDoesNotThrow(() -> this.permissionService.requireAiConfigEdit(credentialsFor(user)));
        assertSame(user, result);
        verifyNoInteractions(detachedUsers);
    }

    @Test
    @TestTransaction
    @DisplayName("requireAiConfigEdit throws when the rank lacks the permission")
    void requireAiConfigEditThrowsWhenNotPermitted() {
        final var user = this.stubUser(this.buildRank());

        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireAiConfigEdit(credentialsFor(user)));
        assertEquals("You do not have permission to edit AI configuration", ex.getMessage());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    @DisplayName("requireAiConfigEdit throws when the user has a null rank")
    void requireAiConfigEditThrowsWhenRankIsNull() {
        final var user = this.stubUser(null);

        assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireAiConfigEdit(credentialsFor(user)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    @DisplayName("requireAiConfigEdit passes an ended session through as the session-ended ValidationException")
    void requireAiConfigEditRefusesEndedSession() {
        final var user = this.stubUser(this.buildRank(Permission.AI_CONFIG_EDIT));
        final var stale = new SessionCredentials(user.publicId, "stale", UUID.randomUUID().toString());

        final var ex = assertThrows(ValidationException.class, () -> this.permissionService.requireAiConfigEdit(stale));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        assertThrows(ValidationException.class, () -> this.permissionService.requireAiConfigEdit(null));
    }

    @Test
    @DisplayName("findCurrentUserRank returns the current user's rank entity")
    void findCurrentUserRankReturnsCurrentRank() {
        final var rank = new UserRankEntity();
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(rank);
        assertSame(rank, this.permissionService.findCurrentUserRank());
    }

    private UserEntity stubUser(final UserRankEntity rank) {
        final var user = new UserEntity();
        user.publicId = UUID.randomUUID().toString();
        user.password = "hash-" + user.publicId;
        user.activated = true;
        user.rank = rank;
        when(this.userRepository.findByPublicId(user.publicId)).thenReturn(Optional.of(user));
        return user;
    }

    @SuppressWarnings("NullAway")
    private static SessionCredentials credentialsFor(final UserEntity user) {
        return new SessionCredentials(user.publicId, AuthService.credentialStamp(user.password),
                UUID.randomUUID().toString());
    }

    @Test
    @TestTransaction
    @DisplayName("requireAdminView succeeds for an active user whose rank holds adminView")
    void requireAdminViewSucceeds() {
        final var rank = this.buildRank(Permission.ADMIN_VIEW);
        final var user = this.stubUser(rank);

        assertDoesNotThrow(() -> this.permissionService.requireAdminView(credentialsFor(user)));
    }

    @Test
    @TestTransaction
    @DisplayName("requireAdminView throws for a rank without adminView")
    void requireAdminViewThrowsWithoutPermission() {
        final var user = this.stubUser(this.buildRank());

        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireAdminView(credentialsFor(user)));
        assertEquals("You do not have permission to view the administration", ex.getMessage());
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    @DisplayName("requireAdminView throws for a user without a rank")
    void requireAdminViewThrowsWithoutRank() {
        final var user = this.stubUser(null);

        assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireAdminView(credentialsFor(user)));
    }

    @SuppressWarnings("NullAway")
    @Test
    @TestTransaction
    @DisplayName("requireAdminView refuses an ended session")
    void requireAdminViewRefusesEndedSession() {
        final var rank = this.buildRank(Permission.ADMIN_VIEW);
        final var user = this.stubUser(rank);
        final var stale = new SessionCredentials(user.publicId, "stale", UUID.randomUUID().toString());

        final var ex = assertThrows(ValidationException.class, () -> this.permissionService.requireAdminView(stale));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        assertThrows(ValidationException.class, () -> this.permissionService.requireAdminView(null));
    }

    @Test
    @DisplayName("requireAdminView refuses a caller with no transaction")
    void requireAdminViewRequiresTransaction() {
        final var rank = this.buildRank(Permission.ADMIN_VIEW);
        final var user = this.stubUser(rank);

        assertThrows(TransactionalException.class, () -> this.permissionService.requireAdminView(credentialsFor(user)));
    }
}
