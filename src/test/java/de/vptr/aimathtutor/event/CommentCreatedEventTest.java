package de.vptr.aimathtutor.event;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CommentCreatedEvent CDI event.
 * Tests cover: construction, getters, immutability, data integrity.
 */
@DisplayName("CommentCreatedEvent Tests")
class CommentCreatedEventTest {

    @Test
    @DisplayName("Should construct CommentCreatedEvent with all parameters")
    void testEventConstruction() {
        // Given
        final Long commentId = 1L;
        final Long exerciseId = 2L;
        final Long userId = 3L;
        final String username = "testuser";
        final String content = "Test comment";
        final LocalDateTime createdAt = LocalDateTime.now();

        // When
        final CommentCreatedEvent event = new CommentCreatedEvent(
                commentId, exerciseId, userId, username, content, createdAt);

        // Then
        assertNotNull(event);
        assertEquals(commentId, event.getCommentId());
        assertEquals(exerciseId, event.getExerciseId());
        assertEquals(userId, event.getUserId());
        assertEquals(username, event.getUsername());
        assertEquals(content, event.getContent());
        assertEquals(createdAt, event.getCreatedAt());
    }

    @Test
    @DisplayName("Should return correct comment ID")
    void testGetCommentId() {
        // Given
        final Long expectedId = 42L;
        final CommentCreatedEvent event = new CommentCreatedEvent(
                expectedId, 1L, 1L, "user", "content", LocalDateTime.now());

        // When
        final Long actualId = event.getCommentId();

        // Then
        assertEquals(expectedId, actualId);
    }

    @Test
    @DisplayName("Should return correct exercise ID")
    void testGetExerciseId() {
        // Given
        final Long expectedExerciseId = 99L;
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, expectedExerciseId, 1L, "user", "content", LocalDateTime.now());

        // When
        final Long actualExerciseId = event.getExerciseId();

