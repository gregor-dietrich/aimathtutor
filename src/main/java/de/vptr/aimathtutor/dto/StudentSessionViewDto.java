package de.vptr.aimathtutor.dto;

import java.time.Duration;
import java.time.LocalDateTime;

import de.vptr.aimathtutor.entity.StudentSessionEntity;
import jakarta.annotation.Nullable;

/**
 * DTO for displaying student session information in admin views. Contains computed fields and safe data for client
 * display.
 */
public class StudentSessionViewDto {

    @Nullable
    public String publicId;
    @Nullable
    public String sessionId;
    @Nullable
    public String userPublicId;
    @Nullable
    public String username;
    @Nullable
    public String exercisePublicId;
    @Nullable
    public String exerciseTitle;
    @Nullable
    public LocalDateTime startTime;
    @Nullable
    public LocalDateTime endTime;
    @Nullable
    public Boolean completed;
    @Nullable
    public Integer actionsCount;
    @Nullable
    public Integer correctActions;
    @Nullable
    public Integer hintsUsed;
    @Nullable
    public String finalExpression;

    // Computed fields
    @Nullable
    public Long durationSeconds;
    @Nullable
    public Double successRate;

    public StudentSessionViewDto() {
    }

    /**
     * Constructs a StudentSessionViewDto from a StudentSessionEntity.
     */
    public StudentSessionViewDto(final StudentSessionEntity entity) {
        if (entity != null) {
            this.publicId = entity.publicId;
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
                this.userPublicId = entity.user.publicId;
                this.username = entity.user.username;
            }

            // Handle exercise information safely
            if (entity.exercise != null) {
                this.exercisePublicId = entity.exercise.publicId;
                this.exerciseTitle = entity.exercise.title;
            }

            // Compute duration only for completed sessions
            if (entity.startTime != null && entity.endTime != null && Boolean.TRUE.equals(entity.completed)) {
                this.durationSeconds = Duration.between(entity.startTime, entity.endTime).toSeconds();
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
    @Nullable
    public String getFormattedDuration() {
        // Don't show duration for incomplete sessions
        if (this.durationSeconds == null || !Boolean.TRUE.equals(this.completed)) {
            return null;
        }

        if (this.durationSeconds == 0) {
            return "0s";
        }

        final long totalSeconds = this.durationSeconds;
        final long hours = totalSeconds / 3600;
        final long minutes = totalSeconds % 3600 / 60;
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
        final double rate = this.successRate;
        return String.format("%.1f%%", rate * 100);
    }
}
