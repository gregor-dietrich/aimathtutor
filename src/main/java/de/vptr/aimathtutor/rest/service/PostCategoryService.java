package de.vptr.aimathtutor.rest.service;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.rest.dto.PostCategoryViewDto;
import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class PostCategoryService {

    @Transactional
    public List<PostCategoryViewDto> getAllCategories() {
        return PostCategoryEntity.listAll().stream()
                .map(entity -> new PostCategoryViewDto((PostCategoryEntity) entity))
                .toList();
    }

    @Transactional
    public Optional<PostCategoryViewDto> findById(final Long id) {
        return PostCategoryEntity.findByIdOptional(id)
                .map(entity -> new PostCategoryViewDto((PostCategoryEntity) entity));
    }

    @Transactional
    public List<PostCategoryViewDto> findRootCategories() {
        return PostCategoryEntity.findRootCategories().stream()
                .map(PostCategoryViewDto::new)
                .toList();
    }

    public List<PostCategoryViewDto> findByParentId(final Long parentId) {
        return PostCategoryEntity.findByParentId(parentId).stream()
                .map(PostCategoryViewDto::new)
                .toList();
    }

    @Transactional
    public PostCategoryViewDto createCategory(final PostCategoryEntity category) {
        // Validate name is provided for creation
        if (category.name == null || category.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for creating a category");
        }

        // If parent is specified, ensure it exists
        if (category.parent != null && category.parent.id != null) {
            final var existingParent = (PostCategoryEntity) PostCategoryEntity.findById(category.parent.id);
            if (existingParent == null) {
                throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
            }
            category.parent = existingParent;
        }

        category.persist();
        return new PostCategoryViewDto(category);
    }

    @Transactional
    public PostCategoryViewDto updateCategory(final PostCategoryEntity category) {
        final var existingCategory = (PostCategoryEntity) PostCategoryEntity.findById(category.id);
        if (existingCategory == null) {
            throw new WebApplicationException("Category not found", Response.Status.NOT_FOUND);
        }

        // Validate name is provided for complete replacement (PUT)
        if (category.name == null || category.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for updating a category");
        }

        // Complete replacement (PUT semantics) - update name and parent
        existingCategory.name = category.name;

        // Handle parent change - validate if parent is provided
        if (category.parent != null && category.parent.id != null) {
            final PostCategoryEntity newParent = PostCategoryEntity.findById(category.parent.id);
            if (newParent == null) {
                throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
            }
            // Prevent circular references
            if (this.isDescendantOf(newParent, existingCategory)) {
                throw new WebApplicationException("Cannot set parent to a descendant category",
                        Response.Status.BAD_REQUEST);
            }
            existingCategory.parent = newParent;
        } else if (category.parent == null) {
            // Explicitly set to null if parent is null (making it a root category)
            existingCategory.parent = null;
        }

        existingCategory.persist();
        return new PostCategoryViewDto(existingCategory);
    }

    @Transactional
    public PostCategoryViewDto patchCategory(final PostCategoryEntity category) {
        final var existingCategory = (PostCategoryEntity) PostCategoryEntity.findById(category.id);
        if (existingCategory == null) {
            throw new WebApplicationException("Category not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (category.name != null) {
            existingCategory.name = category.name;
        }

        // Handle parent change if provided
        if (category.parent != null) {
            if (category.parent.id != null) {
                final PostCategoryEntity newParent = (PostCategoryEntity) PostCategoryEntity
                        .findById(category.parent.id);
                if (newParent == null) {
                    throw new WebApplicationException("Parent category not found", Response.Status.BAD_REQUEST);
                }
                // Prevent circular references
                if (this.isDescendantOf(newParent, existingCategory)) {
                    throw new WebApplicationException("Cannot set parent to a descendant category",
                            Response.Status.BAD_REQUEST);
                }
                existingCategory.parent = newParent;
            } else {
                // Set to null if parent ID is null (making it a root category)
                existingCategory.parent = null;
            }
        }

        existingCategory.persist();
        return new PostCategoryViewDto(existingCategory);
    }

    /**
     * Check if potential parent is a descendant of the category (to prevent
     * circular references)
     */
    private boolean isDescendantOf(final PostCategoryEntity potentialParent, final PostCategoryEntity category) {
        var current = potentialParent.parent;
        while (current != null) {
            if (current.id.equals(category.id)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    @Transactional
    public boolean deleteCategory(final Long id) {
        return PostCategoryEntity.deleteById(id);
    }

    public List<PostCategoryViewDto> searchCategories(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllCategories();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<PostCategoryEntity> categories = PostCategoryEntity.find(
                "LOWER(name) LIKE ?1", searchTerm).list();
        return categories.stream()
                .map(PostCategoryViewDto::new)
                .toList();
    }
}
