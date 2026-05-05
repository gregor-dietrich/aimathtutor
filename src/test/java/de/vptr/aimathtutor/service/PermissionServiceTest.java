package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserRankViewDto;
import de.vptr.aimathtutor.exception.PermissionDeniedException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PermissionServiceTest {

    @InjectMock
    UserRankService userRankService;

    @Inject
    PermissionService permissionService;

    private UserRankViewDto buildRank(final boolean... permissions) {
        final var rank = new UserRankViewDto();
        rank.exerciseAdd = permissions.length > 0 ? permissions[0] : false;
        rank.exerciseEdit = permissions.length > 1 ? permissions[1] : false;
        rank.exerciseDelete = permissions.length > 2 ? permissions[2] : false;
        rank.lessonAdd = permissions.length > 3 ? permissions[3] : false;
        rank.lessonEdit = permissions.length > 4 ? permissions[4] : false;
        rank.lessonDelete = permissions.length > 5 ? permissions[5] : false;
        rank.commentAdd = permissions.length > 6 ? permissions[6] : false;
        rank.commentEdit = permissions.length > 7 ? permissions[7] : false;
        rank.commentDelete = permissions.length > 8 ? permissions[8] : false;
        rank.userAdd = permissions.length > 9 ? permissions[9] : false;
        rank.userEdit = permissions.length > 10 ? permissions[10] : false;
        rank.userDelete = permissions.length > 11 ? permissions[11] : false;
        rank.userGroupAdd = permissions.length > 12 ? permissions[12] : false;
        rank.userGroupEdit = permissions.length > 13 ? permissions[13] : false;
        rank.userGroupDelete = permissions.length > 14 ? permissions[14] : false;
        rank.userRankAdd = permissions.length > 15 ? permissions[15] : false;
        rank.userRankEdit = permissions.length > 16 ? permissions[16] : false;
        rank.userRankDelete = permissions.length > 17 ? permissions[17] : false;
        return rank;
    }

    @Test
    @DisplayName("requireExerciseAdd throws when permission is false")
    void requireExerciseAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireExerciseAdd());
        assertEquals("You do not have permission to add exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseAdd succeeds when permission is true")
    void requireExerciseAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(true));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseAdd());
    }

    @Test
    @DisplayName("requireExerciseEdit throws when permission is false")
    void requireExerciseEditThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireExerciseEdit());
        assertEquals("You do not have permission to edit exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseEdit succeeds when permission is true")
    void requireExerciseEditSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(false, true));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseEdit());
    }

    @Test
    @DisplayName("requireExerciseDelete throws when permission is false")
    void requireExerciseDeleteThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(false, false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireExerciseDelete());
        assertEquals("You do not have permission to delete exercises", ex.getMessage());
    }

    @Test
    @DisplayName("requireExerciseDelete succeeds when permission is true")
    void requireExerciseDeleteSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(false, false, true));
        assertDoesNotThrow(() -> this.permissionService.requireExerciseDelete());
    }

    @Test
    @DisplayName("requireLessonAdd throws when permission is false")
    void requireLessonAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireLessonAdd());
        assertEquals("You do not have permission to add lessons", ex.getMessage());
    }

    @Test
    @DisplayName("requireLessonAdd succeeds when permission is true")
    void requireLessonAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, true));
        assertDoesNotThrow(() -> this.permissionService.requireLessonAdd());
    }

    @Test
    @DisplayName("requireCommentAdd throws when permission is false")
    void requireCommentAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireCommentAdd());
        assertEquals("You do not have permission to add comments", ex.getMessage());
    }

    @Test
    @DisplayName("requireCommentAdd succeeds when permission is true")
    void requireCommentAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, true));
        assertDoesNotThrow(() -> this.permissionService.requireCommentAdd());
    }

    @Test
    @DisplayName("requireUserAdd throws when permission is false")
    void requireUserAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false, false, false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireUserAdd());
        assertEquals("You do not have permission to add users", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserAdd succeeds when permission is true")
    void requireUserAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false, false, false, true));
        assertDoesNotThrow(() -> this.permissionService.requireUserAdd());
    }

    @Test
    @DisplayName("requireUserGroupAdd throws when permission is false")
    void requireUserGroupAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false, false, false,
                false, false, false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireUserGroupAdd());
        assertEquals("You do not have permission to add user groups", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserGroupAdd succeeds when permission is true")
    void requireUserGroupAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false, false, false,
                false, false, false, true));
        assertDoesNotThrow(() -> this.permissionService.requireUserGroupAdd());
    }

    @Test
    @DisplayName("requireUserRankAdd throws when permission is false")
    void requireUserRankAddThrowsWhenFalse() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, false));
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireUserRankAdd());
        assertEquals("You do not have permission to add user ranks", ex.getMessage());
    }

    @Test
    @DisplayName("requireUserRankAdd succeeds when permission is true")
    void requireUserRankAddSucceedsWhenTrue() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(this.buildRank(
                false, false, false, false, false, false, false, false, false,
                false, false, false, false, false, false, true));
        assertDoesNotThrow(() -> this.permissionService.requireUserRankAdd());
    }

    @Test
    @DisplayName("throws when user rank is null")
    void throwsWhenUserRankIsNull() {
        when(this.userRankService.getCurrentUserRank()).thenReturn(null);
        final var ex = assertThrows(PermissionDeniedException.class,
                () -> this.permissionService.requireExerciseAdd());
        assertEquals("You do not have permission to perform this action", ex.getMessage());
    }
}
