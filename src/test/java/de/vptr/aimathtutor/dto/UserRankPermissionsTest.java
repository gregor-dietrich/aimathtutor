package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@SuppressWarnings("NullAway")
class UserRankPermissionsTest {

    @Test
    @DisplayName("All permission fields are null by default")
    void testDefaultFields() {
        final var permissions = new UserRankDto();
        assertNull(permissions.publicId);
        assertNull(permissions.adminView);
        assertNull(permissions.exerciseAdd);
        assertNull(permissions.exerciseEdit);
        assertNull(permissions.exerciseDelete);
        assertNull(permissions.lessonAdd);
        assertNull(permissions.lessonEdit);
        assertNull(permissions.lessonDelete);
        assertNull(permissions.commentAdd);
        assertNull(permissions.commentEdit);
        assertNull(permissions.commentDelete);
        assertNull(permissions.userAdd);
        assertNull(permissions.userEdit);
        assertNull(permissions.userDelete);
        assertNull(permissions.userGroupAdd);
        assertNull(permissions.userGroupEdit);
        assertNull(permissions.userGroupDelete);
        assertNull(permissions.userRankAdd);
        assertNull(permissions.userRankEdit);
        assertNull(permissions.userRankDelete);
    }
}
