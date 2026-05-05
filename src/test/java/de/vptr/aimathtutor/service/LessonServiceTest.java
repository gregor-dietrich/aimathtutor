package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
class LessonServiceTest {

    @Inject
    private LessonService lessonService;

    @Inject
    private EntityManager em;

    private LessonEntity buildLesson(final String prefix) {
        final var lesson = new LessonEntity();
        lesson.name = prefix + "_" + UUID.randomUUID().toString().substring(0, 8);
        return lesson;
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with null name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingLessonWithNullName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = null;

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with empty name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingLessonWithEmptyName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "";

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should throw ValidationException when creating lesson with whitespace name")
    @Transactional
    void shouldThrowValidationExceptionWhenCreatingLessonWithWhitespaceName() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "   ";

        assertThrows(ValidationException.class, () -> {
            this.lessonService.createLesson(lesson);
        });
    }

    @Test
    @DisplayName("Should create root lesson")
    @TestTransaction
    void shouldCreateRootLesson() {
        final LessonEntity lesson = this.buildLesson("root");

        final LessonViewDto created = this.lessonService.createLesson(lesson);

        assertNotNull(created.id);
        assertEquals(lesson.name, created.name);
        assertTrue(created.isRootLesson);
        assertEquals(0, created.childrenCount);
    }

    @Test
    @DisplayName("Should create child lesson with parent reference")
    @TestTransaction
    void shouldCreateChildLessonWithParent() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("parent"));

        final LessonEntity child = this.buildLesson("child");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = parent.id;
        child.parent = parentRef;

        final LessonViewDto childDto = this.lessonService.createLesson(child);

        assertEquals(parent.id, childDto.parentId);
        assertEquals(parent.name, childDto.parentName);
        assertFalse(childDto.isRootLesson);
    }

    @Test
    @DisplayName("Should reject child lesson with unknown parent id")
    @TestTransaction
    void shouldRejectUnknownParentId() {
        final LessonEntity child = this.buildLesson("orphan");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = 999_999L;
        child.parent = parentRef;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.lessonService.createLesson(child));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should find children of a parent lesson")
    @TestTransaction
    void shouldFindChildrenByParentId() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("p"));
        final LessonEntity childRef = this.buildLesson("c");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = parent.id;
        childRef.parent = parentRef;
        final LessonViewDto created = this.lessonService.createLesson(childRef);

        final var children = this.lessonService.findByParentId(parent.id);

        assertEquals(1, children.size());
        assertEquals(created.id, children.get(0).id);
    }

    @Test
    @DisplayName("Should reject circular parent reference")
    @TestTransaction
    void shouldRejectCircularParentReference() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("a"));
        final LessonEntity childEntity = this.buildLesson("b");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = parent.id;
        childEntity.parent = parentRef;
        final LessonViewDto child = this.lessonService.createLesson(childEntity);

        final LessonEntity update = new LessonEntity();
        update.id = parent.id;
        update.name = "renamed";
        final LessonEntity newParent = new LessonEntity();
        newParent.id = child.id;
        update.parent = newParent;

        final var thrown = assertThrows(WebApplicationException.class,
                () -> this.lessonService.updateLesson(update));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should delete lesson by id")
    @TestTransaction
    void shouldDeleteLesson() {
        final LessonViewDto created = this.lessonService.createLesson(this.buildLesson("del"));

        final boolean deleted = this.lessonService.deleteLesson(created.id);

        assertTrue(deleted);
        assertTrue(this.lessonService.findById(created.id).isEmpty());
    }

    @Test
    @DisplayName("Should return DTO with initialized collections after creating a lesson")
    @TestTransaction
    void shouldReturnDtoWithInitializedCollections() {
        this.lessonService.createLesson(this.buildLesson("init"));

        final var lessons = this.lessonService.getAllLessons();

        assertFalse(lessons.isEmpty());
        for (final var lesson : lessons) {
            assertNotNull(lesson.childrenIds, "childrenIds should never be null");
            assertTrue(lesson.childrenCount >= 0);
            assertTrue(lesson.exercisesCount >= 0);
        }
    }

    @Test
    @DisplayName("Should return DTO with initialized collections after context close")
    @TestTransaction
    void shouldReturnDtoWithInitializedCollectionsAfterContextClose() {
        this.lessonService.createLesson(this.buildLesson("init"));

        this.em.clear();

        final var lessons = this.lessonService.getAllLessons();

        assertFalse(lessons.isEmpty());
        for (final var lesson : lessons) {
            assertNotNull(lesson.childrenIds, "childrenIds should never be null");
            assertTrue(lesson.childrenCount >= 0);
            assertTrue(lesson.exercisesCount >= 0);
        }
    }
}
