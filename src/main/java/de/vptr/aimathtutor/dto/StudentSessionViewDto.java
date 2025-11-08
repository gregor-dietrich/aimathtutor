package de.vptr.aimathtutor.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.StudentSessionEntity;

/**
 * DTO for displaying student session information in admin views.
 * Contains computed fields and safe data for client display.
 */
public class StudentSessionViewDto {

    public Long id;
    public String sessionId;
    public Long userId;
    public String username;
    public Long exerciseId;
    public String exerciseTitle;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public Boolean completed;
    public Integer actionsCount;
    public Integer correctActions;
    public Integer hintsUsed;
    public String finalExpression;

    // Computed fields
    public Long durationSeconds;
    public Double successRate;

    public StudentSessionViewDto() {
    }

    /**
     * Constructs a StudentSessionViewDto from a StudentSessionEntity.
     */
    public StudentSessionViewDto(final StudentSessionEntity entity) {
        if (entity != null) {
            this.id = entity.id;
            this.sessionId = entity.sessionId;
            this.startTime = entity.startTime;
            this.endTime = entity.endTime;
            this.completed = entity.completed;
            this.actionsCount = entity.actionsCount;
            this.correctActions = entity.correctActions;
            this.hintsUsed = entity.hintsUsed;
            this.finalExpression = entity.finalExpression;

            // Handle user information safely
            if (entity.user != null) {
                this.userId = entity.user.id;
                this.username = entity.user.username;
            }

            // Handle exercise information safely
            if (entity.exercise != null) {
                this.exerciseId = entity.exercise.id;
                this.exerciseTitle = entity.exercise.title;
            }

            // Compute duration only for completed sessions
            if (entity.startTime != null && entity.endTime != null && entity.completed) {
                this.durationSeconds = Duration.between(entity.startTime, entity.endTime).getSeconds();
            } else {
                this.durationSeconds = null;
            }

            // Compute success rate
            if (entity.actionsCount > 0) {
                this.successRate = (double) entity.correctActions / entity.actionsCount;
            } else {
                this.successRate = 0.0;
            }
        }
    }

    /**
     * Get formatted duration as HH:mm:ss, or null for incomplete sessions
     */
    public String getFormattedDuration() {
        // Don't show duration for incomplete sessions
        if (this.durationSeconds == null || !this.completed) {
            return null;
        }

        if (this.durationSeconds == 0) {
            return "0s";
        }

        final long totalSeconds = this.durationSeconds;
        final long hours = totalSeconds / 3600;
        final long minutes = (totalSeconds % 3600) / 60;
        final long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%02d:%02d", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Get success rate as percentage string
     */
    public String getSuccessRatePercentage() {
        if (this.successRate == null) {
            return "0%";
        }
        return String.format("%.1f%%", this.successRate * 100);
    }
}
