package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing math exercises in the system.
 */
@Entity
@Table(name = "exercises", indexes = {
        @Index(name = "idx_exercise_lesson_published", columnList = "lesson_id, published"),
        @Index(name = "idx_exercise_published_id", columnList = "published, id DESC"),
        @Index(name = "idx_exercise_user_id", columnList = "user_id, id DESC")
})
@NamedQueries({
        @NamedQuery(name = "Exercise.findAllOrdered", query = "FROM ExerciseEntity ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findPublished", query = "FROM ExerciseEntity WHERE published = true ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findByUserId", query = "FROM ExerciseEntity WHERE user.id = :u ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findByLessonId", query = "FROM ExerciseEntity WHERE lesson.id = :l ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findGraspableEnabled", query = "FROM ExerciseEntity WHERE graspableEnabled = true AND published = true ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findGraspableByLesson", query = "FROM ExerciseEntity WHERE graspableEnabled = true AND published = true AND lesson.id = :l ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.searchByTerm", query = "FROM ExerciseEntity WHERE LOWER(title) LIKE :s OR LOWER(content) LIKE :s ORDER BY id DESC"),
        @NamedQuery(name = "Exercise.findByDateRange", query = "FROM ExerciseEntity WHERE created BETWEEN :s AND :e ORDER BY created DESC"),
})
public class ExerciseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @NotBlank
    @Column(nullable = false)
    public String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    public LessonEntity lesson;

    @Column(nullable = false)
    public boolean published = false;

    @Column(nullable = false)
    public boolean commentable = false;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    public List<CommentEntity> comments;

    // Graspable Math Configuration
    @Column(name = "graspable_enabled")
    public boolean graspableEnabled = false;

    @Column(name = "graspable_initial_expression", columnDefinition = "TEXT")
    public String graspableInitialExpression;

    @Column(name = "graspable_target_expression", columnDefinition = "TEXT")
    public String graspableTargetExpression;

    @Column(name = "graspable_difficulty")
    @Enumerated(EnumType.STRING)
    public DifficultyLevel graspableDifficulty;

    @Column(name = "graspable_hints", columnDefinition = "TEXT")
    public String graspableHints; // JSON array of hint strings
}
