package de.vptr.aimathtutor.service.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @Inject
    PermissionService permissionService;

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
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseAdd());
        assertEquals("You do not have permission to add exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseAdd succeeds when permission is true")
    void requireExerciseAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.EXERCISE_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseAdd());
    }

    @Test
    @DisplayName("requireExerciseEdit throws when permission is false")
    void requireExerciseEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseEdit());
        assertEquals("You do not have permission to edit exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseEdit succeeds when permission is true")
    void requireExerciseEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.EXERCISE_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseEdit());
    }

    @Test
    @DisplayName("requireExerciseDelete throws when permission is false")
    void requireExerciseDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseDelete());
        assertEquals("You do not have permission to delete exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseDelete succeeds when permission is true")
    void requireExerciseDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.EXERCISE_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseDelete());
    }

    @Test
    @DisplayName("requireLessonAdd throws when permission is false")
    void requireLessonAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireLessonAdd());
        assertEquals("You do not have permission to add lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonAdd succeeds when permission is true")
    void requireLessonAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.LESSON_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireLessonAdd());
    }

    @Test
    @DisplayName("requireCommentAdd throws when permission is false")
    void requireCommentAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireCommentAdd());
        assertEquals("You do not have permission to add comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentAdd succeeds when permission is true")
    void requireCommentAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.COMMENT_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireCommentAdd());
    }

    @Test
    @DisplayName("requireUserAdd throws when permission is false")
    void requireUserAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserAdd());
        assertEquals("You do not have permission to add users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserAdd succeeds when permission is true")
    void requireUserAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireUserAdd());
    }

    @Test
    @DisplayName("requireUserGroupAdd throws when permission is false")
    void requireUserGroupAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserGroupAdd());
        assertEquals("You do not have permission to add user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupAdd succeeds when permission is true")
    void requireUserGroupAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_GROUP_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupAdd());
    }

    @Test
    @DisplayName("requireUserRankAdd throws when permission is false")
    void requireUserRankAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserRankAdd());
        assertEquals("You do not have permission to add user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankAdd succeeds when permission is true")
    void requireUserRankAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_RANK_ADD));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankAdd());
    }

    @Test
    @DisplayName("requireLessonEdit throws when permission is false")
    void requireLessonEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireLessonEdit());
        assertEquals("You do not have permission to edit lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonEdit succeeds when permission is true")
    void requireLessonEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.LESSON_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireLessonEdit());
    }

    @Test
    @DisplayName("requireLessonDelete throws when permission is false")
    void requireLessonDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireLessonDelete());
        assertEquals("You do not have permission to delete lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonDelete succeeds when permission is true")
    void requireLessonDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.LESSON_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireLessonDelete());
    }

    @Test
    @DisplayName("requireCommentEdit throws when permission is false")
    void requireCommentEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireCommentEdit());
        assertEquals("You do not have permission to edit comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentEdit succeeds when permission is true")
    void requireCommentEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.COMMENT_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireCommentEdit());
    }

    @Test
    @DisplayName("requireCommentDelete throws when permission is false")
    void requireCommentDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireCommentDelete());
        assertEquals("You do not have permission to delete comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentDelete succeeds when permission is true")
    void requireCommentDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.COMMENT_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireCommentDelete());
    }

    @Test
    @DisplayName("requireUserEdit throws when permission is false")
    void requireUserEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserEdit());
        assertEquals("You do not have permission to edit users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserEdit succeeds when permission is true")
    void requireUserEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireUserEdit());
    }

    @Test
    @DisplayName("requireUserDelete throws when permission is false")
    void requireUserDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserDelete());
        assertEquals("You do not have permission to delete users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserDelete succeeds when permission is true")
    void requireUserDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireUserDelete());
    }

    @Test
    @DisplayName("requireUserGroupEdit throws when permission is false")
    void requireUserGroupEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserGroupEdit());
        assertEquals("You do not have permission to edit user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupEdit succeeds when permission is true")
    void requireUserGroupEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_GROUP_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupEdit());
    }

    @Test
    @DisplayName("requireUserGroupDelete throws when permission is false")
    void requireUserGroupDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserGroupDelete());
        assertEquals("You do not have permission to delete user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupDelete succeeds when permission is true")
    void requireUserGroupDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_GROUP_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupDelete());
    }

    @Test
    @DisplayName("requireUserRankEdit throws when permission is false")
    void requireUserRankEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserRankEdit());
        assertEquals("You do not have permission to edit user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankEdit succeeds when permission is true")
    void requireUserRankEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_RANK_EDIT));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankEdit());
    }

    @Test
    @DisplayName("requireUserRankDelete throws when permission is false")
    void requireUserRankDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank());
        final var ex =
                assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireUserRankDelete());
        assertEquals("You do not have permission to delete user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankDelete succeeds when permission is true")
    void requireUserRankDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(this.buildRank(Permission.USER_RANK_DELETE));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankDelete());
    }

    @Test
    @DisplayName("throws when user rank is null")
    void throwsWhenUserRankIsNull() {
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(null);
        final var ex = assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseAdd());
        assertEquals("You do not have permission to perform this action", ex.getMessage());
    }

    @Test
    @DisplayName("Session permission checks never traverse rank membership")
    void requirePermissionsDoesNotInitializeLazyUsers() {
        @SuppressWarnings("unchecked")
        final List<UserEntity> detachedUsers = mock(List.class);
        when(detachedUsers.size()).thenThrow(new LazyInitializationException("simulated detached users collection"));
        final var rank = this.buildRank(Permission.EXERCISE_ADD);
        rank.users = detachedUsers;
        when(this.userRankService.getCurrentUserRankEntity()).thenReturn(rank);

        assertDoesNotThrow(() -> this.permissionService.requireExerciseAdd());
        assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireExerciseEdit());
        verifyNoInteractions(detachedUsers);
    }

    @Test
    @DisplayName("requireAiConfigEdit reads the permission flag without touching the lazy users collection")
    void requireAiConfigEditDoesNotInitializeLazyUsers() {
        // Regression: requireAiConfigEdit runs in the non-transactional, possibly background-thread pre-DNS
        // check in AiConfigService. Building a UserRankViewDto there used to read the lazy `users`
        // collection and fail with a LazyInitializationException on the detached rank. Simulate that
        // detached collection and assert the authorization check no longer touches it.
        @SuppressWarnings("unchecked")
        final List<UserEntity> detachedUsers = mock(List.class);
        when(detachedUsers.size()).thenThrow(new LazyInitializationException("simulated detached users collection"));

        final var rank = new UserRankEntity();
        rank.aiConfigEdit = true;
        rank.users = detachedUsers;
        final var user = new UserEntity();
        user.activated = true;
        user.rank = rank;
        when(this.userRepository.findById(1L)).thenReturn(user);

        final var result = assertDoesNotThrow(() -> this.permissionService.requireAiConfigEdit(1L));
        assertSame(user, result);
    }

    @Test
    @DisplayName("requireAiConfigEdit throws when the rank lacks the permission")
    void requireAiConfigEditThrowsWhenNotPermitted() {
        final var rank = new UserRankEntity();
        rank.aiConfigEdit = false;
        final var user = new UserEntity();
        user.activated = true;
        user.rank = rank;
        when(this.userRepository.findById(2L)).thenReturn(user);

        assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireAiConfigEdit(2L));
    }

    @ParameterizedTest(name = "banned={0}, activated={1}")
    @CsvSource({ "true, true", "false, false" })
    @DisplayName("requireAiConfigEdit throws for a banned or deactivated user whose rank holds the permission")
    void requireAiConfigEditThrowsWhenBannedOrInactive(final boolean banned, final boolean activated) {
        final var rank = new UserRankEntity();
        rank.aiConfigEdit = true;
        final var user = new UserEntity();
        user.banned = banned;
        user.activated = activated;
        user.rank = rank;
        when(this.userRepository.findById(4L)).thenReturn(user);

        assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireAiConfigEdit(4L));
    }

    @Test
    @DisplayName("requireAiConfigEdit throws when the user does not exist")
    void requireAiConfigEditThrowsWhenUserMissing() {
        when(this.userRepository.findById(3L)).thenReturn(null);
        assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireAiConfigEdit(3L));
    }

    @Test
    @DisplayName("requireAiConfigEdit throws when the user has a null rank")
    void requireAiConfigEditThrowsWhenRankIsNull() {
        final var user = new UserEntity();
        user.rank = null;
        when(this.userRepository.findById(3L)).thenReturn(user);
        assertThrows(PermissionDeniedException.class, () -> this.permissionService.requireAiConfigEdit(3L));
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
        final var rank = this.buildRank();
        rank.adminView = true;
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
        final var rank = this.buildRank();
        rank.adminView = true;
        final var user = this.stubUser(rank);
        final var stale = new SessionCredentials(user.publicId, "stale", UUID.randomUUID().toString());

        final var ex = assertThrows(ValidationException.class, () -> this.permissionService.requireAdminView(stale));
        assertEquals(AppConstants.SESSION_ENDED_MESSAGE, ex.getMessage());
        assertThrows(ValidationException.class, () -> this.permissionService.requireAdminView(null));
    }

    @Test
    @DisplayName("requireAdminView refuses a caller with no transaction")
    void requireAdminViewRequiresTransaction() {
        final var rank = this.buildRank();
        rank.adminView = true;
        final var user = this.stubUser(rank);

        assertThrows(TransactionalException.class, () -> this.permissionService.requireAdminView(credentialsFor(user)));
    }
}
