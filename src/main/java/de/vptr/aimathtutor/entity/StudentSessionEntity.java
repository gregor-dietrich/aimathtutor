package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity to track student sessions in Graspable Math workspace. Each session represents a student working on a specific
 * exercise.
 */
@Entity
@Table(name = "student_sessions",
        indexes = { @Index(name = "idx_session_user_start", columnList = "user_id, start_time"),
                @Index(name = "idx_session_exercise_start", columnList = "exercise_id, start_time"),
                @Index(name = "idx_session_completed_start", columnList = "completed, start_time"),
                @Index(name = "idx_session_start_time", columnList = "start_time") })
@NamedQueries({
        @NamedQuery(name = "StudentSession.findBySessionId", query = "FROM StudentSessionEntity WHERE sessionId = :s"),
        @NamedQuery(name = "StudentSession.findByPublicId", query = "FROM StudentSessionEntity WHERE publicId = :p"),
        @NamedQuery(name = "StudentSession.findByUserId", query = "FROM StudentSessionEntity WHERE user.id = :u"),
        @NamedQuery(name = "StudentSession.findByExerciseId",
                query = "FROM StudentSessionEntity WHERE exercise.id = :e"),
        @NamedQuery(name = "StudentSession.findAllOrdered",
                query = "FROM StudentSessionEntity ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findByUserAndExercise",
                query = "FROM StudentSessionEntity " + "WHERE user.id = :u and exercise.id = :e "
                        + "ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findByUserAndDateRange",
                query = "FROM StudentSessionEntity " + "WHERE user.id = :u and startTime >= :s and startTime <= :e "
                        + "ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findByExerciseAndDateRange",
                query = "FROM StudentSessionEntity "
                        + "WHERE exercise.id = :e and startTime >= :s and startTime <= :en "
                        + "ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findByCompletedAndDateRange",
                query = "FROM StudentSessionEntity " + "WHERE completed = :c and startTime >= :s and startTime <= :e "
                        + "ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeAfter",
                query = "FROM StudentSessionEntity WHERE startTime >= :t ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeBetween",
                query = "FROM StudentSessionEntity " + "WHERE startTime >= :s and startTime <= :e "
                        + "ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.countByCompleted",
                query = "SELECT COUNT(s) FROM StudentSessionEntity s WHERE completed = :c"),
        @NamedQuery(name = "StudentSession.countAll", query = "SELECT COUNT(s) FROM StudentSessionEntity s"),
        @NamedQuery(name = "StudentSession.searchByUserOrExercise",
                query = "FROM StudentSessionEntity "
                        + "WHERE lower(user.username) like :p or lower(exercise.title) like :p "
                        + "ORDER BY startTime DESC, publicId DESC"),
        @NamedQuery(name = "StudentSession.findAllWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByUserIdWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.user.id = :u ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByExerciseIdWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.exercise.id = :e ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByUserAndExerciseWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.user.id = :u and s.exercise.id = :e "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByUserAndDateRangeWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.user.id = :u and s.startTime >= :s and s.startTime <= :e "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByExerciseAndDateRangeWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.exercise.id = :e and s.startTime >= :s and s.startTime <= :en "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByCompletedAndDateRangeWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.completed = :c and s.startTime >= :s and s.startTime <= :e "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeAfterWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.startTime >= :t ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeBeforeWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.startTime <= :e ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByStartTimeBetweenWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.startTime >= :s and s.startTime <= :e "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.findByUserIdInWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE s.user.id IN :ids ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.searchByUserOrExerciseWithRelations",
                query = "SELECT s FROM StudentSessionEntity s " + "LEFT JOIN FETCH s.user LEFT JOIN FETCH s.exercise "
                        + "WHERE lower(s.user.username) like :p or lower(s.exercise.title) like :p "
                        + "ORDER BY s.startTime DESC, s.publicId DESC"),
        @NamedQuery(name = "StudentSession.countActiveStudents",
                query = "SELECT COUNT(DISTINCT s.user.id) FROM StudentSessionEntity s WHERE s.startTime >= :t"),
        @NamedQuery(name = "StudentSession.countActiveStudentsBetween",
                query = "SELECT COUNT(DISTINCT s.user.id) FROM StudentSessionEntity s "
                        + "WHERE s.startTime >= :s AND s.startTime < :e"),
        @NamedQuery(name = "StudentSession.countByStartTimeBetween",
                query = "SELECT COUNT(s) FROM StudentSessionEntity s WHERE s.startTime >= :s and s.startTime <= :e"),
        @NamedQuery(name = "StudentSession.countByStartTimeRangeHalfOpen",
                query = "SELECT COUNT(s) FROM StudentSessionEntity s WHERE s.startTime >= :s and s.startTime < :e"),
        @NamedQuery(name = "StudentSession.findProblemCategoryStats",
                query = "SELECT s.exercise.title, COUNT(s) FROM StudentSessionEntity s "
                        + "WHERE s.completed = true GROUP BY s.exercise.title"),
        @NamedQuery(name = "StudentSession.findCompletionRateBuckets",
                query = "SELECT CASE "
                        + "  WHEN s.actionsCount = 0 OR s.correctActions = 0 THEN '0%' "
                        + "  WHEN s.correctActions = s.actionsCount THEN '100%' "
                        + "  WHEN (s.correctActions * 1.0 / s.actionsCount) <= 0.25 THEN '1-25%' "
                        + "  WHEN (s.correctActions * 1.0 / s.actionsCount) <= 0.50 THEN '26-50%' "
                        + "  WHEN (s.correctActions * 1.0 / s.actionsCount) <= 0.75 THEN '51-75%' "
                        + "  ELSE '76-99%' END, COUNT(s) "
                        + "FROM StudentSessionEntity s GROUP BY CASE "
                        + "  WHEN s.actionsCount = 0 OR s.correctActions = 0 THEN '0%' "
                        + "  WHEN s.correctActions = s.actionsCount THEN '100%' "
                        + "  WHEN (s.correctActions * 1.0 / s.actionsCount) <= 0.25 THEN '1-25%' "
                        + "  WHEN (s.correctActions * 1.0 / s.actionsCount) <= 0.50 THEN '26-50%' "
                        + "  WHEN (s.correctActions * 1.0 / s.actionsCount) <= 0.75 THEN '51-75%' "
                        + "  ELSE '76-99%' END"),
        @NamedQuery(name = "StudentSession.findHintUsageBuckets",
                query = "SELECT CASE "
                        + "  WHEN s.hintsUsed = 0 THEN '0 hints' "
                        + "  WHEN s.hintsUsed <= 3 THEN '1-3 hints' "
                        + "  WHEN s.hintsUsed <= 7 THEN '4-7 hints' "
                        + "  ELSE '8+ hints' END, COUNT(s) "
                        + "FROM StudentSessionEntity s GROUP BY CASE "
                        + "  WHEN s.hintsUsed = 0 THEN '0 hints' "
                        + "  WHEN s.hintsUsed <= 3 THEN '1-3 hints' "
                        + "  WHEN s.hintsUsed <= 7 THEN '4-7 hints' "
                        + "  ELSE '8+ hints' END"),
        @NamedQuery(name = "StudentSession.findTopUsersByCompletion",
                query = "SELECT s.user.id, COUNT(s) FROM StudentSessionEntity s "
                        + "WHERE s.completed = true AND s.user IS NOT NULL "
                        + "GROUP BY s.user.id ORDER BY COUNT(s) DESC") })
public class StudentSessionEntity extends BaseEntity {

    @Nullable
    @NotBlank
    @Column(name = "session_id", unique = true, nullable = false)
    public String sessionId;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @Nullable
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    public UserEntity user;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @Nullable
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_id")
    public ExerciseEntity exercise;

    @Nullable
    @Column(name = "start_time")
    public LocalDateTime startTime;

    @Nullable
    @Column(name = "end_time")
    public LocalDateTime endTime;

    @Column(name = "completed", nullable = false)
    public boolean completed = false;

    @Column(name = "actions_count", nullable = false)
    public int actionsCount = 0;

    @Column(name = "correct_actions", nullable = false)
    public int correctActions = 0;

    @Column(name = "hints_used", nullable = false)
    public int hintsUsed = 0;

    @Nullable
    @Column(name = "final_expression", columnDefinition = "TEXT")
    public String finalExpression;

    @Nullable
    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Nullable
    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;
}
