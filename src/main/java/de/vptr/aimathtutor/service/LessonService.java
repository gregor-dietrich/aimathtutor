package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class LessonService {

    @Transactional
    public List<LessonViewDto> getAllLessons() {
        return LessonEntity.listAll().stream()
                .map(entity -> new LessonViewDto((LessonEntity) entity))
                .toList();
    }

    @Transactional
    public Optional<LessonViewDto> findById(final Long id) {
        return LessonEntity.findByIdOptional(id)
                .map(entity -> new LessonViewDto((LessonEntity) entity));
    }

    @Transactional
    public List<LessonViewDto> findRootLessons() {
        return LessonEntity.findRootLessons().stream()
                .map(LessonViewDto::new)
                .toList();
    }

    public List<LessonViewDto> findByParentId(final Long parentId) {
        return LessonEntity.findByParentId(parentId).stream()
                .map(LessonViewDto::new)
                .toList();
    }

    @Transactional
    public LessonViewDto createLesson(final LessonEntity lesson) {
        // Validate name is provided for creation
        if (lesson.name == null || lesson.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for creating a lesson");
        }

        // If parent is specified, ensure it exists
        if (lesson.parent != null && lesson.parent.id != null) {
            final var existingParent = (LessonEntity) LessonEntity.findById(lesson.parent.id);
            if (existingParent == null) {
                throw new WebApplicationException("Parent lesson not found", Response.Status.BAD_REQUEST);
            }
            lesson.parent = existingParent;
        }

        lesson.persist();
        return new LessonViewDto(lesson);
    }

    @Transactional
    public LessonViewDto updateLesson(final LessonEntity lesson) {
        final var existingLesson = (LessonEntity) LessonEntity.findById(lesson.id);
        if (existingLesson == null) {
            throw new WebApplicationException("Lesson not found", Response.Status.NOT_FOUND);
        }

        // Validate name is provided for complete replacement (PUT)
        if (lesson.name == null || lesson.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for updating a lesson");
        }

        // Complete replacement (PUT semantics) - update name and parent
        existingLesson.name = lesson.name;

        // Handle parent change - validate if parent is provided
        if (lesson.parent != null && lesson.parent.id != null) {
            final LessonEntity newParent = LessonEntity.findById(lesson.parent.id);
            if (newParent == null) {
                throw new WebApplicationException("Parent lesson not found", Response.Status.BAD_REQUEST);
            }
            // Prevent circular references
            if (this.isDescendantOf(newParent, existingLesson)) {
                throw new WebApplicationException("Cannot set parent to a descendant lesson",
                        Response.Status.BAD_REQUEST);
            }
            existingLesson.parent = newParent;
        } else if (lesson.parent == null) {
            // Explicitly set to null if parent is null (making it a root lesson)
            existingLesson.parent = null;
        }

        existingLesson.persist();
        return new LessonViewDto(existingLesson);
    }

    @Transactional
    public LessonViewDto patchLesson(final LessonEntity lesson) {
        final var existingLesson = (LessonEntity) LessonEntity.findById(lesson.id);
        if (existingLesson == null) {
            throw new WebApplicationException("Lesson not found", Response.Status.NOT_FOUND);
        }

        // Partial update (PATCH semantics) - only update provided fields
        if (lesson.name != null) {
            existingLesson.name = lesson.name;
        }

        // Handle parent change if provided
        if (lesson.parent != null) {
            if (lesson.parent.id != null) {
                final LessonEntity newParent = (LessonEntity) LessonEntity
                        .findById(lesson.parent.id);
                if (newParent == null) {
                    throw new WebApplicationException("Parent lesson not found", Response.Status.BAD_REQUEST);
                }
                // Prevent circular references
                if (this.isDescendantOf(newParent, existingLesson)) {
                    throw new WebApplicationException("Cannot set parent to a descendant lesson",
                            Response.Status.BAD_REQUEST);
                }
                existingLesson.parent = newParent;
            } else {
                // Set to null if parent ID is null (making it a root lesson)
                existingLesson.parent = null;
            }
        }

        existingLesson.persist();
        return new LessonViewDto(existingLesson);
    }

    /**
     * Check if potential parent is a descendant of the lesson (to prevent
     * circular references)
     */
    private boolean isDescendantOf(final LessonEntity potentialParent, final LessonEntity lesson) {
        var current = potentialParent.parent;
        while (current != null) {
            if (current.id.equals(lesson.id)) {
                return true;
            }
            current = current.parent;
        }
        return false;
    }

    @Transactional
    public boolean deleteLesson(final Long id) {
        return LessonEntity.deleteById(id);
    }

    public List<LessonViewDto> searchLessons(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllLessons();
        }
        final var searchTerm = "%" + query.trim().toLowerCase() + "%";
        final List<LessonEntity> lessons = LessonEntity.find(
                "LOWER(name) LIKE ?1", searchTerm).list();
        return lessons.stream()
                .map(LessonViewDto::new)
                .toList();
    }
}
