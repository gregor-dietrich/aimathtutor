package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.LessonEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class LessonRepositoryTest {

    @Inject
    LessonRepository lessonRepository;

    @Test
    @DisplayName("Null handling for repository methods")
    void testNullHandling() {
        assertNull(this.lessonRepository.findById(null));
        assertTrue(this.lessonRepository.findByIdOptional(null).isEmpty());
        assertTrue(this.lessonRepository.findByPublicId(null).isEmpty());
        assertNull(this.lessonRepository.persist(null));
    }

    @Test
    @DisplayName("findAllOrdered and findRootLessons return non-null lists")
    void testFindAllAndRootLessons() {
        assertNotNull(this.lessonRepository.findAllOrdered());
        assertNotNull(this.lessonRepository.findRootLessons());
    }

    @Test
    @DisplayName("search with null or blank returns all, search with term filters results")
    @TestTransaction
    void testSearch() {
        final var lesson = new LessonEntity();
        lesson.name = "GeometryLesson " + UlidUtil.generate();
        lesson.publicId = UlidUtil.generate();
        this.lessonRepository.persist(lesson);
        this.lessonRepository.flush();

        assertNotNull(this.lessonRepository.search(null));
        assertNotNull(this.lessonRepository.search(""));
        final var results = this.lessonRepository.search("GeometryLesson");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("persist and deleteById cover both found and not-found paths")
    @TestTransaction
    void testPersistAndDeleteById() {
        final var lesson = new LessonEntity();
        lesson.name = "PersistTest " + UlidUtil.generate();
        lesson.publicId = UlidUtil.generate();
        this.lessonRepository.persist(lesson);
        this.lessonRepository.flush();

        assertNotNull(this.lessonRepository.findById(lesson.id));
        assertFalse(this.lessonRepository.deleteById(999_999L));
        assertTrue(this.lessonRepository.deleteById(lesson.id));
        assertNull(this.lessonRepository.findById(lesson.id));
    }

    @Test
    @DisplayName("deleteByPublicId covers found and not-found paths")
    @TestTransaction
    void testDeleteByPublicId() {
        final String pid = UlidUtil.generate();
        assertFalse(this.lessonRepository.deleteByPublicId(pid));

        final var lesson = new LessonEntity();
        lesson.name = "DeleteByPid " + UlidUtil.generate();
        lesson.publicId = pid;
        this.lessonRepository.persist(lesson);
        this.lessonRepository.flush();

        assertTrue(this.lessonRepository.deleteByPublicId(pid));
        assertTrue(this.lessonRepository.findByPublicId(pid).isEmpty());
    }

    @Test
    @DisplayName("isRootLesson returns true when parent is null")
    @TestTransaction
    void testIsRootLesson() {
        final var root = new LessonEntity();
        root.name = "Root " + UlidUtil.generate();
        root.publicId = UlidUtil.generate();
        this.lessonRepository.persist(root);
        this.lessonRepository.flush();

        assertTrue(root.isRootLesson());

        final var child = new LessonEntity();
        child.name = "Child " + UlidUtil.generate();
        child.parent = root;
        child.publicId = UlidUtil.generate();
        this.lessonRepository.persist(child);
        this.lessonRepository.flush();

        assertFalse(child.isRootLesson());
    }
}
