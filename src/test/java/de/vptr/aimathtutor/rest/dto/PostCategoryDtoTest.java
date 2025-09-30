package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PostCategoryDtoTest {

    private PostCategoryDto categoryDto;

    @BeforeEach
    void setUp() {
        categoryDto = new PostCategoryDto();
    }

    @Test
    @DisplayName("Should create PostCategoryDto with default constructor")
    void shouldCreatePostCategoryDtoWithDefaultConstructor() {
        // Then
        assertNull(categoryDto.id);
        assertNull(categoryDto.name);
        assertNull(categoryDto.parentId);
        assertNull(categoryDto.parent);
    }

    @Test
    @DisplayName("Should set all fields correctly")
    void shouldSetAllFieldsCorrectly() {
        // When
        categoryDto.id = 1L;
        categoryDto.name = "Test Category";
        categoryDto.parentId = 2L;

        // Then
        assertEquals(1L, categoryDto.id);
        assertEquals("Test Category", categoryDto.name);
        assertEquals(2L, categoryDto.parentId);
    }

    @Test
    @DisplayName("Should sync parent field correctly")
    void shouldSyncParentFieldCorrectly() {
        // Given
        categoryDto.parentId = 1L;

        // When
        categoryDto.syncParent();

        // Then
        assertNotNull(categoryDto.parent);
        assertEquals(1L, categoryDto.parent.id);

        // Given - Set parent object
        PostCategoryDto.ParentField parentField = new PostCategoryDto.ParentField(2L);
        categoryDto.parent = parentField;
        categoryDto.parentId = null;

        // When
        categoryDto.syncParent();

        // Then
        assertEquals(2L, categoryDto.parentId);
    }

    @Test
    @DisplayName("Should handle ParentField operations")
    void shouldHandleParentFieldOperations() {
        // Test default constructor
        PostCategoryDto.ParentField parentField = new PostCategoryDto.ParentField();
        assertNull(parentField.id);

        // Test parameterized constructor
        PostCategoryDto.ParentField parentField2 = new PostCategoryDto.ParentField(1L);
        assertEquals(1L, parentField2.id);

        // Test setting fields
        parentField.id = 2L;
        assertEquals(2L, parentField.id);
    }

    @Test
    @DisplayName("Should handle null values in sync")
    void shouldHandleNullValuesInSync() {
        // Given - all null
        categoryDto.parentId = null;
        categoryDto.parent = null;

        // When
        categoryDto.syncParent();

        // Then - should remain null
        assertNull(categoryDto.parentId);
        assertNull(categoryDto.parent);
    }

    @Test
    @DisplayName("Should handle root category creation")
    void shouldHandleRootCategoryCreation() {
        // Given - root category has no parent
        categoryDto.name = "Root Category";
        categoryDto.parentId = null;

        // When
        categoryDto.syncParent();

        // Then
        assertEquals("Root Category", categoryDto.name);
        assertNull(categoryDto.parentId);
        assertNull(categoryDto.parent);
    }

    @Test
    @DisplayName("Should handle subcategory creation")
    void shouldHandleSubcategoryCreation() {
        // Given - subcategory has parent
        categoryDto.name = "Sub Category";
        categoryDto.parentId = 1L;

        // When
        categoryDto.syncParent();

        // Then
        assertEquals("Sub Category", categoryDto.name);
        assertEquals(1L, categoryDto.parentId);
        assertNotNull(categoryDto.parent);
        assertEquals(1L, categoryDto.parent.id);
    }

    @Test
    @DisplayName("Should handle empty name")
    void shouldHandleEmptyName() {
        // When
        categoryDto.name = "";

        // Then
        assertEquals("", categoryDto.name);
    }

    @Test
    @DisplayName("Should handle null name")
    void shouldHandleNullName() {
        // When
        categoryDto.name = null;

        // Then
        assertNull(categoryDto.name);
    }

    @Test
    @DisplayName("Should handle special characters in name")
    void shouldHandleSpecialCharactersInName() {
        // Given
        String specialName = "Category with Special-Chars_123 & émojis 🚀";

        // When
        categoryDto.name = specialName;

        // Then
        assertEquals(specialName, categoryDto.name);
    }

    @Test
    @DisplayName("Should handle long category names")
    void shouldHandleLongCategoryNames() {
        // Given
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            longName.append("c");
        }

        // When
        categoryDto.name = longName.toString();

        // Then
        assertEquals(longName.toString(), categoryDto.name);
        assertEquals(300, categoryDto.name.length());
    }

    @Test
    @DisplayName("Should handle category hierarchy scenarios")
    void shouldHandleCategoryHierarchyScenarios() {
        // Scenario 1: Creating root category
        PostCategoryDto rootCategory = new PostCategoryDto();
        rootCategory.id = 1L;
        rootCategory.name = "Root";
        rootCategory.parentId = null;
        rootCategory.syncParent();

        assertNull(rootCategory.parentId);
        assertNull(rootCategory.parent);

        // Scenario 2: Creating child category
        PostCategoryDto childCategory = new PostCategoryDto();
        childCategory.id = 2L;
        childCategory.name = "Child";
        childCategory.parentId = 1L;
        childCategory.syncParent();

        assertEquals(1L, childCategory.parentId);
        assertNotNull(childCategory.parent);
        assertEquals(1L, childCategory.parent.id);

        // Scenario 3: Moving category to different parent
        childCategory.parentId = 3L;
        childCategory.parent = null; // Reset parent object
        childCategory.syncParent();

        assertEquals(3L, childCategory.parentId);
        assertEquals(3L, childCategory.parent.id);

        // Scenario 4: Making category a root category
        childCategory.parentId = null;
        childCategory.parent = null;
        childCategory.syncParent();

        assertNull(childCategory.parentId);
        assertNull(childCategory.parent);
    }

    @Test
    @DisplayName("Should handle parent field with null id")
    void shouldHandleParentFieldWithNullId() {
        // Given
        PostCategoryDto.ParentField parentField = new PostCategoryDto.ParentField(null);
        categoryDto.parent = parentField;

        // When
        categoryDto.syncParent();

        // Then - parent field exists but has null id, so parentId should remain null
        assertNotNull(categoryDto.parent);
        assertNull(categoryDto.parent.id);
        assertNull(categoryDto.parentId);
    }
}