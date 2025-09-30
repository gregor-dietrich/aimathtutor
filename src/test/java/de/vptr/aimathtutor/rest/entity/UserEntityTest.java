package de.vptr.aimathtutor.rest.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

class UserEntityTest {

    private UserEntity userEntity;
    private UserRankEntity userRank;

    @BeforeEach
    void setUp() {
        userEntity = new UserEntity();
        userRank = new UserRankEntity();
        userRank.id = 1L;
        userRank.name = "User";
    }

    @Test
    @DisplayName("Should create UserEntity with all fields")
    void shouldCreateUserEntityWithAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        userEntity.id = 1L;
        userEntity.username = "testuser";
        userEntity.password = "hashedpassword";
        userEntity.salt = "randomsalt";
        userEntity.rank = userRank;
        userEntity.email = "test@example.com";
        userEntity.banned = false;
        userEntity.activated = true;
        userEntity.activationKey = "activation123";
        userEntity.lastIp = "192.168.1.1";
        userEntity.created = now;
        userEntity.lastLogin = now;

        // Then
        assertEquals(1L, userEntity.id);
        assertEquals("testuser", userEntity.username);
        assertEquals("hashedpassword", userEntity.password);
        assertEquals("randomsalt", userEntity.salt);
        assertEquals(userRank, userEntity.rank);
        assertEquals("test@example.com", userEntity.email);
        assertFalse(userEntity.banned);
        assertTrue(userEntity.activated);
        assertEquals("activation123", userEntity.activationKey);
        assertEquals("192.168.1.1", userEntity.lastIp);
        assertEquals(now, userEntity.created);
        assertEquals(now, userEntity.lastLogin);
    }

    @Test
    @DisplayName("Should handle null values for optional fields")
    void shouldHandleNullValuesForOptionalFields() {
        // When
        userEntity.id = 1L;
        userEntity.username = "testuser";
        userEntity.password = "password";
        userEntity.salt = "salt";
        userEntity.rank = userRank;
        userEntity.email = null;
        userEntity.banned = null;
        userEntity.activated = null;
        userEntity.activationKey = null;
        userEntity.lastIp = null;
        userEntity.created = null;
        userEntity.lastLogin = null;

        // Then
        assertNull(userEntity.email);
        assertNull(userEntity.banned);
        assertNull(userEntity.activated);
        assertNull(userEntity.activationKey);
        assertNull(userEntity.lastIp);
        assertNull(userEntity.created);
        assertNull(userEntity.lastLogin);
    }

    @Test
    @DisplayName("Should handle collections properly")
    void shouldHandleCollectionsProperly() {
        // Given
        List<PostEntity> posts = new ArrayList<>();
        List<PostCommentEntity> comments = new ArrayList<>();

        PostEntity post = new PostEntity();
        post.id = 1L;
        posts.add(post);

        PostCommentEntity comment = new PostCommentEntity();
        comment.id = 1L;
        comments.add(comment);

        // When
        userEntity.posts = posts;
        userEntity.comments = comments;

        // Then
        assertNotNull(userEntity.posts);
        assertNotNull(userEntity.comments);
        assertEquals(1, userEntity.posts.size());
        assertEquals(1, userEntity.comments.size());
        assertEquals(post, userEntity.posts.get(0));
        assertEquals(comment, userEntity.comments.get(0));
    }

    @Test
    @DisplayName("Should handle empty collections")
    void shouldHandleEmptyCollections() {
        // When
        userEntity.posts = new ArrayList<>();
        userEntity.comments = new ArrayList<>();

        // Then
        assertNotNull(userEntity.posts);
        assertNotNull(userEntity.comments);
        assertTrue(userEntity.posts.isEmpty());
        assertTrue(userEntity.comments.isEmpty());
    }

    @Test
    @DisplayName("Should set boolean fields correctly")
    void shouldSetBooleanFieldsCorrectly() {
        // When
        userEntity.banned = true;
        userEntity.activated = false;

        // Then
        assertTrue(userEntity.banned);
        assertFalse(userEntity.activated);
    }
}