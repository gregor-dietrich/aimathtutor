package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;

class UserRankEntityTest {

    private UserRankEntity userRank;

    @BeforeEach
    void setUp() {
        userRank = new UserRankEntity();
    }

    @Test
    @DisplayName("Should create UserRankEntity with all fields")
    void shouldCreateUserRankEntityWithAllFields() {
        // Given
        List<UserEntity> users = new ArrayList<>();

        // When
        userRank.id = 1L;
        userRank.name = "Administrator";
        userRank.adminView = true;
        userRank.pageAdd = true;
        userRank.pageDelete = true;
        userRank.pageEdit = true;
        userRank.postAdd = true;
        userRank.postDelete = true;
        userRank.postEdit = true;
        userRank.postCategoryAdd = true;
        userRank.postCategoryDelete = true;
        userRank.postCategoryEdit = true;
        userRank.postCommentAdd = true;
        userRank.postCommentDelete = true;
        userRank.postCommentEdit = true;
        userRank.userAdd = true;
        userRank.userDelete = true;
        userRank.userEdit = true;
        userRank.userGroupAdd = true;
        userRank.userGroupDelete = true;
        userRank.userGroupEdit = true;
        userRank.userAccountAdd = true;
        userRank.userAccountDelete = true;
        userRank.userAccountEdit = true;
        userRank.userRankAdd = true;
        userRank.userRankDelete = true;
        userRank.userRankEdit = true;
        userRank.users = users;

        // Then
        assertEquals(1L, userRank.id);
        assertEquals("Administrator", userRank.name);
        assertTrue(userRank.adminView);
        assertTrue(userRank.pageAdd);
        assertTrue(userRank.pageDelete);
        assertTrue(userRank.pageEdit);
        assertTrue(userRank.postAdd);
        assertTrue(userRank.postDelete);
        assertTrue(userRank.postEdit);
        assertTrue(userRank.postCategoryAdd);
        assertTrue(userRank.postCategoryDelete);
        assertTrue(userRank.postCategoryEdit);
        assertTrue(userRank.postCommentAdd);
        assertTrue(userRank.postCommentDelete);
        assertTrue(userRank.postCommentEdit);
        assertTrue(userRank.userAdd);
        assertTrue(userRank.userDelete);
        assertTrue(userRank.userEdit);
        assertTrue(userRank.userGroupAdd);
        assertTrue(userRank.userGroupDelete);
        assertTrue(userRank.userGroupEdit);
        assertTrue(userRank.userAccountAdd);
        assertTrue(userRank.userAccountDelete);
        assertTrue(userRank.userAccountEdit);
        assertTrue(userRank.userRankAdd);
        assertTrue(userRank.userRankDelete);
        assertTrue(userRank.userRankEdit);
        assertEquals(users, userRank.users);
    }

    @Test
    @DisplayName("Should have default false values for all permissions")
    void shouldHaveDefaultFalseValuesForAllPermissions() {
        // Then
        assertFalse(userRank.adminView);
        assertFalse(userRank.pageAdd);
        assertFalse(userRank.pageDelete);
        assertFalse(userRank.pageEdit);
        assertFalse(userRank.postAdd);
        assertFalse(userRank.postDelete);
        assertFalse(userRank.postEdit);
        assertFalse(userRank.postCategoryAdd);
        assertFalse(userRank.postCategoryDelete);
        assertFalse(userRank.postCategoryEdit);
        assertFalse(userRank.postCommentAdd);
        assertFalse(userRank.postCommentDelete);
        assertFalse(userRank.postCommentEdit);
        assertFalse(userRank.userAdd);
        assertFalse(userRank.userDelete);
        assertFalse(userRank.userEdit);
        assertFalse(userRank.userGroupAdd);
        assertFalse(userRank.userGroupDelete);
        assertFalse(userRank.userGroupEdit);
        assertFalse(userRank.userAccountAdd);
        assertFalse(userRank.userAccountDelete);
        assertFalse(userRank.userAccountEdit);
        assertFalse(userRank.userRankAdd);
        assertFalse(userRank.userRankDelete);
        assertFalse(userRank.userRankEdit);
    }

    @Test
    @DisplayName("Should create read-only user rank")
    void shouldCreateReadOnlyUserRank() {
        // When
        userRank.name = "Viewer";
        userRank.adminView = false;
        // All other permissions remain false by default

        // Then
        assertEquals("Viewer", userRank.name);
        assertFalse(userRank.adminView);
        assertFalse(userRank.userAdd);
        assertFalse(userRank.userDelete);
        assertFalse(userRank.postAdd);
        assertFalse(userRank.postDelete);
    }

    @Test
    @DisplayName("Should create moderator user rank")
    void shouldCreateModeratorUserRank() {
        // When
        userRank.name = "Moderator";
        userRank.adminView = true;
        userRank.postEdit = true;
        userRank.postDelete = true;
        userRank.postCommentEdit = true;
        userRank.postCommentDelete = true;

        // Then
        assertEquals("Moderator", userRank.name);
        assertTrue(userRank.adminView);
        assertTrue(userRank.postEdit);
        assertTrue(userRank.postDelete);
        assertTrue(userRank.postCommentEdit);
        assertTrue(userRank.postCommentDelete);
        assertFalse(userRank.userAdd); // Still can't manage users
        assertFalse(userRank.userRankEdit); // Still can't edit ranks
    }

    @Test
    @DisplayName("Should handle users collection properly")
    void shouldHandleUsersCollectionProperly() {
        // Given
        List<UserEntity> users = new ArrayList<>();
        UserEntity user1 = new UserEntity();
        user1.id = 1L;
        user1.username = "admin1";

        UserEntity user2 = new UserEntity();
        user2.id = 2L;
        user2.username = "admin2";

        users.add(user1);
        users.add(user2);

        // When
        userRank.users = users;

        // Then
        assertNotNull(userRank.users);
        assertEquals(2, userRank.users.size());
        assertEquals(user1, userRank.users.get(0));
        assertEquals(user2, userRank.users.get(1));
    }

    @Test
    @DisplayName("Should handle empty users collection")
    void shouldHandleEmptyUsersCollection() {
        // When
        userRank.users = new ArrayList<>();

        // Then
        assertNotNull(userRank.users);
        assertTrue(userRank.users.isEmpty());
    }

    @Test
    @DisplayName("Should handle null users collection")
    void shouldHandleNullUsersCollection() {
        // When
        userRank.users = null;

        // Then
        assertNull(userRank.users);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        userRank.name = null;

        // Then
        assertNull(userRank.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        userRank.name = "";

        // Then
        assertEquals("", userRank.name);
    }

    @Test
    @DisplayName("Should allow individual permission setting")
    void shouldAllowIndividualPermissionSetting() {
        // When - Set only specific permissions
        userRank.postAdd = true;
        userRank.postCommentAdd = true;

        // Then
        assertTrue(userRank.postAdd);
        assertTrue(userRank.postCommentAdd);
        assertFalse(userRank.postDelete);
        assertFalse(userRank.postEdit);
        assertFalse(userRank.userAdd);
        assertFalse(userRank.adminView);
    }

    @Test
    @DisplayName("Should handle null permission values")
    void shouldHandleNullPermissionValues() {
        // When
        userRank.adminView = null;
        userRank.postAdd = null;

        // Then
        assertNull(userRank.adminView);
        assertNull(userRank.postAdd);
    }
}