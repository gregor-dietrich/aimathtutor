package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserEntityTest {

    private UserEntity userEntity;
    private UserRankEntity userRank;

    @BeforeEach
    void setUp() {
        this.userEntity = new UserEntity();
        this.userRank = new UserRankEntity();
        this.userRank.id = 1L;
        this.userRank.name = "User";
    }

    @Test
    @DisplayName("Should create UserEntity with all fields")
    void shouldCreateUserEntityWithAllFields() {
        // Given
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.userEntity.id = 1L;
        this.userEntity.username = "testuser";
        this.userEntity.password = "hashedpassword";
        this.userEntity.salt = "randomsalt";
        this.userEntity.rank = this.userRank;
        this.userEntity.email = "test@example.com";
        this.userEntity.banned = false;
        this.userEntity.activated = true;
        this.userEntity.activationKey = "activation123";
        this.userEntity.created = now;
        this.userEntity.lastLogin = now;

        // Then
        assertEquals(1L, this.userEntity.id);
        assertEquals("testuser", this.userEntity.username);
        assertEquals("hashedpassword", this.userEntity.password);
        assertEquals("randomsalt", this.userEntity.salt);
        assertEquals(this.userRank, this.userEntity.rank);
        assertEquals("test@example.com", this.userEntity.email);
        assertFalse(this.userEntity.banned);
        assertTrue(this.userEntity.activated);
        assertEquals("activation123", this.userEntity.activationKey);
        assertEquals(now, this.userEntity.created);
        assertEquals(now, this.userEntity.lastLogin);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void shouldHandleNullValuesForOptionalFields() {
        // When
        this.userEntity.id = 1L;
        this.userEntity.username = "testuser";
        this.userEntity.password = "password";
        this.userEntity.salt = "salt";
        this.userEntity.rank = this.userRank;
        this.userEntity.email = null;
        this.userEntity.banned = null;
        this.userEntity.activated = null;
        this.userEntity.activationKey = null;
        this.userEntity.created = null;
        this.userEntity.lastLogin = null;

        // Then
        assertNull(this.userEntity.email);
        assertNull(this.userEntity.banned);
        assertNull(this.userEntity.activated);
        assertNull(this.userEntity.activationKey);
        assertNull(this.userEntity.created);
        assertNull(this.userEntity.lastLogin);
    }

    @Test
    @DisplayName("Should handle collections properly")
    void shouldHandleCollectionsProperly() {
        // Given
        final List<ExerciseEntity> exercises = new ArrayList<>();
        final List<CommentEntity> comments = new ArrayList<>();

        final ExerciseEntity exercise = new ExerciseEntity();
        exercise.id = 1L;
        exercises.add(exercise);

        final CommentEntity comment = new CommentEntity();
        comment.id = 1L;
        comments.add(comment);

        // When
        this.userEntity.exercises = exercises;
        this.userEntity.comments = comments;

        // Then
        assertNotNull(this.userEntity.exercises);
        assertNotNull(this.userEntity.comments);
        assertEquals(1, this.userEntity.exercises.size());
        assertEquals(1, this.userEntity.comments.size());
        assertEquals(exercise, this.userEntity.exercises.get(0));
        assertEquals(comment, this.userEntity.comments.get(0));
    }

    @Test
    @DisplayName("Should handle empty collections")
    void shouldHandleEmptyCollections() {
        // When
        this.userEntity.exercises = new ArrayList<>();
        this.userEntity.comments = new ArrayList<>();

        // Then
        assertNotNull(this.userEntity.exercises);
        assertNotNull(this.userEntity.comments);
        assertTrue(this.userEntity.exercises.isEmpty());
        assertTrue(this.userEntity.comments.isEmpty());
    }

    @Test
    @DisplayName("Should set boolean fields correctly")
    void shouldSetBooleanFieldsCorrectly() {
        // When
        this.userEntity.banned = true;
        this.userEntity.activated = false;

        // Then
        assertTrue(this.userEntity.banned);
        assertFalse(this.userEntity.activated);
    }
}