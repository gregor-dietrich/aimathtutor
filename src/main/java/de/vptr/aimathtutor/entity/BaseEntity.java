package de.vptr.aimathtutor.entity;

import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;

/**
 * Base entity providing common fields for all JPA entities: auto-generated primary key, optimistic locking version, and
 * a ULID-based public identifier.
 */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    public String publicId;

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
