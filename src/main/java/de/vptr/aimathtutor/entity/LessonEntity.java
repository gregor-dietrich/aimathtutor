package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import de.vptr.aimathtutor.service.UlidService;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing lessons in the system.
 */
@Entity
@Table(name = "lessons", indexes = {
        @Index(name = "idx_lesson_parent", columnList = "parent_id")
})
@NamedQueries({
        @NamedQuery(name = "Lesson.findAllOrdered", query = "FROM LessonEntity ORDER BY created DESC, id DESC"),
        @NamedQuery(name = "Lesson.findByPublicId", query = "FROM LessonEntity WHERE publicId = :p"),
        @NamedQuery(name = "Lesson.findRootLessons", query = "FROM LessonEntity WHERE parent IS NULL ORDER BY created DESC, id DESC"),
        @NamedQuery(name = "Lesson.findByParentId", query = "FROM LessonEntity WHERE parent.id = :p ORDER BY created DESC, id DESC"),
        @NamedQuery(name = "Lesson.searchByName", query = "FROM LessonEntity WHERE LOWER(name) LIKE :s")
})
public class LessonEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    public String publicId;

    /**
     * Generates a ULID-based public identifier for this entity if not already set.
     */
    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UlidService.generate();
            return;
        }
        UlidService.requireValid(this.publicId);
    }

    @NotBlank
    @Column(nullable = false)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public LessonEntity parent;

    @OneToMany(mappedBy = "parent")
    public List<LessonEntity> children;

    @OneToMany(mappedBy = "lesson")
    public List<ExerciseEntity> exercises;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    // Helper method to check if this is a root lesson

    /**
     * Checks if this lesson is a root-level lesson (has no parent).
     *
     * @return true if this lesson has no parent, false otherwise
     */
    public boolean isRootLesson() {
        return this.parent == null;
    }

    // Helper method to get all sub-lessons recursively
}
