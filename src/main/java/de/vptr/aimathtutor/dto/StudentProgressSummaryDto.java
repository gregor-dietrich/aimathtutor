package de.vptr.aimathtutor.dto;

import java.time.LocalDateTime;

/**
 * DTO for displaying a student's overall progress summary.
 * Used in admin views to show aggregate statistics per student.
 */
public class StudentProgressSummaryDto {

    public Long userId;
    public String username;
    public Integer totalSessions;
    public Integer completedSessions;
    public Integer totalProblems;
    public Integer completedProblems;
    public Integer hintsUsed;
    public Double averageActionsPerProblem;
    public Double successRate;
    public LocalDateTime lastActivity;

    public StudentProgressSummaryDto() {
    }

    public StudentProgressSummaryDto(
            final Long userId,
            final String username,
            final Integer totalSessions,
            final Integer completedSessions,
            final Integer totalProblems,
            final Integer completedProblems,
            final Integer hintsUsed,
            final Double averageActionsPerProblem,
            final Double successRate,
            final LocalDateTime lastActivity) {
        this.userId = userId;
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
        final double rate = (double) this.completedSessions / this.totalSessions;
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
