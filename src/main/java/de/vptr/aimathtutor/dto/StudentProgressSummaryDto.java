package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

import jakarta.annotation.Nullable;

/**
 * DTO for displaying a student's overall progress summary. Used in admin views to show aggregate statistics per
 * student.
 */
public class StudentProgressSummaryDto {

    @Nullable
    public String userPublicId;
    @Nullable
    public String username;
    @Nullable
    public Integer totalSessions;
    @Nullable
    public Integer completedSessions;
    @Nullable
    public Integer totalProblems;
    @Nullable
    public Integer completedProblems;
    @Nullable
    public Integer hintsUsed;
    @Nullable
    public Double averageActionsPerProblem;
    @Nullable
    public Double successRate;
    @Nullable
    public LocalDateTime lastActivity;

    public StudentProgressSummaryDto() {
    }

    /**
     * Constructs a StudentProgressSummaryDto with the specified parameters.
     */
    public StudentProgressSummaryDto(@Nullable final String userPublicId, @Nullable final String username,
            @Nullable final Integer totalSessions, @Nullable final Integer completedSessions,
            @Nullable final Integer totalProblems, @Nullable final Integer completedProblems,
            @Nullable final Integer hintsUsed, @Nullable final Double averageActionsPerProblem,
            @Nullable final Double successRate, @Nullable final LocalDateTime lastActivity) {
        this.userPublicId = userPublicId;
        this.username = username;
        this.totalSessions = totalSessions;
        this.completedSessions = completedSessions;
        this.totalProblems = totalProblems;
        this.completedProblems = completedProblems;
        this.hintsUsed = hintsUsed;
        this.averageActionsPerProblem = averageActionsPerProblem;
        this.successRate = successRate;
        this.lastActivity = lastActivity;
    }

    /**
     * Get completion rate as percentage string
     */
    public String getCompletionRatePercentage() {
        if (this.totalSessions == null || this.totalSessions == 0) {
            return "0%";
        }
        final Integer completed = this.completedSessions;
        final Integer total = this.totalSessions;
        if (completed == null) {
            return "0%";
        }
        final double rate = (double) completed / total;
        return String.format("%.1f%%", rate * 100);
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

    /**
     * Get average actions as formatted string
     */
    public String getFormattedAverageActions() {
        if (this.averageActionsPerProblem == null) {
            return "0";
        }
        return String.format("%.1f", this.averageActionsPerProblem);
    }
}
