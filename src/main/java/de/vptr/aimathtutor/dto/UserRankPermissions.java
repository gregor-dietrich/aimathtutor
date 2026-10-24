package de.vptr.aimathtutor.dto;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

import jakarta.annotation.Nullable;

/**
 * Shared permission fields for {@link UserRankDto} and {@link UserRankViewDto}. Extracted to eliminate CPD duplication.
 * A permission with no entry was not provided, which a partial update leaves untouched.
 */
public abstract class UserRankPermissions {

    private final EnumMap<Permission, Boolean> permissions = new EnumMap<>(Permission.class);

    protected UserRankPermissions() {
    }

    @Nullable
    public String publicId;

    /**
     * The version the caller loaded; a full update is refused when the stored version differs. Null skips the check.
     */
    @Nullable
    public Long version;

    /**
     * The provided value of a permission.
     *
     * @param permission
     *            the permission to read
     * @return the value, or null if it was not provided
     */
    @Nullable
    public Boolean get(final Permission permission) {
        return this.permissions.get(permission);
    }

    /**
     * Sets a permission.
     *
     * @param permission
     *            the permission to write
     * @param value
     *            the value; null marks it as not provided
     */
    public void set(final Permission permission, @Nullable final Boolean value) {
        if (value == null) {
            this.permissions.remove(permission);
        } else {
            this.permissions.put(permission, value);
        }
    }

    /**
     * Whether a permission was provided and is granted.
     *
     * @param permission
     *            the permission to test
     * @return true only if the value is true
     */
    public boolean has(final Permission permission) {
        return Boolean.TRUE.equals(this.get(permission));
    }

    /**
     * The permissions that are granted.
     *
     * @return a new set
     */
    public Set<Permission> granted() {
        final var granted = EnumSet.noneOf(Permission.class);
        for (final var permission : Permission.values()) {
            if (this.has(permission)) {
                granted.add(permission);
            }
        }
        return granted;
    }
}
