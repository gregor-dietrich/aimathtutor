package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import de.vptr.aimathtutor.util.UlidUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity to log AI interactions and feedback.
 * Useful for analytics, improving AI responses, and debugging.
 */
@Entity
@Table(name = "ai_interactions", indexes = {
        @Index(name = "idx_ai_session", columnList = "session_id"),
        @Index(name = "idx_ai_user", columnList = "user_id"),
        @Index(name = "idx_ai_exercise", columnList = "exercise_id")
})
@NamedQueries({
        @NamedQuery(name = "AiInteraction.findAll", query = "FROM AiInteractionEntity ORDER BY created DESC, id DESC"),
        @NamedQuery(name = "AiInteraction.findByPublicId", query = "FROM AiInteractionEntity WHERE publicId = :p"),
        @NamedQuery(name = "AiInteraction.findBySessionId", query = "FROM AiInteractionEntity WHERE sessionId = :s"),
        @NamedQuery(name = "AiInteraction.findByUserId", query = "FROM AiInteractionEntity WHERE user.id = :u"),
        @NamedQuery(name = "AiInteraction.findByExerciseId", query = "FROM AiInteractionEntity WHERE exercise.id = :e")
})
public class AiInteractionEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    public String publicId;

    /**
     * Generates a ULID-based public identifier for this entity if not already set.
     */
    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null || this.publicId.isBlank()) {
            this.publicId = UlidUtil.generate();
            return;
        }
        UlidUtil.requireValid(this.publicId);
    }

    @Column(name = "session_id")
    public String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    public ExerciseEntity exercise;

    @NotBlank
    @Column(name = "event_type", nullable = false)
    public String eventType; // Type of math action

    @Column(name = "student_message", columnDefinition = "TEXT")
    public String studentMessage; // Optional: explicit student question/message

    @Column(name = "expression_before", columnDefinition = "TEXT")
    public String expressionBefore;

    @Column(name = "expression_after", columnDefinition = "TEXT")
    public String expressionAfter;

    @NotBlank
    @Column(name = "feedback_type", nullable = false)
    public String feedbackType; // POSITIVE, CORRECTIVE, HINT, etc.

    @Column(name = "feedback_message", columnDefinition = "TEXT")
    public String feedbackMessage;

    @Column(name = "confidence_score")
    public Double confidenceScore;

    @Column(name = "action_correct")
    public boolean actionCorrect;

    @Column(name = "conversation_context", columnDefinition = "TEXT")
    public String conversationContext; // JSON string of context sent with AI request

    @Generated(event = EventType.INSERT)
    @Column(name = "created")
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;
}
