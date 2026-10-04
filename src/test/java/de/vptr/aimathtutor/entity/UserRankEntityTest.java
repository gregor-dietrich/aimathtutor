package de.vptr.aimathtutor.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class UserRankEntityTest {

    /**
     * Every permission flag: the entity's boolean instance fields, found by reflection so a new flag can't be missed.
     */
    static Stream<String> permissionFlags() {
        return Arrays.stream(UserRankEntity.class.getDeclaredFields())
                .filter(f -> f.getType() == boolean.class && !Modifier.isStatic(f.getModifiers())).map(Field::getName);
    }

    @Test
    @DisplayName("permissions() lists every boolean flag of the rank")
    void permissionsCoverEveryFlag() {
        assertEquals(permissionFlags().count(), new UserRankEntity().permissions().size());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("permissionFlags")
    @DisplayName("A rank holding one flag the other lacks grants beyond it")
    void flagMissingFromCeilingGrantsBeyond(final String flag) throws ReflectiveOperationException {
        final UserRankEntity rank = rankWith(List.of(flag));
        final UserRankEntity everythingElse = rankWith(permissionFlags().filter(f -> !f.equals(flag)).toList());

        assertTrue(rank.grantsBeyond(new UserRankEntity().permissions()));
        assertTrue(rank.grantsBeyond(everythingElse.permissions()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("permissionFlags")
    @DisplayName("A rank whose flags the other also holds does not grant beyond it")
    void flagInCeilingDoesNotGrantBeyond(final String flag) throws ReflectiveOperationException {
        final UserRankEntity rank = rankWith(List.of(flag));

        assertFalse(rank.grantsBeyond(rank.permissions()));
        assertFalse(rank.grantsBeyond(rankWith(permissionFlags().toList()).permissions()));
        assertFalse(new UserRankEntity().grantsBeyond(rank.permissions()));
    }

    private static UserRankEntity rankWith(final List<String> flags) throws ReflectiveOperationException {
        final UserRankEntity rank = new UserRankEntity();
        for (final String flag : flags) {
            UserRankEntity.class.getDeclaredField(flag).setBoolean(rank, true);
        }
        return rank;
    }
}
