package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserRankDtoTest {

    private UserRankDto userRankDto;

    @BeforeEach
    void setUp() {
        this.userRankDto = new UserRankDto();
    }

    @Test
    @DisplayName("Should create UserRankDto with default constructor")
    void shouldCreateUserRankDtoWithDefaultConstructor() {
        // Then - All fields should be null by default
        assertNull(this.userRankDto.id);
        assertNull(this.userRankDto.name);
        assertNull(this.userRankDto.adminView);
        assertNull(this.userRankDto.exerciseAdd);
        assertNull(this.userRankDto.exerciseDelete);
        assertNull(this.userRankDto.exerciseEdit);
        assertNull(this.userRankDto.lessonAdd);
        assertNull(this.userRankDto.lessonDelete);
        assertNull(this.userRankDto.lessonEdit);
        assertNull(this.userRankDto.commentAdd);
        assertNull(this.userRankDto.commentDelete);
        assertNull(this.userRankDto.commentEdit);
        assertNull(this.userRankDto.userAdd);
        assertNull(this.userRankDto.userDelete);
        assertNull(this.userRankDto.userEdit);
        assertNull(this.userRankDto.userGroupAdd);
        assertNull(this.userRankDto.userGroupDelete);
        assertNull(this.userRankDto.userGroupEdit);
        assertNull(this.userRankDto.userRankAdd);
        assertNull(this.userRankDto.userRankDelete);
        assertNull(this.userRankDto.userRankEdit);
    }

    @Test
    @DisplayName("Should create UserRankDto with name constructor")
    void shouldCreateUserRankDtoWithNameConstructor() {
        // When
        final UserRankDto dto = new UserRankDto("Administrator");

        // Then
        assertEquals("Administrator", dto.name);
        assertNull(dto.id);
        // All permissions should still be null
        assertNull(dto.adminView);
        assertNull(dto.exerciseAdd);
    }

    @Test
    @DisplayName("Should set all permission fields correctly")
    void shouldSetAllPermissionFieldsCorrectly() {
        // When - Set all permissions to true
        this.userRankDto.id = 1L;
        this.userRankDto.name = "Super Admin";
        this.userRankDto.adminView = true;
        this.userRankDto.exerciseAdd = true;
        this.userRankDto.exerciseDelete = true;
        this.userRankDto.exerciseEdit = true;
        this.userRankDto.lessonAdd = true;
        this.userRankDto.lessonDelete = true;
        this.userRankDto.lessonEdit = true;
        this.userRankDto.commentAdd = true;
        this.userRankDto.commentDelete = true;
        this.userRankDto.commentEdit = true;
        this.userRankDto.userAdd = true;
        this.userRankDto.userDelete = true;
        this.userRankDto.userEdit = true;
        this.userRankDto.userGroupAdd = true;
        this.userRankDto.userGroupDelete = true;
        this.userRankDto.userGroupEdit = true;
        this.userRankDto.userRankAdd = true;
        this.userRankDto.userRankDelete = true;
        this.userRankDto.userRankEdit = true;

        // Then - All permissions should be true
        assertEquals(1L, this.userRankDto.id);
        assertEquals("Super Admin", this.userRankDto.name);
        assertTrue(this.userRankDto.adminView);
        assertTrue(this.userRankDto.exerciseAdd);
        assertTrue(this.userRankDto.exerciseDelete);
        assertTrue(this.userRankDto.exerciseEdit);
        assertTrue(this.userRankDto.lessonAdd);
        assertTrue(this.userRankDto.lessonDelete);
        assertTrue(this.userRankDto.lessonEdit);
        assertTrue(this.userRankDto.commentAdd);
        assertTrue(this.userRankDto.commentDelete);
        assertTrue(this.userRankDto.commentEdit);
        assertTrue(this.userRankDto.userAdd);
        assertTrue(this.userRankDto.userDelete);
        assertTrue(this.userRankDto.userEdit);
        assertTrue(this.userRankDto.userGroupAdd);
        assertTrue(this.userRankDto.userGroupDelete);
        assertTrue(this.userRankDto.userGroupEdit);
        assertTrue(this.userRankDto.userRankAdd);
        assertTrue(this.userRankDto.userRankDelete);
        assertTrue(this.userRankDto.userRankEdit);
    }

    @Test
    @DisplayName("Should create read-only user rank")
    void shouldCreateReadOnlyUserRank() {
        // When - Create a read-only rank
        this.userRankDto.name = "Viewer";
        this.userRankDto.adminView = false;
        // Leave all other permissions as null/false

        // Then
        assertEquals("Viewer", this.userRankDto.name);
        assertFalse(this.userRankDto.adminView);
        assertNull(this.userRankDto.exerciseAdd);
        assertNull(this.userRankDto.userAdd);
        assertNull(this.userRankDto.userDelete);
    }

    @Test
    @DisplayName("Should create moderator user rank")
    void shouldCreateModeratorUserRank() {
        // When - Create a moderator rank
        this.userRankDto.name = "Moderator";
        this.userRankDto.adminView = true;
        this.userRankDto.exerciseEdit = true;
        this.userRankDto.exerciseDelete = true;
        this.userRankDto.commentEdit = true;
        this.userRankDto.commentDelete = true;
        // User management permissions remain null/false

        // Then
        assertEquals("Moderator", this.userRankDto.name);
        assertTrue(this.userRankDto.adminView);
        assertTrue(this.userRankDto.exerciseEdit);
        assertTrue(this.userRankDto.exerciseDelete);
        assertTrue(this.userRankDto.commentEdit);
        assertTrue(this.userRankDto.commentDelete);
        assertNull(this.userRankDto.userAdd);
        assertNull(this.userRankDto.userDelete);
        assertNull(this.userRankDto.userRankEdit);
    }

    @Test
    @DisplayName("Should create content creator rank")
    void shouldCreateContentCreatorRank() {
        // When - Create a content creator rank
        this.userRankDto.name = "Author";
        this.userRankDto.exerciseAdd = true;
        this.userRankDto.exerciseEdit = true;
        this.userRankDto.commentAdd = true;
        this.userRankDto.commentEdit = true;
        // No delete or admin permissions

        // Then
        assertEquals("Author", this.userRankDto.name);
        assertTrue(this.userRankDto.exerciseAdd);
        assertTrue(this.userRankDto.exerciseEdit);
        assertTrue(this.userRankDto.commentAdd);
        assertTrue(this.userRankDto.commentEdit);
        assertNull(this.userRankDto.exerciseDelete);
        assertNull(this.userRankDto.adminView);
        assertNull(this.userRankDto.userAdd);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        this.userRankDto.name = null;

        // Then
        assertNull(this.userRankDto.name);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        this.userRankDto.name = "";

        // Then
        assertEquals("", this.userRankDto.name);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        final String specialName = "Admin-Level_1 & Manager (2024)";

        // When
        this.userRankDto.name = specialName;

        // Then
        assertEquals(specialName, this.userRankDto.name);
    }

    @Test
    @DisplayName("Should handle mixed boolean values")
    void shouldHandleMixedBooleanValues() {
        // When - Set mixed permissions
        this.userRankDto.adminView = true;
        this.userRankDto.exerciseAdd = false;
        this.userRankDto.exerciseEdit = true;
        this.userRankDto.exerciseDelete = false;
        this.userRankDto.userAdd = null;

        // Then
        assertTrue(this.userRankDto.adminView);
        assertFalse(this.userRankDto.exerciseAdd);
        assertTrue(this.userRankDto.exerciseEdit);
        assertFalse(this.userRankDto.exerciseDelete);
        assertNull(this.userRankDto.userAdd);
    }

    @Test
    @DisplayName("Should maintain permission independence")
    void shouldMaintainPermissionIndependence() {
        // When - Change one permission
        this.userRankDto.exerciseAdd = true;

        // Then - Other permissions should remain null
        assertTrue(this.userRankDto.exerciseAdd);
        assertNull(this.userRankDto.exerciseEdit);
        assertNull(this.userRankDto.exerciseDelete);
        assertNull(this.userRankDto.userAdd);

        // When - Change another permission
        this.userRankDto.userEdit = false;

        // Then - Previous permission should remain unchanged
        assertTrue(this.userRankDto.exerciseAdd);
        assertFalse(this.userRankDto.userEdit);
        assertNull(this.userRankDto.exerciseEdit);
    }

    @Test
    @DisplayName("Should handle all permission categories")
    void shouldHandleAllPermissionCategories() {
        // View permissions
        this.userRankDto.adminView = true;

        // Exercise permissions
        this.userRankDto.exerciseAdd = true;
        this.userRankDto.exerciseDelete = true;
        this.userRankDto.exerciseEdit = true;

        // Lesson permissions
        this.userRankDto.lessonAdd = false;
        this.userRankDto.lessonDelete = false;
        this.userRankDto.lessonEdit = true;

        // Comment permissions
        this.userRankDto.commentAdd = true;
        this.userRankDto.commentDelete = true;
        this.userRankDto.commentEdit = true;

        // User permissions
        this.userRankDto.userAdd = false;
        this.userRankDto.userDelete = false;
        this.userRankDto.userEdit = true;

        // User group permissions
        this.userRankDto.userGroupAdd = true;
        this.userRankDto.userGroupDelete = false;
        this.userRankDto.userGroupEdit = true;

        // User rank permissions
        this.userRankDto.userRankAdd = false;
        this.userRankDto.userRankDelete = false;
        this.userRankDto.userRankEdit = false;

        // Then - Verify all permissions are set correctly
        assertTrue(this.userRankDto.adminView);
        assertTrue(this.userRankDto.exerciseAdd);
        assertTrue(this.userRankDto.exerciseDelete);
        assertTrue(this.userRankDto.exerciseEdit);
        assertFalse(this.userRankDto.lessonAdd);
        assertFalse(this.userRankDto.lessonDelete);
        assertTrue(this.userRankDto.lessonEdit);
        assertTrue(this.userRankDto.commentAdd);
        assertTrue(this.userRankDto.commentDelete);
        assertTrue(this.userRankDto.commentEdit);
        assertFalse(this.userRankDto.userAdd);
        assertFalse(this.userRankDto.userDelete);
        assertTrue(this.userRankDto.userEdit);
        assertTrue(this.userRankDto.userGroupAdd);
        assertFalse(this.userRankDto.userGroupDelete);
        assertTrue(this.userRankDto.userGroupEdit);
        assertFalse(this.userRankDto.userRankAdd);
        assertFalse(this.userRankDto.userRankDelete);
        assertFalse(this.userRankDto.userRankEdit);
    }
}