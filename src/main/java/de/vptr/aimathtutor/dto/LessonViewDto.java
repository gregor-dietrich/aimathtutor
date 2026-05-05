package de.vptr.aimathtutor.dto;

import java.util.List;

import de.vptr.aimathtutor.entity.LessonEntity;

/**
 * Response DTO for lesson operations.
 * Contains computed fields and safe data for client responses.
 */
public class LessonViewDto {
    public String publicId;
    public String name;
    public String parentPublicId;
    public String parentName;
    public boolean isRootLesson;
    public int childrenCount;
    public int exercisesCount;
    public List<String> childrenPublicIds;

    /**
     * Default constructor for JSON mapping.
     */
    public LessonViewDto() {
    }

    /**
     * Constructs a LessonViewDto from a lesson entity.
     *
     * @param entity the lesson entity to convert
     */
    public LessonViewDto(final LessonEntity entity) {
        this.publicId = entity.publicId;
        this.name = entity.name;
        this.isRootLesson = entity.isRootLesson();

        if (entity.parent != null) {
            this.parentPublicId = entity.parent.publicId;
            this.parentName = entity.parent.name;
        }

        if (entity.children != null && !entity.children.isEmpty()) {
            this.childrenCount = entity.children.size();
            this.childrenPublicIds = entity.children.stream()
                    .map(child -> child.publicId)
                    .toList();
        } else {
            this.childrenCount = 0;
            this.childrenPublicIds = List.of();
        }

        if (entity.exercises != null) {
            this.exercisesCount = (int) entity.exercises.stream()
                    .filter(ex -> Boolean.TRUE.equals(ex.published))
                    .count();
        } else {
            this.exercisesCount = 0;
        }
    }

    public boolean isRootLesson() {
        return this.isRootLesson;
    }

    public String getName() {
        return this.name;
    }

    public String getPublicId() {
        return this.publicId;
    }

    /**
     * Converts this view DTO to a lesson operation DTO.
     *
     * @return a {@link LessonDto} with the same data
     */
    public LessonDto toLessonDto() {
        final var dto = new LessonDto();
        dto.publicId = this.publicId;
        dto.name = this.name;
        dto.parentPublicId = this.parentPublicId;
        if (this.parentPublicId != null) {
            dto.parent = new LessonDto.ParentField(this.parentPublicId);
        }
        return dto;
    }
}
