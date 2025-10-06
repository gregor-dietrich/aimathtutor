package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserRankDtoTest {

    private UserRankDto userRankDto;

    @BeforeEach
    void setUp() {
        userRankDto = new UserRankDto();
    }

    @Test
    @DisplayName("Should create UserRankDto with default constructor")
    void shouldCreateUserRankDtoWithDefaultConstructor() {
        // Then - All fields should be null by default
        assertNull(userRankDto.id);
        assertNull(userRankDto.name);
        assertNull(userRankDto.adminView);
        assertNull(userRankDto.pageAdd);
        assertNull(userRankDto.pageDelete);
        assertNull(userRankDto.pageEdit);
        assertNull(userRankDto.postAdd);
        assertNull(userRankDto.postDelete);
        assertNull(userRankDto.postEdit);
        assertNull(userRankDto.postCategoryAdd);
        assertNull(userRankDto.postCategoryDelete);
        assertNull(userRankDto.postCategoryEdit);
        assertNull(userRankDto.postCommentAdd);
        assertNull(userRankDto.postCommentDelete);
        assertNull(userRankDto.postCommentEdit);
        assertNull(userRankDto.userAdd);
        assertNull(userRankDto.userDelete);
        assertNull(userRankDto.userEdit);
        assertNull(userRankDto.userGroupAdd);
        assertNull(userRankDto.userGroupDelete);
        assertNull(userRankDto.userGroupEdit);
        assertNull(userRankDto.userAccountAdd);
        assertNull(userRankDto.userAccountDelete);
        assertNull(userRankDto.userAccountEdit);
        assertNull(userRankDto.userRankAdd);
        assertNull(userRankDto.userRankDelete);
        assertNull(userRankDto.userRankEdit);
    }

    @Test
    @DisplayName("Should create UserRankDto with name constructor")
    void shouldCreateUserRankDtoWithNameConstructor() {
        // When
        UserRankDto dto = new UserRankDto("Administrator");

        // Then
        assertEquals("Administrator", dto.name);
        assertNull(dto.id);
        // All permissions should still be null
        assertNull(dto.adminView);
        assertNull(dto.postAdd);
    }

    @Test
    @DisplayName("Should set all permission fields correctly")
    void shouldSetAllPermissionFieldsCorrectly() {
        // When - Set all permissions to true
        userRankDto.id = 1L;
        userRankDto.name = "Super Admin";
        userRankDto.adminView = true;
        userRankDto.pageAdd = true;
        userRankDto.pageDelete = true;
        userRankDto.pageEdit = true;
        userRankDto.postAdd = true;
        userRankDto.postDelete = true;
        userRankDto.postEdit = true;
        userRankDto.postCategoryAdd = true;
        userRankDto.postCategoryDelete = true;
        userRankDto.postCategoryEdit = true;
        userRankDto.postCommentAdd = true;
        userRankDto.postCommentDelete = true;
        userRankDto.postCommentEdit = true;
        userRankDto.userAdd = true;
        userRankDto.userDelete = true;
        userRankDto.userEdit = true;
        userRankDto.userGroupAdd = true;
        userRankDto.userGroupDelete = true;
        userRankDto.userGroupEdit = true;
        userRankDto.userAccountAdd = true;
        userRankDto.userAccountDelete = true;
        userRankDto.userAccountEdit = true;
        userRankDto.userRankAdd = true;
        userRankDto.userRankDelete = true;
        userRankDto.userRankEdit = true;

        // Then - All permissions should be true
        assertEquals(1L, userRankDto.id);
        assertEquals("Super Admin", userRankDto.name);
        assertTrue(userRankDto.adminView);
        assertTrue(userRankDto.pageAdd);
        assertTrue(userRankDto.pageDelete);
        assertTrue(userRankDto.pageEdit);
        assertTrue(userRankDto.postAdd);
        assertTrue(userRankDto.postDelete);
        assertTrue(userRankDto.postEdit);
        assertTrue(userRankDto.postCategoryAdd);
        assertTrue(userRankDto.postCategoryDelete);
        assertTrue(userRankDto.postCategoryEdit);
        assertTrue(userRankDto.postCommentAdd);
        assertTrue(userRankDto.postCommentDelete);
        assertTrue(userRankDto.postCommentEdit);
        assertTrue(userRankDto.userAdd);
        assertTrue(userRankDto.userDelete);
        assertTrue(userRankDto.userEdit);
        assertTrue(userRankDto.userGroupAdd);
        assertTrue(userRankDto.userGroupDelete);
        assertTrue(userRankDto.userGroupEdit);
        assertTrue(userRankDto.userAccountAdd);
        assertTrue(userRankDto.userAccountDelete);
        assertTrue(userRankDto.userAccountEdit);
        assertTrue(userRankDto.userRankAdd);
        assertTrue(userRankDto.userRankDelete);
        assertTrue(userRankDto.userRankEdit);
    }

    @Test
    @DisplayName("Should create read-only user rank")
    void shouldCreateReadOnlyUserRank() {
        // When - Create a read-only rank
        userRankDto.name = "Viewer";
        userRankDto.adminView = false;
        // Leave all other permissions as null/false

        // Then
        assertEquals("Viewer", userRankDto.name);
        assertFalse(userRankDto.adminView);
        assertNull(userRankDto.postAdd);
        assertNull(userRankDto.userAdd);
        assertNull(userRankDto.userDelete);
    }

    @Test
    @DisplayName("Should create moderator user rank")
    void shouldCreateModeratorUserRank() {
        // When - Create a moderator rank
        userRankDto.name = "Moderator";
        userRankDto.adminView = true;
        userRankDto.postEdit = true;
        userRankDto.postDelete = true;
        userRankDto.postCommentEdit = true;
        userRankDto.postCommentDelete = true;
        // User management permissions remain null/false

        // Then
        assertEquals("Moderator", userRankDto.name);
        assertTrue(userRankDto.adminView);
        assertTrue(userRankDto.postEdit);
        assertTrue(userRankDto.postDelete);
        assertTrue(userRankDto.postCommentEdit);
        assertTrue(userRankDto.postCommentDelete);
        assertNull(userRankDto.userAdd);
        assertNull(userRankDto.userDelete);
        assertNull(userRankDto.userRankEdit);
    }

    @Test
    @DisplayName("Should create content creator rank")
    void shouldCreateContentCreatorRank() {
        // When - Create a content creator rank
        userRankDto.name = "Author";
        userRankDto.postAdd = true;
        userRankDto.postEdit = true;
        userRankDto.postCommentAdd = true;
        userRankDto.postCommentEdit = true;
        // No delete or admin permissions

        // Then
        assertEquals("Author", userRankDto.name);
        assertTrue(userRankDto.postAdd);
        assertTrue(userRankDto.postEdit);
        assertTrue(userRankDto.postCommentAdd);
        assertTrue(userRankDto.postCommentEdit);
        assertNull(userRankDto.postDelete);
        assertNull(userRankDto.adminView);
        assertNull(userRankDto.userAdd);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        userRankDto.name = null;

        // Then
        assertNull(userRankDto.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        userRankDto.name = "";

        // Then
        assertEquals("", userRankDto.name);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        String specialName = "Admin-Level_1 & Manager (2024)";

        // When
        userRankDto.name = specialName;

        // Then
        assertEquals(specialName, userRankDto.name);
    }

    @Test
    @DisplayName("Should handle mixed boolean values")
    void shouldHandleMixedBooleanValues() {
        // When - Set mixed permissions
        userRankDto.adminView = true;
        userRankDto.postAdd = false;
        userRankDto.postEdit = true;
        userRankDto.postDelete = false;
        userRankDto.userAdd = null;

        // Then
        assertTrue(userRankDto.adminView);
        assertFalse(userRankDto.postAdd);
        assertTrue(userRankDto.postEdit);
        assertFalse(userRankDto.postDelete);
        assertNull(userRankDto.userAdd);
    }

    @Test
    @DisplayName("Should maintain permission independence")
    void shouldMaintainPermissionIndependence() {
        // When - Change one permission
        userRankDto.postAdd = true;

        // Then - Other permissions should remain null
        assertTrue(userRankDto.postAdd);
        assertNull(userRankDto.postEdit);
        assertNull(userRankDto.postDelete);
        assertNull(userRankDto.userAdd);

        // When - Change another permission
        userRankDto.userEdit = false;

        // Then - Previous permission should remain unchanged
        assertTrue(userRankDto.postAdd);
        assertFalse(userRankDto.userEdit);
        assertNull(userRankDto.postEdit);
    }

    @Test
    @DisplayName("Should handle all permission categories")
    void shouldHandleAllPermissionCategories() {
        // View permissions
        userRankDto.adminView = true;

        // Page permissions
        userRankDto.pageAdd = true;
        userRankDto.pageDelete = false;
        userRankDto.pageEdit = true;

        // Post permissions
        userRankDto.postAdd = true;
        userRankDto.postDelete = true;
        userRankDto.postEdit = true;

        // Post category permissions
        userRankDto.postCategoryAdd = false;
        userRankDto.postCategoryDelete = false;
        userRankDto.postCategoryEdit = true;

        // Post comment permissions
        userRankDto.postCommentAdd = true;
        userRankDto.postCommentDelete = true;
        userRankDto.postCommentEdit = true;

        // User permissions
        userRankDto.userAdd = false;
        userRankDto.userDelete = false;
        userRankDto.userEdit = true;

        // User group permissions
        userRankDto.userGroupAdd = true;
        userRankDto.userGroupDelete = false;
        userRankDto.userGroupEdit = true;

        // Account permissions
        userRankDto.userAccountAdd = false;
        userRankDto.userAccountDelete = false;
        userRankDto.userAccountEdit = true;

        // User rank permissions
        userRankDto.userRankAdd = false;
        userRankDto.userRankDelete = false;
        userRankDto.userRankEdit = false;

        // Then - Verify all permissions are set correctly
        assertTrue(userRankDto.adminView);
        assertTrue(userRankDto.pageAdd);
        assertFalse(userRankDto.pageDelete);
        assertTrue(userRankDto.pageEdit);
        assertTrue(userRankDto.postAdd);
        assertTrue(userRankDto.postDelete);
        assertTrue(userRankDto.postEdit);
        assertFalse(userRankDto.postCategoryAdd);
        assertFalse(userRankDto.postCategoryDelete);
        assertTrue(userRankDto.postCategoryEdit);
        assertTrue(userRankDto.postCommentAdd);
        assertTrue(userRankDto.postCommentDelete);
        assertTrue(userRankDto.postCommentEdit);
        assertFalse(userRankDto.userAdd);
        assertFalse(userRankDto.userDelete);
        assertTrue(userRankDto.userEdit);
        assertTrue(userRankDto.userGroupAdd);
        assertFalse(userRankDto.userGroupDelete);
        assertTrue(userRankDto.userGroupEdit);
        assertFalse(userRankDto.userAccountAdd);
        assertFalse(userRankDto.userAccountDelete);
        assertTrue(userRankDto.userAccountEdit);
        assertFalse(userRankDto.userRankAdd);
        assertFalse(userRankDto.userRankDelete);
        assertFalse(userRankDto.userRankEdit);
    }
}