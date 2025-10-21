package de.vptr.aimathtutor.entity;

import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "lessons")
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
    public boolean isRootLesson() {
        return this.parent == null;
    }

    // Helper method to get all sub-lessons recursively
    public static List<LessonEntity> findByParentId(final Long parentId) {
        if (parentId == null) {
            return find("parent IS NULL").list();
        }
        return find("parent.id", parentId).list();
    }

    // Helper method to find root lessons
    public static List<LessonEntity> findRootLessons() {
        return find("parent IS NULL").list();
    }
}
