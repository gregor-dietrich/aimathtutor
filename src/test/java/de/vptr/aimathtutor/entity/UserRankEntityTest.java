package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserRankEntityTest {

    private UserRankEntity userRank;

    @BeforeEach
    void setUp() {
        this.userRank = new UserRankEntity();
    }

    @Test
    @DisplayName("Should create UserRankEntity with all fields")
    void shouldCreateUserRankEntityWithAllFields() {
        // Given
        final List<UserEntity> users = new ArrayList<>();

        // When
        this.userRank.id = 1L;
        this.userRank.name = "Administrator";
        this.userRank.adminView = true;
        this.userRank.exerciseAdd = true;
        this.userRank.exerciseDelete = true;
        this.userRank.exerciseEdit = true;
        this.userRank.lessonAdd = true;
        this.userRank.lessonDelete = true;
        this.userRank.lessonEdit = true;
        this.userRank.commentAdd = true;
        this.userRank.commentDelete = true;
        this.userRank.commentEdit = true;
        this.userRank.userAdd = true;
        this.userRank.userDelete = true;
        this.userRank.userEdit = true;
        this.userRank.userGroupAdd = true;
        this.userRank.userGroupDelete = true;
        this.userRank.userGroupEdit = true;
        this.userRank.userRankAdd = true;
        this.userRank.userRankDelete = true;
        this.userRank.userRankEdit = true;
        this.userRank.users = users;

        // Then
        assertEquals(1L, this.userRank.id);
        assertEquals("Administrator", this.userRank.name);
        assertTrue(this.userRank.adminView);
        assertTrue(this.userRank.exerciseAdd);
        assertTrue(this.userRank.exerciseDelete);
        assertTrue(this.userRank.exerciseEdit);
        assertTrue(this.userRank.lessonAdd);
        assertTrue(this.userRank.lessonDelete);
        assertTrue(this.userRank.lessonEdit);
        assertTrue(this.userRank.commentAdd);
        assertTrue(this.userRank.commentDelete);
        assertTrue(this.userRank.commentEdit);
        assertTrue(this.userRank.userAdd);
        assertTrue(this.userRank.userDelete);
        assertTrue(this.userRank.userEdit);
        assertTrue(this.userRank.userGroupAdd);
        assertTrue(this.userRank.userGroupDelete);
        assertTrue(this.userRank.userGroupEdit);
        assertTrue(this.userRank.userRankAdd);
        assertTrue(this.userRank.userRankDelete);
        assertTrue(this.userRank.userRankEdit);
        assertEquals(users, this.userRank.users);
    }

    @Test
    @DisplayName("Should have default false values for all permissions")
    void shouldHaveDefaultFalseValuesForAllPermissions() {
        // Then
        assertFalse(this.userRank.adminView);
        assertFalse(this.userRank.exerciseAdd);
        assertFalse(this.userRank.exerciseDelete);
        assertFalse(this.userRank.exerciseEdit);
        assertFalse(this.userRank.lessonAdd);
        assertFalse(this.userRank.lessonDelete);
        assertFalse(this.userRank.lessonEdit);
        assertFalse(this.userRank.commentAdd);
        assertFalse(this.userRank.commentDelete);
        assertFalse(this.userRank.commentEdit);
        assertFalse(this.userRank.userAdd);
        assertFalse(this.userRank.userDelete);
        assertFalse(this.userRank.userEdit);
        assertFalse(this.userRank.userGroupAdd);
        assertFalse(this.userRank.userGroupDelete);
        assertFalse(this.userRank.userGroupEdit);
        assertFalse(this.userRank.userRankAdd);
        assertFalse(this.userRank.userRankDelete);
        assertFalse(this.userRank.userRankEdit);
    }

    @Test
    @DisplayName("Should create read-only user rank")
    void shouldCreateReadOnlyUserRank() {
        // When
        this.userRank.name = "Viewer";
        this.userRank.adminView = false;
        // All other permissions remain false by default

        // Then
        assertEquals("Viewer", this.userRank.name);
        assertFalse(this.userRank.adminView);
        assertFalse(this.userRank.userAdd);
        assertFalse(this.userRank.userDelete);
        assertFalse(this.userRank.exerciseAdd);
        assertFalse(this.userRank.exerciseDelete);
    }

    @Test
    @DisplayName("Should create moderator user rank")
    void shouldCreateModeratorUserRank() {
        // When
        this.userRank.name = "Moderator";
        this.userRank.adminView = true;
        this.userRank.exerciseEdit = true;
        this.userRank.exerciseDelete = true;
        this.userRank.commentEdit = true;
        this.userRank.commentDelete = true;

        // Then
        assertEquals("Moderator", this.userRank.name);
        assertTrue(this.userRank.adminView);
        assertTrue(this.userRank.exerciseEdit);
        assertTrue(this.userRank.exerciseDelete);
        assertTrue(this.userRank.commentEdit);
        assertTrue(this.userRank.commentDelete);
        assertFalse(this.userRank.userAdd); // Still can't manage users
        assertFalse(this.userRank.userRankEdit); // Still can't edit ranks
    }

    @Test
    @DisplayName("Should handle users collection properly")
    void shouldHandleUsersCollectionProperly() {
        // Given
        final List<UserEntity> users = new ArrayList<>();
        final UserEntity user1 = new UserEntity();
        user1.id = 1L;
        user1.username = "admin1";

        final UserEntity user2 = new UserEntity();
        user2.id = 2L;
        user2.username = "admin2";

        users.add(user1);
        users.add(user2);

        // When
        this.userRank.users = users;

        // Then
        assertNotNull(this.userRank.users);
        assertEquals(2, this.userRank.users.size());
        assertEquals(user1, this.userRank.users.get(0));
        assertEquals(user2, this.userRank.users.get(1));
    }

    @Test
    @DisplayName("Should handle empty users collection")
    void shouldHandleEmptyUsersCollection() {
        // When
        this.userRank.users = new ArrayList<>();

        // Then
        assertNotNull(this.userRank.users);
        assertTrue(this.userRank.users.isEmpty());
    }

    @Test
    @DisplayName("Should handle null users collection")
    void shouldHandleNullUsersCollection() {
        // When
        this.userRank.users = null;

        // Then
        assertNull(this.userRank.users);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        this.userRank.name = null;

        // Then
        assertNull(this.userRank.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        this.userRank.name = "";

        // Then
        assertEquals("", this.userRank.name);
    }

    @Test
    @DisplayName("Should allow individual permission setting")
    void shouldAllowIndividualPermissionSetting() {
        // When - Set only specific permissions
        this.userRank.exerciseAdd = true;
        this.userRank.commentAdd = true;

        // Then
        assertTrue(this.userRank.exerciseAdd);
        assertTrue(this.userRank.commentAdd);
        assertFalse(this.userRank.exerciseDelete);
        assertFalse(this.userRank.exerciseEdit);
        assertFalse(this.userRank.userAdd);
        assertFalse(this.userRank.adminView);
    }

    @Test
    @DisplayName("Should handle null permission values")
    void shouldHandleNullPermissionValues() {
        // When
        this.userRank.adminView = null;
        this.userRank.exerciseAdd = null;

        // Then
        assertNull(this.userRank.adminView);
        assertNull(this.userRank.exerciseAdd);
    }
}