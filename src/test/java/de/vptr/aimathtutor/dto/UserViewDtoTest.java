package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;

@SuppressWarnings("NullAway")
class UserViewDtoTest {

    @Test
    @DisplayName("Default constructor has null fields")
    void testDefaultConstructor() {
        final var dto = new UserViewDto();
        assertNull(dto.publicId);
        assertNull(dto.username);
        assertNull(dto.email);
        assertNull(dto.rankPublicId);
        assertNull(dto.rankName);
        assertNull(dto.banned);
        assertNull(dto.activated);
        assertNull(dto.created);
        assertNull(dto.exercisesCount);
        assertNull(dto.commentsCount);
        assertNull(dto.userAvatarEmoji);
        assertNull(dto.tutorAvatarEmoji);
    }

    @Test
    @DisplayName("Entity constructor maps fields")
    void testEntityConstructor() {
        final var entity = new UserEntity();
        entity.publicId = "up1";
        entity.username = "alice";
        entity.email = "alice@example.com";
        entity.banned = false;
        entity.activated = true;
        entity.created = LocalDateTime.now(ZoneId.systemDefault());
        entity.userAvatarEmoji = "😀";
        entity.tutorAvatarEmoji = "🎓";

        final var rank = new UserRankEntity();
        rank.publicId = "rp1";
        rank.name = "Student";
        entity.rank = rank;

        final var dto = new UserViewDto(entity);
        assertEquals("up1", dto.publicId);
        assertEquals("alice", dto.username);
        assertEquals("alice@example.com", dto.email);
        assertEquals("rp1", dto.rankPublicId);
        assertEquals("Student", dto.rankName);
        assertEquals(false, dto.banned);
        assertEquals(true, dto.activated);
        assertEquals("😀", dto.userAvatarEmoji);
        assertEquals("🎓", dto.tutorAvatarEmoji);
        assertEquals(0L, dto.exercisesCount);
        assertEquals(0L, dto.commentsCount);
    }

    @Test
    @DisplayName("Entity constructor handles null entity")
    void testEntityConstructorNull() {
        final var dto = new UserViewDto((UserEntity) null);
        assertNull(dto.publicId);
    }

    @Test
    @DisplayName("Entity constructor handles null rank")
    void testEntityConstructorNullRank() {
        final var entity = new UserEntity();
        entity.publicId = "up1";
        entity.rank = null;
        entity.exercises = List.of();
        entity.comments = List.of();

        final var dto = new UserViewDto(entity);
        assertEquals("up1", dto.publicId);
        assertNull(dto.rankPublicId);
        assertNull(dto.rankName);
    }

    @Test
    @DisplayName("toUserDto converts")
    void testToUserDto() {
        final var viewDto = new UserViewDto();
        viewDto.publicId = "up1";
        viewDto.username = "bob";
        viewDto.email = "bob@example.com";
        viewDto.rankPublicId = "rp1";
        viewDto.banned = false;
        viewDto.activated = true;

        final var dto = viewDto.toUserDto();
        assertEquals("up1", dto.publicId);
        assertEquals("bob", dto.username);
        assertEquals("bob@example.com", dto.email);
        assertEquals("rp1", dto.rankPublicId);
        assertEquals(false, dto.banned);
        assertEquals(true, dto.activated);
        assertNull(dto.password);
    }
}