        // Then
        assertEquals(expectedExerciseId, actualExerciseId);
    }

    @Test
    @DisplayName("Should return correct user ID")
    void testGetUserId() {
        // Given
        final Long expectedUserId = 77L;
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, expectedUserId, "user", "content", LocalDateTime.now());

        // When
        final Long actualUserId = event.getUserId();

        // Then
        assertEquals(expectedUserId, actualUserId);
    }

    @Test
    @DisplayName("Should return correct username")
    void testGetUsername() {
        // Given
        final String expectedUsername = "alice_wonderland";
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, expectedUsername, "content", LocalDateTime.now());

        // When
        final String actualUsername = event.getUsername();

        // Then
        assertEquals(expectedUsername, actualUsername);
    }

    @Test
    @DisplayName("Should return correct comment content")
    void testGetContent() {
        // Given
        final String expectedContent = "This is my insightful comment!";
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", expectedContent, LocalDateTime.now());

        // When
        final String actualContent = event.getContent();

        // Then
        assertEquals(expectedContent, actualContent);
    }

    @Test
    @DisplayName("Should return correct creation timestamp")
    void testGetCreatedAt() {
        // Given
        final LocalDateTime expectedTime = LocalDateTime.of(2025, 10, 18, 15, 30, 0);
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", "content", expectedTime);

        // When
        final LocalDateTime actualTime = event.getCreatedAt();

        // Then
        assertEquals(expectedTime, actualTime);
    }

    @Test
    @DisplayName("Should handle long content strings correctly")
    void testEventWithLongContent() {
        // Given
        final String longContent = "x".repeat(1000);
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", longContent, LocalDateTime.now());

        // When
        final String retrievedContent = event.getContent();

        // Then
        assertEquals(longContent, retrievedContent);
        assertEquals(1000, retrievedContent.length());
    }

    @Test
    @DisplayName("Should handle special characters in content")
    void testEventWithSpecialCharacters() {
        // Given
        final String specialContent = "Test @#$%^&*()_+-=[]{}|;':\",./<>?";
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", specialContent, LocalDateTime.now());

        // When
        final String retrievedContent = event.getContent();

        // Then
        assertEquals(specialContent, retrievedContent);
    }

    @Test
    @DisplayName("Should handle special characters in username")
    void testEventWithSpecialUsername() {
        // Given
        final String specialUsername = "user_123-ABC.test";
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, specialUsername, "content", LocalDateTime.now());

        // When
        final String retrievedUsername = event.getUsername();

        // Then
        assertEquals(specialUsername, retrievedUsername);
    }

    @Test
    @DisplayName("Should handle null timestamp in construction")
    void testEventWithNullTimestamp() {
        // Given/When
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", "content", null);

        // Then
        assertNull(event.getCreatedAt());
    }

    @Test
    @DisplayName("Should handle null content in construction")
    void testEventWithNullContent() {
        // Given/When
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", null, LocalDateTime.now());

        // Then
        assertNull(event.getContent());
    }

    @Test
    @DisplayName("Should handle null username in construction")
    void testEventWithNullUsername() {
        // Given/When
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, null, "content", LocalDateTime.now());

        // Then
        assertNull(event.getUsername());
    }

    @Test
    @DisplayName("Should maintain data integrity across multiple getter calls")
    void testDataIntegrity() {
        // Given
        final Long commentId = 1L;
        final Long exerciseId = 2L;
        final Long userId = 3L;
        final String username = "testuser";
        final String content = "Test comment";
        final LocalDateTime createdAt = LocalDateTime.now();

        final CommentCreatedEvent event = new CommentCreatedEvent(
                commentId, exerciseId, userId, username, content, createdAt);

        // When/Then - Call getters multiple times and verify consistency
        for (int i = 0; i < 5; i++) {
            assertEquals(commentId, event.getCommentId());
            assertEquals(exerciseId, event.getExerciseId());
            assertEquals(userId, event.getUserId());
            assertEquals(username, event.getUsername());
            assertEquals(content, event.getContent());
            assertEquals(createdAt, event.getCreatedAt());
        }
    }

    @Test
    @DisplayName("Should handle zero and negative IDs (edge case)")
    void testEventWithZeroAndNegativeIds() {
        // Given - Zero IDs
        final CommentCreatedEvent eventZero = new CommentCreatedEvent(
                0L, 0L, 0L, "user", "content", LocalDateTime.now());

        // Then
        assertEquals(0L, eventZero.getCommentId());
        assertEquals(0L, eventZero.getExerciseId());
        assertEquals(0L, eventZero.getUserId());

        // Given - Negative IDs (should not happen in practice but let's test)
        final CommentCreatedEvent eventNegative = new CommentCreatedEvent(
                -1L, -2L, -3L, "user", "content", LocalDateTime.now());

        // Then
        assertEquals(-1L, eventNegative.getCommentId());
        assertEquals(-2L, eventNegative.getExerciseId());
        assertEquals(-3L, eventNegative.getUserId());
    }

    @Test
    @DisplayName("Should handle empty string content")
    void testEventWithEmptyContent() {
        // Given
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", "", LocalDateTime.now());

        // When
        final String content = event.getContent();

        // Then
        assertEquals("", content);
        assertTrue(content.isEmpty());
    }

    @Test
    @DisplayName("Should handle whitespace-only content")
    void testEventWithWhitespaceContent() {
        // Given
        final String whitespaceContent = "   \t\n  ";
        final CommentCreatedEvent event = new CommentCreatedEvent(
                1L, 1L, 1L, "user", whitespaceContent, LocalDateTime.now());

        // When
        final String content = event.getContent();

        // Then
        assertEquals(whitespaceContent, content);
    }

    @Test
    @DisplayName("Should handle very large ID values")
    void testEventWithLargeIds() {
        // Given
        final Long largeId = Long.MAX_VALUE;
        final CommentCreatedEvent event = new CommentCreatedEvent(
                largeId, largeId, largeId, "user", "content", LocalDateTime.now());

        // When
        final Long retrievedId = event.getCommentId();

        // Then
        assertEquals(largeId, retrievedId);
    }
}
