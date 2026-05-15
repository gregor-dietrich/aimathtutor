package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.vptr.aimathtutor.dto.LessonViewDto;
import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.service.security.PermissionService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@QuarkusTest
@SuppressWarnings("NullAway")
class LessonServiceTest {

    @Inject
    private LessonService lessonService;

    @Inject
    private EntityManager em;

    @InjectMock
    private PermissionService permissionService;

    @BeforeEach
    void setUpPermissionService() {
        Mockito.doNothing().when(this.permissionService).requireLessonAdd();
        Mockito.doNothing().when(this.permissionService).requireLessonEdit();
        Mockito.doNothing().when(this.permissionService).requireLessonDelete();
    }

    private Long getLessonNumericId(final String publicId) {
        return this.em.createQuery("SELECT l FROM LessonEntity l WHERE l.publicId = :p", LessonEntity.class)
                .setParameter("p", publicId).getSingleResult().id;
    }

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

        assertNotNull(created.publicId);
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
        parentRef.id = this.getLessonNumericId(parent.publicId);
        child.parent = parentRef;

        final LessonViewDto childDto = this.lessonService.createLesson(child);

        assertEquals(parent.publicId, childDto.parentPublicId);
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

        final var thrown = assertThrows(WebApplicationException.class, () -> this.lessonService.createLesson(child));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should find children of a parent lesson")
    @TestTransaction
    void shouldFindChildrenByParentId() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("p"));
        final LessonEntity childRef = this.buildLesson("c");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = this.getLessonNumericId(parent.publicId);
        childRef.parent = parentRef;
        final LessonViewDto created = this.lessonService.createLesson(childRef);

        final var children = this.lessonService.findByParentId(this.getLessonNumericId(parent.publicId));

        assertEquals(1, children.size());
        assertEquals(created.publicId, children.get(0).publicId);
    }

    @Test
    @DisplayName("Should reject circular parent reference")
    @TestTransaction
    void shouldRejectCircularParentReference() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("a"));
        final LessonEntity childEntity = this.buildLesson("b");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = this.getLessonNumericId(parent.publicId);
        childEntity.parent = parentRef;
        final LessonViewDto child = this.lessonService.createLesson(childEntity);

        final LessonEntity update = new LessonEntity();
        update.publicId = parent.publicId;
        update.name = "renamed";
        final LessonEntity newParent = new LessonEntity();
        newParent.publicId = child.publicId;
        update.parent = newParent;

        final var thrown = assertThrows(WebApplicationException.class, () -> this.lessonService.updateLesson(update));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("Should delete lesson by id")
    @TestTransaction
    void shouldDeleteLesson() {
        final LessonViewDto created = this.lessonService.createLesson(this.buildLesson("del"));
        final Long numericId = this.getLessonNumericId(created.publicId);

        final boolean deleted = this.lessonService.deleteLesson(created.publicId);

        assertTrue(deleted);
        assertTrue(this.lessonService.findById(numericId).isEmpty());
    }

    @Test
    @DisplayName("Should return DTO with initialized collections after creating a lesson")
    @TestTransaction
    void shouldReturnDtoWithInitializedCollections() {
        this.lessonService.createLesson(this.buildLesson("init"));

        this.assertAllLessonsHaveValidCollections();
    }

    @Test
    @DisplayName("Should return DTO with initialized collections after context close")
    @TestTransaction
    void shouldReturnDtoWithInitializedCollectionsAfterContextClose() {
        this.lessonService.createLesson(this.buildLesson("init"));

        this.em.clear();

        this.assertAllLessonsHaveValidCollections();
    }

    private void assertAllLessonsHaveValidCollections() {
        final var lessons = this.lessonService.getAllLessons();
        assertFalse(lessons.isEmpty());
        for (final var lesson : lessons) {
            assertNotNull(lesson.childrenPublicIds, "childrenPublicIds should never be null");
            assertTrue(lesson.childrenCount >= 0);
            assertTrue(lesson.exercisesCount >= 0);
        }
    }

    @Test
    @DisplayName("updateLesson replaces the lesson name")
    @TestTransaction
    void testUpdateLesson_replacesName() {
        final LessonViewDto created = this.lessonService.createLesson(this.buildLesson("update_src"));

        final LessonEntity update = new LessonEntity();
        update.publicId = created.publicId;
        update.name = "Updated Lesson Name";

        final LessonViewDto updated = this.lessonService.updateLesson(update);

        assertEquals("Updated Lesson Name", updated.name);

        this.em.flush();
        this.em.clear();

        final LessonEntity persisted =
                this.em.createQuery("SELECT l FROM LessonEntity l WHERE l.publicId = :p", LessonEntity.class)
                        .setParameter("p", created.publicId).getSingleResult();
        assertEquals("Updated Lesson Name", persisted.name, "Updated name should be persisted in the database");
    }

    @Test
    @DisplayName("patchLesson updates only the provided name")
    @TestTransaction
    void testPatchLesson_updatesName() {
        final LessonViewDto created = this.lessonService.createLesson(this.buildLesson("patch_src"));
        final String originalPublicId = created.publicId;

        final LessonEntity patch = new LessonEntity();
        patch.publicId = created.publicId;
        patch.name = "Patched Name";

        final LessonViewDto patched = this.lessonService.patchLesson(patch);

        assertEquals("Patched Name", patched.name);
        assertEquals(originalPublicId, patched.publicId, "publicId should be unchanged after patch");

        this.em.flush();
        this.em.clear();

        final LessonEntity persisted =
                this.em.createQuery("SELECT l FROM LessonEntity l WHERE l.publicId = :p", LessonEntity.class)
                        .setParameter("p", originalPublicId).getSingleResult();
        assertEquals("Patched Name", persisted.name, "Patched name should be persisted in the database");
    }

    @Test
    @DisplayName("findRootLessons returns only lessons without a parent")
    @TestTransaction
    void testFindRootLessons_returnsOnlyRoots() {
        final LessonViewDto root = this.lessonService.createLesson(this.buildLesson("root"));
        final LessonEntity child = this.buildLesson("child");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = this.getLessonNumericId(root.publicId);
        child.parent = parentRef;
        final LessonViewDto childDto = this.lessonService.createLesson(child);

        final var roots = this.lessonService.findRootLessons();

        assertNotNull(roots);
        assertFalse(roots.isEmpty(), "There should be at least one root lesson");
        assertTrue(roots.stream().allMatch(l -> l.parentPublicId == null), "Root lessons should have no parent");
        assertFalse(roots.stream().anyMatch(l -> childDto.publicId.equals(l.publicId)),
                "Child lesson should not appear in root lessons");
    }

    @Test
    @DisplayName("searchLessons with blank query returns all lessons")
    @TestTransaction
    void testSearchLessons_blank() {
        final LessonViewDto createdLesson = this.lessonService.createLesson(this.buildLesson("search_blank"));
        final var results = this.lessonService.searchLessons("");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Blank query should return all lessons");
        assertTrue(results.stream().anyMatch(l -> createdLesson.publicId.equals(l.publicId)),
                "Blank search should include the newly created lesson");
    }

    @Test
    @DisplayName("searchLessons with matching term finds the lesson")
    @TestTransaction
    void testSearchLessons_match() {
        final LessonEntity lesson = this.buildLesson("UniqueSearchable");
        final String uniqueName = lesson.name;
        this.lessonService.createLesson(lesson);

        final var results = this.lessonService.searchLessons(uniqueName);
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Search should find the lesson by name");
        assertTrue(results.stream().anyMatch(l -> uniqueName.equals(l.name)));
    }

    @Test
    @DisplayName("searchLessons with non-matching term returns empty list")
    @TestTransaction
    void testSearchLessons_noMatch() {
        final var results = this.lessonService.searchLessons("zzz_nobody_here_xyz_9999");
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("findById returns empty for unknown id")
    @TestTransaction
    void testFindById_notFound() {
        final var result = this.lessonService.findById(-999L);
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("deleteLesson returns false for unknown publicId")
    @TestTransaction
    void testDeleteLesson_notFound() {
        final boolean deleted = this.lessonService.deleteLesson("00000000000000000000000000");
        assertFalse(deleted);
    }

    @Test
    @DisplayName("updateLesson throws BAD_REQUEST when parent publicId does not exist")
    @TestTransaction
    void updateLesson_parentNotFound_throwsBadRequest() {
        final LessonViewDto created = this.lessonService.createLesson(this.buildLesson("update_parent_missing"));

        final LessonEntity update = new LessonEntity();
        update.publicId = created.publicId;
        update.name = "Updated Name";
        final LessonEntity nonExistentParent = new LessonEntity();
        nonExistentParent.publicId = "00000000000000000000nonexistent";
        update.parent = nonExistentParent;

        final var thrown = assertThrows(WebApplicationException.class, () -> this.lessonService.updateLesson(update));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }

    @Test
    @DisplayName("updateLesson clears parent when lesson.parent is null (PUT semantics)")
    @TestTransaction
    void updateLesson_setParentToNull_clearsParent() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("clear_parent"));
        final LessonEntity childEntity = this.buildLesson("clear_child");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = this.getLessonNumericId(parent.publicId);
        childEntity.parent = parentRef;
        final LessonViewDto child = this.lessonService.createLesson(childEntity);

        final LessonEntity update = new LessonEntity();
        update.publicId = child.publicId;
        update.name = child.name;
        update.parent = null;

        final LessonViewDto updated = this.lessonService.updateLesson(update);
        assertNull(updated.parentPublicId);
        assertTrue(updated.isRootLesson);
    }

    @Test
    @DisplayName("patchLesson clears parent when parent.id is null")
    @TestTransaction
    void patchLesson_parentIdNull_clearsParent() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("patch_clear_parent"));
        final LessonEntity childEntity = this.buildLesson("patch_clear_child");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = this.getLessonNumericId(parent.publicId);
        childEntity.parent = parentRef;
        final LessonViewDto child = this.lessonService.createLesson(childEntity);

        final LessonEntity patch = new LessonEntity();
        patch.publicId = child.publicId;
        final LessonEntity parentWithNullId = new LessonEntity();
        parentWithNullId.id = null;
        patch.parent = parentWithNullId;

        final LessonViewDto patched = this.lessonService.patchLesson(patch);
        assertNull(patched.parentPublicId);
    }

    @Test
    @DisplayName("deleteLesson throws BAD_REQUEST when lesson has children")
    @TestTransaction
    void deleteLesson_withChildren_throwsBadRequest() {
        final LessonViewDto parent = this.lessonService.createLesson(this.buildLesson("del_parent"));
        final LessonEntity childEntity = this.buildLesson("del_child");
        final LessonEntity parentRef = new LessonEntity();
        parentRef.id = this.getLessonNumericId(parent.publicId);
        childEntity.parent = parentRef;
        this.lessonService.createLesson(childEntity);

        final var thrown =
                assertThrows(WebApplicationException.class, () -> this.lessonService.deleteLesson(parent.publicId));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), thrown.getResponse().getStatus());
    }
}
