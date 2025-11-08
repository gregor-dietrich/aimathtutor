package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing math exercises in the system.
 */
@Entity
@Table(name = "exercises")
@NamedQueries({
        @NamedQuery(name = "Exercise.findAllOrdered", query = "FROM ExerciseEntity ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findPublished", query = "FROM ExerciseEntity WHERE published = true ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findByUserId", query = "FROM ExerciseEntity WHERE user.id = :u ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findByLessonId", query = "FROM ExerciseEntity WHERE lesson.id = :l ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findGraspableEnabled", query = "FROM ExerciseEntity WHERE graspableEnabled = true AND published = true ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findGraspableByLesson", query = "FROM ExerciseEntity WHERE graspableEnabled = true AND published = true AND lesson.id = :l ORDER BY id DESC"),
})
public class ExerciseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    public LessonEntity lesson;

    @Column(columnDefinition = "TINYINT(1)")
    public Boolean published = false;

    @Column(columnDefinition = "TINYINT(1)")
    public Boolean commentable = false;

    public LocalDateTime created;

    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "exercise")
    @JsonIgnore
    public List<CommentEntity> comments;

    // Graspable Math Configuration
    @Column(name = "graspable_enabled", columnDefinition = "TINYINT(1)")
    public Boolean graspableEnabled = false;

    @Column(name = "graspable_initial_expression", columnDefinition = "TEXT")
    public String graspableInitialExpression;

    @Column(name = "graspable_target_expression", columnDefinition = "TEXT")
    public String graspableTargetExpression;

    @Column(name = "graspable_difficulty")
    public String graspableDifficulty; // "beginner", "intermediate", "advanced"

    @Column(name = "graspable_hints", columnDefinition = "TEXT")
    public String graspableHints; // JSON array of hint strings
}
