package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;

class UserRankViewDtoTest {

    @Test
    @DisplayName("Constructing a rank display DTO never traverses rank membership")
    void constructorDoesNotInitializeLazyUsers() {
        @SuppressWarnings("unchecked")
        final List<UserEntity> detachedUsers = mock(List.class);
        when(detachedUsers.size()).thenThrow(new LazyInitializationException("simulated detached users collection"));
        final var rank = new UserRankEntity();
        rank.publicId = "rank-id";
        rank.name = "Teacher";
        rank.exerciseAdd = true;
        rank.users = detachedUsers;

        final var dto = new UserRankViewDto(rank);

        assertEquals(rank.publicId, dto.publicId);
        assertEquals("Teacher", dto.name);
        assertTrue(dto.hasAnyExercisePermission());
        assertFalse(dto.canAdminView());
        verifyNoInteractions(detachedUsers);
    }
}
