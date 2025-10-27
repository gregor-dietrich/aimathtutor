package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity to log AI interactions and feedback.
 * Useful for analytics, improving AI responses, and debugging.
 */
@Entity
@Table(name = "ai_interactions")
@NamedQueries({
        @NamedQuery(name = "AIInteraction.findAll", query = "FROM AIInteractionEntity ORDER BY id DESC"),
        @NamedQuery(name = "AIInteraction.findBySessionId", query = "FROM AIInteractionEntity WHERE sessionId = :s"),
        @NamedQuery(name = "AIInteraction.findByUserId", query = "FROM AIInteractionEntity WHERE user.id = :u"),
        @NamedQuery(name = "AIInteraction.findByExerciseId", query = "FROM AIInteractionEntity WHERE exercise.id = :e")
})
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

    @Column(name = "student_message", columnDefinition = "TEXT")
    public String studentMessage; // Optional: explicit student question/message

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

    @Column(name = "conversation_context", columnDefinition = "TEXT")
    public String conversationContext; // JSON string of context sent with AI request

    @SuppressFBWarnings(value = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE", justification = "Panache entity field intentionally public for ORM mapping")
    public LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
