package de.vptr.aimathtutor.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing lessons in the system.
 */
@Entity
@Table(name = "lessons", indexes = {
        @Index(name = "idx_lesson_parent", columnList = "parent_id")
})
@NamedQueries({
        @NamedQuery(name = "Lesson.findAllOrdered", query = "FROM LessonEntity ORDER BY id DESC"),
        @NamedQuery(name = "Lesson.findRootLessons", query = "FROM LessonEntity WHERE parent IS NULL ORDER BY id DESC"),
        @NamedQuery(name = "Lesson.findByParentId", query = "FROM LessonEntity WHERE parent.id = :p ORDER BY id DESC"),
        @NamedQuery(name = "Lesson.searchByName", query = "FROM LessonEntity WHERE LOWER(name) LIKE :s")
})
public class LessonEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public LessonEntity parent;

    @OneToMany(mappedBy = "parent")
    public List<LessonEntity> children;

    @OneToMany(mappedBy = "lesson")
    public List<ExerciseEntity> exercises;

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
