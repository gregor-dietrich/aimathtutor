package de.vptr.aimathtutor.dto;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The permissions a user rank can hold, listed once. {@code UserRankEntity} keeps one boolean column per constant and
 * reads it through {@code has}, {@code UserRankService.setPermission} writes it, both by switch; the DTOs hold them in
 * an {@code EnumMap}.
 * <p>
 * A new permission needs its constant here, its entity field and column, a migration that adds the column and sets it
 * on every rank holding all the others (existing databases included; otherwise no administrator remains and nobody can
 * grant it), the column in the README's administrator recovery SQL, and the {@code PermissionService} check that
 * enforces it. {@code PermissionTest} and {@code AdminRecoverySqlTest} fail until the entity and the README match.
 */
public enum Permission {
    ADMIN_VIEW(Group.VIEW, Operation.VIEW, "Admin View"),
    EXERCISE_ADD(Group.EXERCISE, Operation.ADD, "Can Add Exercises"),
    EXERCISE_EDIT(Group.EXERCISE, Operation.EDIT, "Can Edit Exercises"),
    EXERCISE_DELETE(Group.EXERCISE, Operation.DELETE, "Can Delete Exercises"),
    LESSON_ADD(Group.LESSON, Operation.ADD, "Can Add Lessons"),
    LESSON_EDIT(Group.LESSON, Operation.EDIT, "Can Edit Lessons"),
    LESSON_DELETE(Group.LESSON, Operation.DELETE, "Can Delete Lessons"),
    COMMENT_ADD(Group.COMMENT, Operation.ADD, "Can Add Comments"),
    COMMENT_EDIT(Group.COMMENT, Operation.EDIT, "Can Edit Comments"),
    COMMENT_DELETE(Group.COMMENT, Operation.DELETE, "Can Delete Comments"),
    USER_ADD(Group.USER, Operation.ADD, "Can Add Users"), USER_EDIT(Group.USER, Operation.EDIT, "Can Edit Users"),
    USER_DELETE(Group.USER, Operation.DELETE, "Can Delete Users"),
    USER_GROUP_ADD(Group.USER_GROUP, Operation.ADD, "Can Add User Groups"),
    USER_GROUP_EDIT(Group.USER_GROUP, Operation.EDIT, "Can Edit User Groups"),
    USER_GROUP_DELETE(Group.USER_GROUP, Operation.DELETE, "Can Delete User Groups"),
    USER_RANK_ADD(Group.USER_RANK, Operation.ADD, "Can Add User Ranks"),
    USER_RANK_EDIT(Group.USER_RANK, Operation.EDIT, "Can Edit User Ranks"),
    USER_RANK_DELETE(Group.USER_RANK, Operation.DELETE, "Can Delete User Ranks"),
    AI_CONFIG_EDIT(Group.AI_CONFIG, Operation.EDIT, "Can Edit AI Configuration");

    private final Group group;
    private final Operation operation;
    private final String label;

    Permission(final Group group, final Operation operation, final String label) {
        this.group = group;
        this.operation = operation;
        this.label = label;
    }

    public Group group() {
        return this.group;
    }

    public Operation operation() {
        return this.operation;
    }

    public String label() {
        return this.label;
    }

    /**
     * The name of this permission's column in {@code user_ranks}.
     *
     * @return the lower-case constant name
     */
    public String column() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * The name of this permission's field on {@code UserRankEntity}, for JPQL.
     *
     * @return the camelCase constant name
     */
    public String field() {
        final var camel = new StringBuilder();
        var upper = false;
        for (final char c : this.column().toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                camel.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return camel.toString();
    }

    /**
     * The permissions that pass a filter.
     *
     * @param filter
     *            selects the permissions to keep
     * @return a new set, in declaration order
     */
    public static Set<Permission> where(final Predicate<Permission> filter) {
        return Arrays.stream(values()).filter(filter).collect(() -> EnumSet.noneOf(Permission.class), EnumSet::add,
                EnumSet::addAll);
    }

    /** The sections the permissions are shown in, in display order. */
    public enum Group {
        VIEW("View Permissions", "Admin View"), EXERCISE("Exercise Permissions", "Exercises"),
        LESSON("Lesson Permissions", "Lessons"), COMMENT("Comment Permissions", "Comments"),
        USER("User Permissions", "Users"), USER_GROUP("User Group Permissions", "User Groups"),
        USER_RANK("User Rank Permissions", "User Ranks"), AI_CONFIG("AI Configuration Permissions", "AI Config");

        private final String sectionTitle;
        private final String columnHeader;

        Group(final String sectionTitle, final String columnHeader) {
            this.sectionTitle = sectionTitle;
            this.columnHeader = columnHeader;
        }

        public String sectionTitle() {
            return this.sectionTitle;
        }

        public String columnHeader() {
            return this.columnHeader;
        }

        /**
         * The permissions of this group.
         *
         * @return a new set, in declaration order
         */
        public Set<Permission> permissions() {
            return where(p -> p.group == this);
        }
    }

    /** What a permission lets its holder do. */
    public enum Operation {
        VIEW, ADD, EDIT, DELETE
    }
}
