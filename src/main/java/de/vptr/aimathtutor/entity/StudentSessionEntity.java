package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity to track student sessions in Graspable Math workspace.
 * Each session represents a student working on a specific exercise.
 */
@Entity
@Table(name = "student_sessions")
@NamedQueries({
        @NamedQuery(name = "StudentSession.findBySessionId", query = "FROM StudentSessionEntity WHERE sessionId = :s"),
        @NamedQuery(name = "StudentSession.findByUserId", query = "FROM StudentSessionEntity WHERE user.id = :u"),
        @NamedQuery(name = "StudentSession.findByExerciseId", query = "FROM StudentSessionEntity WHERE exercise.id = :e"),
        @NamedQuery(name = "StudentSession.findAllOrdered", query = "FROM StudentSessionEntity ORDER BY id DESC"),
        @NamedQuery(name = "StudentSession.findByUserAndExercise", query = "FROM StudentSessionEntity WHERE user.id = :u and exercise.id = :e ORDER BY id DESC"),
        @NamedQuery(name = "StudentSession.findByUserAndDateRange", query = "FROM StudentSessionEntity WHERE user.id = :u and startTime >= :s and startTime <= :e ORDER BY startTime DESC"),
        @NamedQuery(name = "StudentSession.findByExerciseAndDateRange", query = "FROM StudentSessionEntity WHERE exercise.id = :e and startTime >= :s and startTime <= :en ORDER BY startTime DESC"),
        @NamedQuery(name = "StudentSession.findByCompletedAndDateRange", query = "FROM StudentSessionEntity WHERE completed = :c and startTime >= :s and startTime <= :e ORDER BY startTime DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeAfter", query = "FROM StudentSessionEntity WHERE startTime >= :t ORDER BY startTime DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeBetween", query = "FROM StudentSessionEntity WHERE startTime >= :s and startTime <= :e ORDER BY startTime DESC"),
        @NamedQuery(name = "StudentSession.countByCompleted", query = "SELECT COUNT(s) FROM StudentSessionEntity s WHERE completed = :c"),
        @NamedQuery(name = "StudentSession.countAll", query = "SELECT COUNT(s) FROM StudentSessionEntity s"),
        @NamedQuery(name = "StudentSession.searchByUserOrExercise", query = "FROM StudentSessionEntity WHERE lower(user.username) like :p or lower(exercise.title) like :p ORDER BY startTime DESC")
})
public class StudentSessionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(name = "session_id", unique = true)
    public String sessionId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exercise_id")
    public ExerciseEntity exercise;

    @Column(name = "start_time")
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Panache entity fields are public by design for ORM mapping")
    public LocalDateTime startTime;

    @Column(name = "end_time")
    public LocalDateTime endTime;

    @Column(name = "completed")
    public Boolean completed = false;

    @Column(name = "actions_count")
    public Integer actionsCount = 0;

    @Column(name = "correct_actions")
    public Integer correctActions = 0;

    @Column(name = "hints_used")
    public Integer hintsUsed = 0;

    @Column(name = "final_expression", columnDefinition = "TEXT")
    public String finalExpression;

    public static StudentSessionEntity findBySessionId(final String sessionId) {
        return find("sessionId", sessionId).firstResult();
    }

    @PrePersist
    public void prePersist() {
        if (this.startTime == null) {
            this.startTime = LocalDateTime.now();
        }
    }
}
