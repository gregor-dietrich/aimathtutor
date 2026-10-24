package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.vptr.aimathtutor.entity.UserEntity;
import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.service.UserRankService;

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
        UserRankService.setPermission(rank, Permission.EXERCISE_ADD, true);
        rank.users = detachedUsers;

        final var dto = new UserRankViewDto(rank);

        assertEquals(rank.publicId, dto.publicId);
        assertEquals("Teacher", dto.name);
        assertTrue(dto.hasAny(Permission.Group.EXERCISE));
        assertFalse(dto.has(Permission.ADMIN_VIEW));
        verifyNoInteractions(detachedUsers);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("The view copies every permission and converts back to an equal DTO")
    void copiesEveryPermissionAndRoundTrips(final Permission permission) {
        final var rank = new UserRankEntity();
        rank.publicId = "rank-id";
        rank.version = 7L;
        rank.name = "Teacher";
        UserRankService.setPermission(rank, permission, true);

        final var view = new UserRankViewDto(rank);
        final var dto = view.toUserRankDto();

        assertEquals(EnumSet.of(permission), view.granted());
        for (final var other : Permission.values()) {
            assertEquals(other == permission, view.get(other), other.name());
            assertEquals(view.get(other), dto.get(other), other.name());
        }
        assertEquals("rank-id", dto.publicId);
        assertEquals(7L, dto.version);
        assertEquals("Teacher", dto.name);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("grantsBeyond is true exactly when a granted permission is missing from the ceiling")
    void grantsBeyondTheCeiling(final Permission permission) {
        final var view = new UserRankViewDto();
        view.set(permission, true);

        assertTrue(view.grantsBeyond(EnumSet.noneOf(Permission.class)));
        assertTrue(view.grantsBeyond(EnumSet.complementOf(EnumSet.of(permission))));
        assertFalse(view.grantsBeyond(EnumSet.of(permission)));
        assertFalse(new UserRankViewDto().grantsBeyond(EnumSet.noneOf(Permission.class)));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("hasAny is true for the group of a granted permission only")
    void hasAnyFollowsTheGroup(final Permission permission) {
        final var view = new UserRankViewDto();
        view.set(permission, true);

        for (final var group : Permission.Group.values()) {
            assertEquals(group == permission.group(), view.hasAny(group), group.name());
        }
    }
}
