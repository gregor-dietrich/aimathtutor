package de.vptr.aimathtutor.rest.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.rest.entity.PostCommentEntity;
import de.vptr.aimathtutor.rest.entity.PostEntity;
import de.vptr.aimathtutor.rest.entity.UserEntity;
import de.vptr.aimathtutor.rest.entity.UserRankEntity;

class UserViewDtoTest {

    private UserViewDto userViewDto;

    @BeforeEach
    void setUp() {
        userViewDto = new UserViewDto();
    }

    @Test
    @DisplayName("Should create UserViewDto with default constructor")
    void shouldCreateUserViewDtoWithDefaultConstructor() {
        // Then
        assertNull(userViewDto.id);
        assertNull(userViewDto.username);
        assertNull(userViewDto.email);
        assertNull(userViewDto.rankId);
        assertNull(userViewDto.rankName);
        assertNull(userViewDto.banned);
        assertNull(userViewDto.activated);
        assertNull(userViewDto.lastIp);
        assertNull(userViewDto.created);
        assertNull(userViewDto.lastLogin);
        assertNull(userViewDto.postsCount);
        assertNull(userViewDto.commentsCount);
    }

    @Test
    @DisplayName("Should create UserViewDto from UserEntity")
    void shouldCreateUserViewDtoFromUserEntity() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        UserEntity userEntity = new UserEntity();
        UserRankEntity userRank = new UserRankEntity();

        List<PostEntity> posts = new ArrayList<>();
        posts.add(new PostEntity());
        posts.add(new PostEntity());

        List<PostCommentEntity> comments = new ArrayList<>();
        comments.add(new PostCommentEntity());

        userEntity.id = 1L;
        userEntity.username = "testuser";
        userEntity.email = "test@example.com";
        userRank.id = 2L;
        userRank.name = "User";
        userEntity.rank = userRank;
        userEntity.banned = false;
        userEntity.activated = true;
        userEntity.lastIp = "192.168.1.1";
        userEntity.created = now;
        userEntity.lastLogin = now;
        userEntity.posts = posts;
        userEntity.comments = comments;

        // When
        UserViewDto dto = new UserViewDto(userEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("testuser", dto.username);
        assertEquals("test@example.com", dto.email);
        assertEquals(2L, dto.rankId);
        assertEquals("User", dto.rankName);
        assertFalse(dto.banned);
        assertTrue(dto.activated);
        assertEquals("192.168.1.1", dto.lastIp);
        assertEquals(now, dto.created);
        assertEquals(now, dto.lastLogin);
        assertEquals(2L, dto.postsCount);
        assertEquals(1L, dto.commentsCount);
    }

    @Test
    @DisplayName("Should handle null UserEntity")
    void shouldHandleNullUserEntity() {
        // When
        UserViewDto dto = new UserViewDto(null);

        // Then - All fields should remain null
        assertNull(dto.id);
        assertNull(dto.username);
        assertNull(dto.email);
        assertNull(dto.rankId);
        assertNull(dto.rankName);
        assertNull(dto.banned);
        assertNull(dto.activated);
        assertNull(dto.lastIp);
        assertNull(dto.created);
        assertNull(dto.lastLogin);
        assertNull(dto.postsCount);
        assertNull(dto.commentsCount);
    }

    @Test
    @DisplayName("Should handle UserEntity with null rank")
    void shouldHandleUserEntityWithNullRank() {
        // Given
        UserEntity userEntity = new UserEntity();
        userEntity.id = 1L;
        userEntity.username = "testuser";
        userEntity.rank = null;
        userEntity.posts = new ArrayList<>();
        userEntity.comments = new ArrayList<>();

        // When
        UserViewDto dto = new UserViewDto(userEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("testuser", dto.username);
        assertNull(dto.rankId);
        assertNull(dto.rankName);
        assertEquals(0L, dto.postsCount);
        assertEquals(0L, dto.commentsCount);
    }

    @Test
    @DisplayName("Should handle UserEntity with null collections")
    void shouldHandleUserEntityWithNullCollections() {
        // Given
        UserEntity userEntity = new UserEntity();
        UserRankEntity userRank = new UserRankEntity();
        userEntity.id = 1L;
        userEntity.username = "testuser";
        userRank.id = 1L;
        userRank.name = "User";
        userEntity.rank = userRank;
        userEntity.posts = null;
        userEntity.comments = null;

        // When
        UserViewDto dto = new UserViewDto(userEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("testuser", dto.username);
        assertEquals(1L, dto.rankId);
        assertEquals("User", dto.rankName);
        assertEquals(0L, dto.postsCount);
        assertEquals(0L, dto.commentsCount);
    }

    @Test
    @DisplayName("Should convert to UserDto correctly")
    void shouldConvertToUserDtoCorrectly() {
        // Given
        userViewDto.id = 1L;
        userViewDto.username = "testuser";
        userViewDto.email = "test@example.com";
        userViewDto.rankId = 2L;
        userViewDto.banned = false;

        // When
        UserDto userDto = userViewDto.toUserDto();

        // Then
        assertEquals(1L, userDto.id);
        assertEquals("testuser", userDto.username);
        assertEquals("test@example.com", userDto.email);
        assertEquals(2L, userDto.rankId);
        assertFalse(userDto.banned);
        assertNull(userDto.password); // Should not be included
    }

    @Test
    @DisplayName("Should handle all field assignments")
    void shouldHandleAllFieldAssignments() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        userViewDto.id = 1L;
        userViewDto.username = "testuser";
        userViewDto.email = "test@example.com";
        userViewDto.rankId = 2L;
        userViewDto.rankName = "Administrator";
        userViewDto.banned = true;
        userViewDto.activated = false;
        userViewDto.lastIp = "192.168.1.100";
        userViewDto.created = now;
        userViewDto.lastLogin = now;
        userViewDto.postsCount = 5L;
        userViewDto.commentsCount = 10L;

        // Then
        assertEquals(1L, userViewDto.id);
        assertEquals("testuser", userViewDto.username);
        assertEquals("test@example.com", userViewDto.email);
        assertEquals(2L, userViewDto.rankId);
        assertEquals("Administrator", userViewDto.rankName);
        assertTrue(userViewDto.banned);
        assertFalse(userViewDto.activated);
        assertEquals("192.168.1.100", userViewDto.lastIp);
        assertEquals(now, userViewDto.created);
        assertEquals(now, userViewDto.lastLogin);
        assertEquals(5L, userViewDto.postsCount);
        assertEquals(10L, userViewDto.commentsCount);
    }
}