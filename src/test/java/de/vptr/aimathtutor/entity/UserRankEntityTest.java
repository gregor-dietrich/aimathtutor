package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.service.UserRankService;

class UserRankEntityTest {

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("A rank holding one permission the other lacks grants beyond it")
    void permissionMissingFromCeilingGrantsBeyond(final Permission permission) {
        final var rank = rankWith(EnumSet.of(permission));

        assertTrue(rank.grantsBeyond(new UserRankEntity().granted()));
        assertTrue(rank.grantsBeyond(EnumSet.complementOf(EnumSet.of(permission))));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("A rank whose permissions the other also holds does not grant beyond it")
    void permissionInCeilingDoesNotGrantBeyond(final Permission permission) {
        final var rank = rankWith(EnumSet.of(permission));

        assertFalse(rank.grantsBeyond(rank.granted()));
        assertFalse(rank.grantsBeyond(EnumSet.allOf(Permission.class)));
        assertFalse(new UserRankEntity().grantsBeyond(rank.granted()));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("granted() lists exactly the permission that is set")
    void grantedListsTheSetPermission(final Permission permission) {
        assertEquals(EnumSet.of(permission), rankWith(EnumSet.of(permission)).granted());
        assertTrue(new UserRankEntity().granted().isEmpty());
    }

    private static UserRankEntity rankWith(final EnumSet<Permission> permissions) {
        final var rank = new UserRankEntity();
        permissions.forEach(p -> UserRankService.setPermission(rank, p, true));
        return rank;
    }
}
