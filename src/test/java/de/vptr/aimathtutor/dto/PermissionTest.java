package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.vptr.aimathtutor.entity.UserRankEntity;
import de.vptr.aimathtutor.service.UserRankService;
import jakarta.persistence.Column;

class PermissionTest {

    /** Every primitive {@code boolean} field of the rank counts as a permission. */
    private static Field[] permissionFields() {
        return Arrays.stream(UserRankEntity.class.getDeclaredFields())
                .filter(f -> f.getType() == boolean.class && !Modifier.isStatic(f.getModifiers()))
                .toArray(Field[]::new);
    }

    @Test
    @DisplayName("Each boolean field of the rank maps to exactly one constant, and each constant to a field")
    void fieldsAndConstantsMatch() {
        final var fields = Arrays.stream(permissionFields()).map(Field::getName).sorted().toList();
        final var constants = Arrays.stream(Permission.values()).map(Permission::field).sorted().toList();

        assertEquals(fields, constants);
        assertEquals(constants.size(), constants.stream().distinct().count());
    }

    @Test
    @DisplayName("Each field's column name equals the constant's column()")
    void columnsMatch() {
        for (final var field : permissionFields()) {
            final var permission = Arrays.stream(Permission.values()).filter(p -> p.field().equals(field.getName()))
                    .findFirst().orElseThrow();
            assertEquals(permission.column(), field.getAnnotation(Column.class).name(), field.getName());
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(Permission.class)
    @DisplayName("set(p, true) sets exactly the field of p, and has(p) agrees")
    void setSetsExactlyOneField(final Permission permission) throws ReflectiveOperationException {
        final var rank = new UserRankEntity();
        UserRankService.setPermission(rank, permission, true);

        assertTrue(rank.has(permission));
        for (final var field : permissionFields()) {
            assertEquals(field.getName().equals(permission.field()), field.getBoolean(rank), field.getName());
        }
        UserRankService.setPermission(rank, permission, false);
        assertFalse(rank.has(permission));
    }

    @Test
    @DisplayName("The groups partition all constants")
    void groupsPartitionConstants() {
        final var seen = EnumSet.noneOf(Permission.class);
        for (final var group : Permission.Group.values()) {
            for (final var permission : group.permissions()) {
                assertEquals(group, permission.group());
                assertTrue(seen.add(permission), permission.name());
            }
        }
        assertEquals(EnumSet.allOf(Permission.class), seen);
    }

    @Test
    @DisplayName("granted() is a snapshot: later changes to the rank do not show in it")
    void grantedReturnsSnapshot() {
        final var rank = new UserRankEntity();
        UserRankService.setPermission(rank, Permission.USER_EDIT, true);

        final var captured = rank.granted();
        UserRankService.setPermission(rank, Permission.USER_EDIT, false);
        UserRankService.setPermission(rank, Permission.LESSON_ADD, true);

        assertEquals(EnumSet.of(Permission.USER_EDIT), captured);
    }
}
