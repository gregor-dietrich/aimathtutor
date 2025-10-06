package de.vptr.aimathtutor.rest.dto;

import java.util.List;

import de.vptr.aimathtutor.rest.entity.LessonEntity;

/**
 * Response DTO for lesson operations.
 * Contains computed fields and safe data for client responses.
 */
public class LessonViewDto {

    public Long id;
    public String name;
    public Long parentId;
    public String parentName;
    public boolean isRootLesson;
    public int childrenCount;
    public int exercisesCount;
    public List<Long> childrenIds;

    public LessonViewDto() {
        // Default constructor for Jackson
    }

    public LessonViewDto(final LessonEntity entity) {
        this.id = entity.id;
        this.name = entity.name;
        this.isRootLesson = entity.isRootLesson();

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

        // Compute exercises count safely
        try {
            if (entity.exercises != null) {
                this.exercisesCount = entity.exercises.size();
            } else {
                this.exercisesCount = 0;
            }
        } catch (final org.hibernate.LazyInitializationException e) {
            // Collection not initialized, set default
            this.exercisesCount = 0;
        }
    }

    /**
     * Helper method to check if this is a root lesson
     */
    public boolean isRootLesson() {
        return this.isRootLesson;
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
     * Convert this ViewDto to a LessonDto for create/update operations
     */
    public LessonDto toLessonDto() {
        final var dto = new LessonDto();
        dto.id = this.id;
        dto.name = this.name;
        dto.parentId = this.parentId;
        return dto;
    }
}
