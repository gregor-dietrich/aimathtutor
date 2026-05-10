package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserRankEntity;

@SuppressWarnings("NullAway")
class UserRankViewDtoTest {

    @Test
    @DisplayName("Default constructor creates instance with null fields")
    void testDefaultConstructor() {
        final var dto = new UserRankViewDto();
        assertNull(dto.name);
        assertNull(dto.publicId);
        assertNull(dto.usersCount);
    }

    @Test
    @DisplayName("Constructor from null entity yields null fields")
    void testConstructorFromNull() {
        final var dto = new UserRankViewDto(null);
        assertNull(dto.name);
        assertNull(dto.publicId);
    }

    @Test
    @DisplayName("Constructor from entity maps all fields")
    void testConstructorFromEntity() {
        final var rank = new UserRankEntity();
        rank.publicId = "rank-123";
        rank.name = "Admin";
        rank.adminView = true;
        rank.exerciseAdd = true;
        rank.exerciseDelete = false;
        rank.exerciseEdit = true;
        rank.lessonAdd = false;
        rank.lessonDelete = false;
        rank.lessonEdit = false;
        rank.commentAdd = true;
        rank.commentDelete = true;
        rank.commentEdit = false;
        rank.userAdd = false;
        rank.userDelete = false;
        rank.userEdit = false;
        rank.userGroupAdd = false;
        rank.userGroupDelete = false;
        rank.userGroupEdit = false;
        rank.userRankAdd = false;
        rank.userRankDelete = false;
        rank.userRankEdit = false;
        rank.users = new ArrayList<>();

        final var dto = new UserRankViewDto(rank);
        assertEquals("rank-123", dto.publicId);
        assertEquals("Admin", dto.name);
        assertEquals(true, dto.adminView);
        assertEquals(true, dto.exerciseAdd);
        assertEquals(false, dto.exerciseDelete);
        assertEquals(0L, dto.usersCount);
    }

    @Test
    @DisplayName("Constructor from entity with users counts correctly")
    void testConstructorFromEntityWithUsers() {
        final var rank = new UserRankEntity();
        rank.publicId = "r1";
        rank.name = "Teacher";
        rank.users = new ArrayList<>();

        final var dto = new UserRankViewDto(rank);
        assertEquals(0L, dto.usersCount);
    }

    @Test
    @DisplayName("canAdminView returns true when adminView is true")
    void testCanAdminView() {
        final var dto = new UserRankViewDto();
        dto.adminView = true;
        assertTrue(dto.canAdminView());
        dto.adminView = false;
        assertFalse(dto.canAdminView());
        dto.adminView = null;
        assertFalse(dto.canAdminView());
    }

    @Test
    @DisplayName("hasAnyExercisePermission returns true when any exercise permission is true")
    void testHasAnyExercisePermission() {
        final var dto = new UserRankViewDto();
        assertFalse(dto.hasAnyExercisePermission());

        dto.exerciseAdd = true;
        assertTrue(dto.hasAnyExercisePermission());

        dto.exerciseAdd = null;
        dto.exerciseEdit = true;
        assertTrue(dto.hasAnyExercisePermission());
    }

    @Test
    @DisplayName("hasAnyLessonPermission returns true when any lesson permission is true")
    void testHasAnyLessonPermission() {
        final var dto = new UserRankViewDto();
        assertFalse(dto.hasAnyLessonPermission());

        dto.lessonAdd = true;
        assertTrue(dto.hasAnyLessonPermission());
    }

    @Test
    @DisplayName("hasAnyCommentPermission returns true when any comment permission is true")
    void testHasAnyCommentPermission() {
        final var dto = new UserRankViewDto();
        assertFalse(dto.hasAnyCommentPermission());

        dto.commentDelete = true;
        assertTrue(dto.hasAnyCommentPermission());
    }

    @Test
    @DisplayName("hasAnyUserPermission returns true when any user permission is true")
    void testHasAnyUserPermission() {
        final var dto = new UserRankViewDto();
        assertFalse(dto.hasAnyUserPermission());

        dto.userEdit = true;
        assertTrue(dto.hasAnyUserPermission());
    }

    @Test
    @DisplayName("hasAnyUserGroupPermission returns true when any user group permission is true")
    void testHasAnyUserGroupPermission() {
        final var dto = new UserRankViewDto();
        assertFalse(dto.hasAnyUserGroupPermission());

        dto.userGroupAdd = true;
        assertTrue(dto.hasAnyUserGroupPermission());
    }

    @Test
    @DisplayName("hasAnyUserRankPermission returns true when any user rank permission is true")
    void testHasAnyUserRankPermission() {
        final var dto = new UserRankViewDto();
        assertFalse(dto.hasAnyUserRankPermission());

        dto.userRankDelete = true;
        assertTrue(dto.hasAnyUserRankPermission());
    }

    @Test
    @DisplayName("toUserRankDto maps all fields correctly")
    void testToUserRankDto() {
        final var dto = new UserRankViewDto();
        dto.publicId = "pid-1";
        dto.name = "Student";
        dto.adminView = false;
        dto.exerciseAdd = true;
        dto.exerciseDelete = false;
        dto.exerciseEdit = true;
        dto.lessonAdd = false;
        dto.lessonDelete = false;
        dto.lessonEdit = false;
        dto.commentAdd = true;
        dto.commentDelete = true;
        dto.commentEdit = false;
        dto.userAdd = false;
        dto.userDelete = false;
        dto.userEdit = false;
        dto.userGroupAdd = false;
        dto.userGroupDelete = false;
        dto.userGroupEdit = false;
        dto.userRankAdd = false;
        dto.userRankDelete = false;
        dto.userRankEdit = false;

        final var result = dto.toUserRankDto();
        assertNotNull(result);
        assertEquals("pid-1", result.publicId);
        assertEquals("Student", result.name);
        assertEquals(true, result.exerciseAdd);
        assertEquals(true, result.exerciseEdit);
        assertEquals(true, result.commentAdd);
        assertEquals(true, result.commentDelete);
        assertEquals(false, result.adminView);
    }
}
