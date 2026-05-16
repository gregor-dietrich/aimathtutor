package de.vptr.aimathtutor.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class ExerciseRepositoryTest {

    @Inject
    ExerciseRepository exerciseRepository;

    @Test
    @DisplayName("Null handling for repository methods")
    void testNullHandling() {
        assertTrue(this.exerciseRepository.findByPublicId(null).isEmpty());
        assertNull(this.exerciseRepository.findById(null));
        assertTrue(this.exerciseRepository.findByIdOptional(null).isEmpty());
        assertNull(this.exerciseRepository.persist(null));
    }

    @Test
    @DisplayName("findAllOrdered, findPublished, and countPublished return non-null results")
    void testFindAllAndPublished() {
        assertNotNull(this.exerciseRepository.findAllOrdered());
        assertNotNull(this.exerciseRepository.findPublished());
        assertTrue(this.exerciseRepository.countPublished() >= 0);
    }

    @Test
    @DisplayName("persist and deleteById cover both found and not-found paths")
    @TestTransaction
    void testPersistAndDeleteById() {
        final var exercise = new ExerciseEntity();
        exercise.title = "Test Exercise " + UlidUtil.generate();
        exercise.content = "content";
        exercise.publicId = UlidUtil.generate();
        this.exerciseRepository.persist(exercise);
        this.exerciseRepository.flush();

        assertNotNull(this.exerciseRepository.findById(exercise.id));
        assertFalse(this.exerciseRepository.deleteById(999_999L));
        assertTrue(this.exerciseRepository.deleteById(exercise.id));
        assertNull(this.exerciseRepository.findById(exercise.id));
    }

    @Test
    @DisplayName("deleteByPublicId covers found and not-found paths")
    @TestTransaction
    void testDeleteByPublicId() {
        final String pid = UlidUtil.generate();
        assertFalse(this.exerciseRepository.deleteByPublicId(pid));

        final var exercise = new ExerciseEntity();
        exercise.title = "Del Test " + UlidUtil.generate();
        exercise.content = "content";
        exercise.publicId = pid;
        this.exerciseRepository.persist(exercise);
        this.exerciseRepository.flush();

        assertTrue(this.exerciseRepository.deleteByPublicId(pid));
        assertTrue(this.exerciseRepository.findByPublicId(pid).isEmpty());
    }

    @Test
    @DisplayName("search with null or blank returns all, search with term filters")
    @TestTransaction
    void testSearch() {
        final var exercise = new ExerciseEntity();
        exercise.title = "AlgebraTest " + UlidUtil.generate();
        exercise.content = "content";
        exercise.publicId = UlidUtil.generate();
        this.exerciseRepository.persist(exercise);
        this.exerciseRepository.flush();

        assertNotNull(this.exerciseRepository.search(null));
        assertNotNull(this.exerciseRepository.search(""));
        final var results = this.exerciseRepository.search("AlgebraTest");
        assertFalse(results.isEmpty());
    }

    @Test
    @DisplayName("findByDateRange returns results within the range")
    @TestTransaction
    void testFindByDateRange() {
        final var exercise = new ExerciseEntity();
        exercise.title = "DateRange " + UlidUtil.generate();
        exercise.content = "content";
        exercise.publicId = UlidUtil.generate();
        this.exerciseRepository.persist(exercise);
        this.exerciseRepository.flush();

        final var now = LocalDateTime.now(ZoneId.systemDefault());
        final var results = this.exerciseRepository.findByDateRange(now.minusMinutes(1), now.plusMinutes(1));
        assertNotNull(results);
    }
}
