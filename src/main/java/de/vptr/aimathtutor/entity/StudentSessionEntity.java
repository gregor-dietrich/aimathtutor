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
