package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExerciseEntityTest {

    private ExerciseEntity exerciseEntity;
    private UserEntity user;
    private LessonEntity lesson;

    @BeforeEach
    void setUp() {
        this.exerciseEntity = new ExerciseEntity();
        this.user = new UserEntity();
        this.user.id = 1L;
        this.user.username = "testuser";

        this.lesson = new LessonEntity();
        this.lesson.id = 1L;
        this.lesson.name = "General";
    }

    @Test
    @DisplayName("Should create ExerciseEntity with all fields")
    void shouldCreateExerciseEntityWithAllFields() {
        // Given
        final LocalDateTime now = LocalDateTime.now();
        final List<CommentEntity> comments = new ArrayList<>();

        // When
        this.exerciseEntity.id = 1L;
        this.exerciseEntity.title = "Test Exercise";
        this.exerciseEntity.content = "This is test content";
        this.exerciseEntity.user = this.user;
        this.exerciseEntity.lesson = this.lesson;
        this.exerciseEntity.published = true;
        this.exerciseEntity.commentable = true;
        this.exerciseEntity.created = now;
        this.exerciseEntity.lastEdit = now;
        this.exerciseEntity.comments = comments;

        // Then
        assertEquals(1L, this.exerciseEntity.id);
        assertEquals("Test Exercise", this.exerciseEntity.title);
        assertEquals("This is test content", this.exerciseEntity.content);
        assertEquals(this.user, this.exerciseEntity.user);
        assertEquals(this.lesson, this.exerciseEntity.lesson);
        assertTrue(this.exerciseEntity.published);
        assertTrue(this.exerciseEntity.commentable);
        assertEquals(now, this.exerciseEntity.created);
        assertEquals(now, this.exerciseEntity.lastEdit);
        assertEquals(comments, this.exerciseEntity.comments);
    }

    @Test
    @DisplayName("Should have default values for boolean fields")
    void shouldHaveDefaultValuesForBooleanFields() {
        // Then
        assertFalse(this.exerciseEntity.published);
        assertFalse(this.exerciseEntity.commentable);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void shouldHandleNullValuesForOptionalFields() {
        // When
        this.exerciseEntity.title = "Required Title";
        this.exerciseEntity.content = "Required Content";
        this.exerciseEntity.user = null;
        this.exerciseEntity.lesson = null;
        this.exerciseEntity.created = null;
        this.exerciseEntity.lastEdit = null;
        this.exerciseEntity.comments = null;

        // Then
        assertNull(this.exerciseEntity.user);
        assertNull(this.exerciseEntity.lesson);
        assertNull(this.exerciseEntity.created);
        assertNull(this.exerciseEntity.lastEdit);
        assertNull(this.exerciseEntity.comments);
    }

    @Test
    @DisplayName("Should handle comments collection properly")
    void shouldHandleCommentsCollectionProperly() {
        // Given
        final List<CommentEntity> comments = new ArrayList<>();
        final CommentEntity comment1 = new CommentEntity();
        comment1.id = 1L;
        comment1.content = "First comment";

        final CommentEntity comment2 = new CommentEntity();
        comment2.id = 2L;
        comment2.content = "Second comment";

        comments.add(comment1);
        comments.add(comment2);

        // When
        this.exerciseEntity.comments = comments;

        // Then
        assertNotNull(this.exerciseEntity.comments);
        assertEquals(2, this.exerciseEntity.comments.size());
        assertEquals(comment1, this.exerciseEntity.comments.get(0));
        assertEquals(comment2, this.exerciseEntity.comments.get(1));
    }

    @Test
    @DisplayName("Should handle empty comments collection")
    void shouldHandleEmptyCommentsCollection() {
        // When
        this.exerciseEntity.comments = new ArrayList<>();

        // Then
        assertNotNull(this.exerciseEntity.comments);
        assertTrue(this.exerciseEntity.comments.isEmpty());
    }

    @Test
    @DisplayName("Should set boolean fields correctly")
    void shouldSetBooleanFieldsCorrectly() {
        // When
        this.exerciseEntity.published = true;
        this.exerciseEntity.commentable = false;

        // Then
        assertTrue(this.exerciseEntity.published);
        assertFalse(this.exerciseEntity.commentable);
    }
}