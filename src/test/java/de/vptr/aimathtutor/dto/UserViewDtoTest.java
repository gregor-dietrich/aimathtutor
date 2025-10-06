package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.UserDto;
import de.vptr.aimathtutor.dto.UserViewDto;
import de.vptr.aimathtutor.entity.CommentEntity;
import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;

class UserViewDtoTest {

    private UserViewDto userViewDto;

    @BeforeEach
    void setUp() {
        this.userViewDto = new UserViewDto();
    }

    @Test
    @DisplayName("Should create UserViewDto with default constructor")
    void shouldCreateUserViewDtoWithDefaultConstructor() {
        // Then
        assertNull(this.userViewDto.id);
        assertNull(this.userViewDto.username);
        assertNull(this.userViewDto.email);
        assertNull(this.userViewDto.rankId);
        assertNull(this.userViewDto.rankName);
        assertNull(this.userViewDto.banned);
        assertNull(this.userViewDto.activated);
        assertNull(this.userViewDto.lastIp);
        assertNull(this.userViewDto.created);
        assertNull(this.userViewDto.lastLogin);
        assertNull(this.userViewDto.exercisesCount);
        assertNull(this.userViewDto.commentsCount);
    }

    @Test
    @DisplayName("Should create UserViewDto from UserEntity")
    void shouldCreateUserViewDtoFromUserEntity() {
        // Given
        final LocalDateTime now = LocalDateTime.now();
        final UserEntity userEntity = new UserEntity();
        final UserRankEntity userRank = new UserRankEntity();

        final List<ExerciseEntity> exercises = new ArrayList<>();
        exercises.add(new ExerciseEntity());
        exercises.add(new ExerciseEntity());

        final List<CommentEntity> comments = new ArrayList<>();
        comments.add(new CommentEntity());

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
        userEntity.exercises = exercises;
        userEntity.comments = comments;

        // When
        final UserViewDto dto = new UserViewDto(userEntity);

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
        assertEquals(2L, dto.exercisesCount);
        assertEquals(1L, dto.commentsCount);
    }

    @Test
    @DisplayName("Should handle null UserEntity")
    void shouldHandleNullUserEntity() {
        // When
        final UserViewDto dto = new UserViewDto(null);

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
        assertNull(dto.exercisesCount);
        assertNull(dto.commentsCount);
    }

    @Test
    @DisplayName("Should handle UserEntity with null rank")
    void shouldHandleUserEntityWithNullRank() {
        // Given
        final UserEntity userEntity = new UserEntity();
        userEntity.id = 1L;
        userEntity.username = "testuser";
        userEntity.rank = null;
        userEntity.exercises = new ArrayList<>();
        userEntity.comments = new ArrayList<>();

        // When
        final UserViewDto dto = new UserViewDto(userEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("testuser", dto.username);
        assertNull(dto.rankId);
        assertNull(dto.rankName);
        assertEquals(0L, dto.exercisesCount);
        assertEquals(0L, dto.commentsCount);
    }

    @Test
    @DisplayName("Should handle UserEntity with null collections")
    void shouldHandleUserEntityWithNullCollections() {
        // Given
        final UserEntity userEntity = new UserEntity();
        final UserRankEntity userRank = new UserRankEntity();
        userEntity.id = 1L;
        userEntity.username = "testuser";
        userRank.id = 1L;
        userRank.name = "User";
        userEntity.rank = userRank;
        userEntity.exercises = null;
        userEntity.comments = null;

        // When
        final UserViewDto dto = new UserViewDto(userEntity);

        // Then
        assertEquals(1L, dto.id);
        assertEquals("testuser", dto.username);
        assertEquals(1L, dto.rankId);
        assertEquals("User", dto.rankName);
        assertEquals(0L, dto.exercisesCount);
        assertEquals(0L, dto.commentsCount);
    }

    @Test
    @DisplayName("Should convert to UserDto correctly")
    void shouldConvertToUserDtoCorrectly() {
        // Given
        this.userViewDto.id = 1L;
        this.userViewDto.username = "testuser";
        this.userViewDto.email = "test@example.com";
        this.userViewDto.rankId = 2L;
        this.userViewDto.banned = false;

        // When
        final UserDto userDto = this.userViewDto.toUserDto();

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
        final LocalDateTime now = LocalDateTime.now();

        // When
        this.userViewDto.id = 1L;
        this.userViewDto.username = "testuser";
        this.userViewDto.email = "test@example.com";
        this.userViewDto.rankId = 2L;
        this.userViewDto.rankName = "Administrator";
        this.userViewDto.banned = true;
        this.userViewDto.activated = false;
        this.userViewDto.lastIp = "192.168.1.100";
        this.userViewDto.created = now;
        this.userViewDto.lastLogin = now;
        this.userViewDto.exercisesCount = 5L;
        this.userViewDto.commentsCount = 10L;

        // Then
        assertEquals(1L, this.userViewDto.id);
        assertEquals("testuser", this.userViewDto.username);
        assertEquals("test@example.com", this.userViewDto.email);
        assertEquals(2L, this.userViewDto.rankId);
        assertEquals("Administrator", this.userViewDto.rankName);
        assertTrue(this.userViewDto.banned);
        assertFalse(this.userViewDto.activated);
        assertEquals("192.168.1.100", this.userViewDto.lastIp);
        assertEquals(now, this.userViewDto.created);
        assertEquals(now, this.userViewDto.lastLogin);
        assertEquals(5L, this.userViewDto.exercisesCount);
        assertEquals(10L, this.userViewDto.commentsCount);
    }
}