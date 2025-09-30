package de.vptr.aimathtutor.rest.dto;

import java.util.List;

import de.vptr.aimathtutor.rest.entity.PostCategoryEntity;

/**
 * Response DTO for post category operations.
 * Contains computed fields and safe data for client responses.
 */
public class PostCategoryViewDto {

    public Long id;
    public String name;
    public Long parentId;
    public String parentName;
    public boolean isRootCategory;
    public int childrenCount;
    public int postsCount;
    public List<Long> childrenIds;

    public PostCategoryViewDto() {
        // Default constructor for Jackson
    }

    public PostCategoryViewDto(final PostCategoryEntity entity) {
        this.id = entity.id;
        this.name = entity.name;
        this.isRootCategory = entity.isRootCategory();

        // Handle parent information safely
        if (entity.parent != null) {
            this.parentId = entity.parent.id;
            this.parentName = entity.parent.name;
        }

        // Compute children count and IDs safely
        try {
            if (entity.children != null && !entity.children.isEmpty()) {
                this.childrenCount = entity.children.size();
                this.childrenIds = entity.children.stream()
                        .map(child -> child.id)
                        .toList();
            } else {
                this.childrenCount = 0;
                this.childrenIds = List.of();
            }
        } catch (final org.hibernate.LazyInitializationException e) {
            // Collection not initialized, set defaults
            this.childrenCount = 0;
            this.childrenIds = List.of();
        }

        // Compute posts count safely
        try {
            if (entity.posts != null) {
                this.postsCount = entity.posts.size();
            } else {
                this.postsCount = 0;
            }
        } catch (final org.hibernate.LazyInitializationException e) {
            // Collection not initialized, set default
            this.postsCount = 0;
        }
    }

    /**
     * Helper method to check if this is a root category
     */
    public boolean isRootCategory() {
        return this.isRootCategory;
    }

    /**
     * Getter for name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Getter for id
     */
    public Long getId() {
        return this.id;
    }

    /**
     * Convert this ViewDto to a PostCategoryDto for create/update operations
     */
    public PostCategoryDto toPostCategoryDto() {
        final PostCategoryDto dto = new PostCategoryDto();
        dto.id = this.id;
        dto.name = this.name;
        dto.parentId = this.parentId;
        return dto;
    }
}
