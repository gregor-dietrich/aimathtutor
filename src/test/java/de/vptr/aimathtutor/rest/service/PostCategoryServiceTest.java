package de.vptr.aimathtutor.rest.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
class PostCategoryServiceTest {

    @InjectMocks
    private PostCategoryService postCategoryService;

    @Test
    @DisplayName("Should throw ValidationException when creating category with null name")
    void shouldThrowValidationExceptionWhenCreatingCategoryWithNullName() {
        PostCategoryEntity category = new PostCategoryEntity();
        category.name = null;

        assertThrows(ValidationException.class, () -> {
            postCategoryService.createCategory(category);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating category with empty name")
    void shouldThrowValidationExceptionWhenCreatingCategoryWithEmptyName() {
        PostCategoryEntity category = new PostCategoryEntity();
        category.name = "";

        assertThrows(ValidationException.class, () -> {
            postCategoryService.createCategory(category);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating category with whitespace name")
    void shouldThrowValidationExceptionWhenCreatingCategoryWithWhitespaceName() {
        PostCategoryEntity category = new PostCategoryEntity();
        category.name = "   ";

        assertThrows(ValidationException.class, () -> {
            postCategoryService.createCategory(category);
        });
    }
}
