package de.vptr.aimathtutor.entity;

import de.vptr.aimathtutor.util.AppConstants;
import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import jakarta.validation.ValidationException;

/**
 * Base entity providing common fields for all JPA entities: auto-generated primary key, optimistic locking version, and
 * a ULID-based public identifier.
 */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    public Long id;

    @Version
    @Nullable
    public Long version;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    @Nullable
    public String publicId;

    /**
     * Refuses a write whose {@code expected} version differs from the stored one; null skips the check.
     *
     * @param expected
     *            the version the caller loaded, or {@code null} to skip the check
     * @throws ValidationException
     *             with {@link AppConstants#STALE_EDIT_MESSAGE} when the versions differ
     */
    public void requireVersion(@Nullable final Long expected) {
        if (expected != null && !expected.equals(this.version)) {
            throw new ValidationException(AppConstants.STALE_EDIT_MESSAGE);
        }
    }

    /**
     * Generates a ULID-based public identifier for this entity if not already set. Validates existing publicId values
     * on persist.
     */
    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UlidUtil.generate();
            return;
        }
        UlidUtil.requireValid(this.publicId);
    }
}
