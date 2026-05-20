package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Formula;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing math exercises in the system.
 */
@Entity
@Table(name = "exercises",
        indexes = { @Index(name = "idx_exercise_lesson_published", columnList = "lesson_id, published"),
                @Index(name = "idx_exercise_public_id", columnList = "public_id"),
                @Index(name = "idx_exercise_user_id", columnList = "user_id, created DESC") })
@NamedQueries({
        @NamedQuery(name = "Exercise.findAllOrdered", query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user "
                + "LEFT JOIN FETCH e.lesson ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.findByPublicId", query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user "
                + "LEFT JOIN FETCH e.lesson WHERE e.publicId = :p"),
        @NamedQuery(name = "Exercise.findPublished", query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user "
                + "LEFT JOIN FETCH e.lesson WHERE e.published = true ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.findByUserId", query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user "
                + "LEFT JOIN FETCH e.lesson WHERE e.user.id = :u ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.findByLessonId", query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user "
                + "LEFT JOIN FETCH e.lesson WHERE e.lesson.id = :l ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.findGraspableEnabled",
                query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user LEFT JOIN FETCH e.lesson "
                        + "WHERE e.graspableEnabled = true AND e.published = true ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.findGraspableByLesson",
                query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user LEFT JOIN FETCH e.lesson "
                        + "WHERE e.graspableEnabled = true AND e.published = true AND e.lesson.id = :l "
                        + "ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.searchByTerm",
                query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user LEFT JOIN FETCH e.lesson "
                        + "WHERE LOWER(e.title) LIKE :s OR LOWER(e.content) LIKE :s ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.findByDateRange", query = "FROM ExerciseEntity e LEFT JOIN FETCH e.user "
                + "LEFT JOIN FETCH e.lesson WHERE e.created BETWEEN :s AND :e ORDER BY e.created DESC"),
        @NamedQuery(name = "Exercise.countPublished",
                query = "SELECT COUNT(e) FROM ExerciseEntity e WHERE e.published = true") })
public class ExerciseEntity extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    @Nullable
    public String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    @NotBlank
    @Nullable
    public String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @Nullable
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    @Nullable
    public LessonEntity lesson;

    @Column(nullable = false)
    public boolean published = false;

    @Column(nullable = false)
    public boolean commentable = false;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "exercise", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    @Nullable
    public List<CommentEntity> comments;

    /**
     * Read-only count of comments on this exercise, computed by SQL on every fetch. Avoids loading the entire comments
     * collection when DTOs need only the size.
     */
    @Formula("(SELECT COUNT(*) FROM comments c WHERE c.exercise_id = id)")
    @Nullable
    public Long commentsCount;

    // Graspable Math Configuration
    @Column(name = "graspable_enabled")
    public boolean graspableEnabled = false;

    @Column(name = "graspable_initial_expression", columnDefinition = "TEXT")
    @Nullable
    public String graspableInitialExpression;

    @Column(name = "graspable_target_expression", columnDefinition = "TEXT")
    @Nullable
    public String graspableTargetExpression;

    @Column(name = "graspable_difficulty")
    @Enumerated(EnumType.STRING)
    @Nullable
    public DifficultyLevel graspableDifficulty;

    @Column(name = "graspable_hints", columnDefinition = "TEXT")
    @Nullable
    public String graspableHints; // JSON array of hint strings
}
