package de.vptr.aimathtutor.dto;

/**
 * Bundles current and previous-period counts for 6 KPI metrics, with computed percentage-change accessors. Used by
 * {@code AdminDashboardView} to display trend arrows.
 */
public class DashboardTrendDto {

    public final long totalSessions;
    public final long prevTotalSessions;
    public final long completedSessions;
    public final long prevCompletedSessions;
    public final long activeStudents;
    public final long prevActiveStudents;
    public final long todaySessions;
    public final long prevTodaySessions;
    public final long totalUsers;
    public final long prevTotalUsers;
    public final long publishedExercises;
    public final long prevPublishedExercises;

    /**
     * Creates a trend DTO with current and prior-period counts for all 6 KPI metrics.
     */
    public DashboardTrendDto(final long totalSessions, final long prevTotalSessions, final long completedSessions,
            final long prevCompletedSessions, final long activeStudents, final long prevActiveStudents,
            final long todaySessions, final long prevTodaySessions, final long totalUsers, final long prevTotalUsers,
            final long publishedExercises, final long prevPublishedExercises) {
        this.totalSessions = totalSessions;
        this.prevTotalSessions = prevTotalSessions;
        this.completedSessions = completedSessions;
        this.prevCompletedSessions = prevCompletedSessions;
        this.activeStudents = activeStudents;
        this.prevActiveStudents = prevActiveStudents;
        this.todaySessions = todaySessions;
        this.prevTodaySessions = prevTodaySessions;
        this.totalUsers = totalUsers;
        this.prevTotalUsers = prevTotalUsers;
        this.publishedExercises = publishedExercises;
        this.prevPublishedExercises = prevPublishedExercises;
    }

    /**
     * Returns the percentage change for total sessions.
     *
     * @return percentage change (positive = increase, negative = decrease)
     */
    public double totalSessionsChange() {
        return computeChange(this.totalSessions, this.prevTotalSessions);
    }

    /**
     * Returns the percentage change for completed sessions.
     *
     * @return percentage change
     */
    public double completedSessionsChange() {
        return computeChange(this.completedSessions, this.prevCompletedSessions);
    }

    /**
     * Returns the percentage change for active students.
     *
     * @return percentage change
     */
    public double activeStudentsChange() {
        return computeChange(this.activeStudents, this.prevActiveStudents);
    }

    /**
     * Returns the percentage change for today's sessions.
     *
     * @return percentage change
     */
    public double todaySessionsChange() {
        return computeChange(this.todaySessions, this.prevTodaySessions);
    }

    /**
     * Returns the percentage change for total users.
     *
     * @return percentage change
     */
    public double totalUsersChange() {
        return computeChange(this.totalUsers, this.prevTotalUsers);
    }

    /**
     * Returns the percentage change for published exercises.
     *
     * @return percentage change
     */
    public double publishedExercisesChange() {
        return computeChange(this.publishedExercises, this.prevPublishedExercises);
    }

    private static double computeChange(final long current, final long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((double) current - previous) / previous * 100.0;
    }
}
