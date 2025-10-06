package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity to log AI interactions and feedback.
 * Useful for analytics, improving AI responses, and debugging.
 */
@Entity
@Table(name = "ai_interactions")
public class AIInteractionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "session_id")
    public String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    public ExerciseEntity exercise;

    @NotBlank
    @Column(name = "event_type")
    public String eventType; // Type of math action

    @Column(name = "expression_before", columnDefinition = "TEXT")
    public String expressionBefore;

    @Column(name = "expression_after", columnDefinition = "TEXT")
    public String expressionAfter;

    @NotBlank
    @Column(name = "feedback_type")
    public String feedbackType; // POSITIVE, CORRECTIVE, HINT, etc.

    @Column(name = "feedback_message", columnDefinition = "TEXT")
    public String feedbackMessage;

    @Column(name = "confidence_score")
    public Double confidenceScore;

    @Column(name = "action_correct")
    public Boolean actionCorrect;

    public LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
