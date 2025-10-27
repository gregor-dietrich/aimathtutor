package de.vptr.aimathtutor.service;

import java.util.List;
import java.util.Optional;

import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.repository.ExerciseRepository;
import de.vptr.aimathtutor.repository.LessonRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class LessonService {

    @Inject
    LessonRepository lessonRepository;

    @Inject
    ExerciseRepository exerciseRepository;

    @Transactional
    public List<LessonViewDto> getAllLessons() {
        return this.lessonRepository.findAllOrdered().stream().map(LessonViewDto::new).toList();
    }

    @Transactional
    public Optional<LessonViewDto> findById(final Long id) {
        return this.lessonRepository.findByIdOptional(id).map(LessonViewDto::new);
    }

    @Transactional
    public List<LessonViewDto> findRootLessons() {
        return this.lessonRepository.findRootLessons().stream().map(LessonViewDto::new).toList();
    }

    public List<LessonViewDto> findByParentId(final Long parentId) {
        return this.lessonRepository.findByParentId(parentId).stream().map(LessonViewDto::new).toList();
    }

    @Transactional
    public LessonViewDto createLesson(final LessonEntity lesson) {
        // Validate name is provided for creation
        if (lesson.name == null || lesson.name.trim().isEmpty()) {
            throw new ValidationException("Name is required for creating a lesson");
        }

        // If parent is specified, ensure it exists
        if (lesson.parent != null && lesson.parent.id != null) {
            final var existingParent = this.lessonRepository.findById(lesson.parent.id);
            if (existingParent == null) {
                throw new WebApplicationException("Parent lesson not found", Response.Status.BAD_REQUEST);
            }
            lesson.parent = existingParent;
        }

        final LessonEntity persisted = this.lessonRepository.persist(lesson);
        return new LessonViewDto(persisted);
    }

    @Transactional
    public LessonViewDto updateLesson(final LessonEntity lesson) {
        final var existingLesson = this.lessonRepository.findById(lesson.id);
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
            final LessonEntity newParent = this.lessonRepository.findById(lesson.parent.id);
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

        final LessonEntity persisted = this.lessonRepository.persist(existingLesson);
        return new LessonViewDto(persisted);
    }

    @Transactional
    public LessonViewDto patchLesson(final LessonEntity lesson) {
        final var existingLesson = this.lessonRepository.findById(lesson.id);
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
                final LessonEntity newParent = this.lessonRepository.findById(lesson.parent.id);
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

        final LessonEntity persisted = this.lessonRepository.persist(existingLesson);
        return new LessonViewDto(persisted);
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
        return this.lessonRepository.deleteById(id);
    }

    @Transactional
    public List<LessonViewDto> searchLessons(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllLessons();
        }
        final List<LessonEntity> lessons = this.lessonRepository.search(query);
        return lessons.stream().map(LessonViewDto::new).toList();
    }
}
