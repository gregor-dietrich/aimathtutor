package de.vptr.aimathtutor.repository;

import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.LessonEntity;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Integration tests for {@link LessonRepository}.
 */
@QuarkusTest
public class LessonRepositoryIT {

    @Inject
    LessonRepository lessonRepository;

    private LessonEntity createLesson(final String name) {
        return createLesson(name, null);
    }

    private LessonEntity createLesson(final String name, @Nullable final LessonEntity parent) {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = name;
        lesson.parent = parent;
        this.lessonRepository.persist(lesson);
        return lesson;
    }

    @Test
    @TestTransaction
    public void testFindById_found() {
        final LessonEntity lesson = this.createLesson("Algebra Basics");
        final LessonEntity found = this.lessonRepository.findById(Objects.requireNonNull(lesson.id));
        Assertions.assertNotNull(found);
        Assertions.assertEquals("Algebra Basics", found.name);
    }

    @Test
    @TestTransaction
    @SuppressWarnings("NullAway")
    public void testFindById_null() {
        Assertions.assertNull(this.lessonRepository.findById(null));
    }

    @Test
    @TestTransaction
    public void testFindById_notFound() {
        Assertions.assertNull(this.lessonRepository.findById(999_999L));
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_found() {
        final LessonEntity lesson = this.createLesson("Calculus Intro");
        final var found = this.lessonRepository.findByIdOptional(Objects.requireNonNull(lesson.id));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("Calculus Intro", found.get().name);
    }

    @Test
    @TestTransaction
    public void testFindByIdOptional_notFound() {
        Assertions.assertTrue(this.lessonRepository.findByIdOptional(999_999L).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_found() {
        final LessonEntity lesson = this.createLesson("Geometry");
        final var found = this.lessonRepository.findByPublicId(Objects.requireNonNull(lesson.publicId));
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals(lesson.publicId, found.get().publicId);
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_null() {
        Assertions.assertTrue(this.lessonRepository.findByPublicId(null).isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindByPublicId_notFound() {
        Assertions.assertTrue(this.lessonRepository.findByPublicId("nonexistent-public-id").isEmpty());
    }

    @Test
    @TestTransaction
    public void testFindAllOrdered_returnsLessons() {
        final LessonEntity alpha = this.createLesson("Alpha");
        final LessonEntity beta = this.createLesson("Beta");
        final List<LessonEntity> all = this.lessonRepository.findAllOrdered();
        Assertions.assertFalse(all.isEmpty());
        final List<Long> ids = all.stream().map(l -> l.id).toList();
        Assertions.assertTrue(ids.indexOf(beta.id) < ids.indexOf(alpha.id),
                "findAllOrdered must return the newest lesson (beta, higher id) before the older one (alpha)");
    }

    @Test
    @TestTransaction
    public void testFindRootLessons_returnsLessonsWithNoParent() {
        final LessonEntity root = this.createLesson("Root Lesson");
        this.createLesson("Child Lesson", root);

        final List<LessonEntity> roots = this.lessonRepository.findRootLessons();
        Assertions.assertFalse(roots.isEmpty());
        Assertions.assertTrue(roots.stream().allMatch(LessonEntity::isRootLesson));
        Assertions.assertTrue(roots.stream().anyMatch(l -> "Root Lesson".equals(l.name)));
    }

    @Test
    @TestTransaction
    public void testFindByParentId_returnsChildren() {
        final LessonEntity parent = this.createLesson("Parent Lesson");
        final LessonEntity child = this.createLesson("Child Lesson A", parent);

        final List<LessonEntity> children = this.lessonRepository.findByParentId(Objects.requireNonNull(parent.id));
        Assertions.assertFalse(children.isEmpty());
        Assertions.assertTrue(children.stream().anyMatch(l -> Objects.equals(l.id, child.id)));
    }

    @Test
    @TestTransaction
    public void testSearch_byName_returnsMatchingLessons() {
        this.createLesson("Linear Algebra");
        final List<LessonEntity> results = this.lessonRepository.search("linear");
        Assertions.assertFalse(results.isEmpty());
        Assertions.assertTrue(results.stream().anyMatch(l -> "Linear Algebra".equals(l.name)));
    }

    @Test
    @TestTransaction
    @SuppressWarnings("NullAway")
    public void testSearch_nullReturnsAll() {
        this.createLesson("Statistics");
        final List<LessonEntity> baseline = this.lessonRepository.findAllOrdered();
        final List<LessonEntity> results = this.lessonRepository.search(null);
        Assertions.assertEquals(baseline.size(), results.size(), "search(null) must return all lessons");
    }

    @Test
    @TestTransaction
    public void testSearch_blankReturnsAll() {
        this.createLesson("Probability");
        final List<LessonEntity> baseline = this.lessonRepository.findAllOrdered();
        final List<LessonEntity> results = this.lessonRepository.search("   ");
        Assertions.assertEquals(baseline.size(), results.size(), "search(blank) must return all lessons");
    }

    @Test
    @TestTransaction
    public void testDeleteById_existingLesson() {
        final LessonEntity lesson = this.createLesson("To Delete");
        final Long id = Objects.requireNonNull(lesson.id);
        Assertions.assertTrue(this.lessonRepository.deleteById(id));
        Assertions.assertNull(this.lessonRepository.findById(id));
    }

    @Test
    @TestTransaction
    public void testDeleteById_nonExisting() {
        Assertions.assertFalse(this.lessonRepository.deleteById(999_999L));
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_existingLesson() {
        final LessonEntity lesson = this.createLesson("To Delete By PublicId");
        final String publicId = Objects.requireNonNull(lesson.publicId);
        Assertions.assertTrue(this.lessonRepository.deleteByPublicId(publicId));
        Assertions.assertTrue(this.lessonRepository.findByPublicId(publicId).isEmpty());
    }

    @Test
    @TestTransaction
    public void testDeleteByPublicId_nonExisting() {
        Assertions.assertFalse(this.lessonRepository.deleteByPublicId("nonexistent"));
    }

    @Test
    @TestTransaction
    public void testPersist_setsIdAndPublicId() {
        final LessonEntity lesson = new LessonEntity();
        lesson.name = "Persist Test";
        final LessonEntity result = this.lessonRepository.persist(lesson);
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.id);
        Assertions.assertNotNull(result.publicId);
    }

    @Test
    @TestTransaction
    @SuppressWarnings("NullAway")
    public void testPersist_null() {
        Assertions.assertNull(this.lessonRepository.persist(null));
    }

    @Test
    @TestTransaction
    public void testIsRootLesson_trueWhenNoParent() {
        final LessonEntity root = this.createLesson("Root");
        Assertions.assertTrue(root.isRootLesson());
    }

    @Test
    @TestTransaction
    public void testIsRootLesson_falseWhenHasParent() {
        final LessonEntity parent = this.createLesson("Parent");
        final LessonEntity child = this.createLesson("Child", parent);
        Assertions.assertFalse(child.isRootLesson());
    }
}
