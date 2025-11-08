package de.vptr.aimathtutor.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing lessons in the system.
 */
@Entity
@Table(name = "lessons")
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

    /**
     * Finds all lessons that have a specific parent lesson.
     *
     * @param parentId the ID of the parent lesson; if null, returns all root
     *                 lessons
     * @return a list of {@link LessonEntity} objects that are children of the
     *         parent
     */
    public static List<LessonEntity> findByParentId(final Long parentId) {
        if (parentId == null) {
            return find("parent IS NULL").list();
        }
        return find("parent.id", parentId).list();
    }

    // Helper method to find root lessons

    /**
     * Finds all root-level lessons (lessons with no parent).
     *
     * @return a list of root {@link LessonEntity} objects
     */
    public static List<LessonEntity> findRootLessons() {
        return find("parent IS NULL").list();
    }
}
