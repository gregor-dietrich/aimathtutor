package de.vptr.aimathtutor.view.admin;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.component.dashboard.ChartCard;
import de.vptr.aimathtutor.component.dashboard.ChartUtil;
import de.vptr.aimathtutor.component.dashboard.DashboardKpiCard;
import de.vptr.aimathtutor.component.dashboard.RecentActivityFeed;
import de.vptr.aimathtutor.component.dashboard.TopStudentsList;
import de.vptr.aimathtutor.dto.DashboardTrendDto;
import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import de.vptr.aimathtutor.service.AnalyticsService;
import de.vptr.aimathtutor.util.AsyncDataLoader;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Premium admin dashboard with KPI cards in a responsive CSS grid, SVG line/bar charts, donut chart for completion
 * rates, recent activity feed, and top performers. Glass-morphism design language with gradient accents.
 */
@Route(value = "admin/dashboard", layout = AdminMainLayout.class)
@PageTitle("Admin Dashboard - AI Math Tutor")
public class AdminDashboardView extends AbstractAdminView {

    @Inject
    private transient AnalyticsService analyticsService;

    @Nullable
    private transient DashboardKpiCard totalSessionsCard;
    @Nullable
    private transient DashboardKpiCard completedSessionsCard;
    @Nullable
    private transient DashboardKpiCard activeStudentsCard;
    @Nullable
    private transient DashboardKpiCard todaySessionsCard;
    @Nullable
    private transient DashboardKpiCard totalUsersCard;
    @Nullable
    private transient DashboardKpiCard publishedExercisesCard;
    @Nullable
    private transient DashboardKpiCard avgSuccessRateCard;

    @Nullable
    private transient ChartCard sessionsChart;
    @Nullable
    private transient ChartCard topExercisesChart;
    @Nullable
    private transient ChartCard completionRateChart;
    @Nullable
    private transient ChartCard hintsChart;

    @Nullable
    private transient VerticalLayout activityFeedContainer;
    @Nullable
    private transient VerticalLayout topStudentsContainer;

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }
        this.buildUi();
        this.loadDashboardData();
    }

    private void buildUi() {
        this.removeAll();
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);

        final var title = new H2("Dashboard Overview");
        title.getStyle().set("margin-top", "0").set("margin-bottom", "4px").set("font-size", "22px")
                .set("font-weight", "700").set("color", "var(--lumo-header-text-color)")
                .set("letter-spacing", "-0.3px");
        this.add(title);

        final var subtitle = new Span("Real-time analytics at a glance");
        subtitle.getStyle().set("font-size", "13px").set("color", "var(--lumo-secondary-text-color)")
                .set("margin-bottom", "8px").set("display", "block");
        this.add(subtitle);

        // Row 1: Active Students, Total Users, Published Exercises
        this.activeStudentsCard = new DashboardKpiCard("Active Students (7d)", "—");
        this.totalUsersCard = new DashboardKpiCard("Total Users", "—");
        this.publishedExercisesCard = new DashboardKpiCard("Published Exercises", "—");

        final var kpiRow1 = new Div(this.activeStudentsCard, this.totalUsersCard, this.publishedExercisesCard);
        kpiRow1.setWidthFull();
        kpiRow1.getStyle().set("display", "grid").set("grid-template-columns", "repeat(3, 1fr)").set("gap", "14px")
                .set("margin-bottom", "6px");
        this.add(kpiRow1);

        // Row 2: Total Sessions, Completed Sessions, Today's Sessions, Avg Success Rate
        this.totalSessionsCard = new DashboardKpiCard("Total Sessions", "—");
        this.completedSessionsCard = new DashboardKpiCard("Completed Sessions", "—");
        this.todaySessionsCard = new DashboardKpiCard("Today's Sessions", "—");
        this.avgSuccessRateCard = new DashboardKpiCard("Avg Success Rate", "—");

        final var kpiRow2 = new Div(this.totalSessionsCard, this.completedSessionsCard, this.todaySessionsCard,
                this.avgSuccessRateCard);
        kpiRow2.setWidthFull();
        kpiRow2.getStyle().set("display", "grid").set("grid-template-columns", "repeat(4, 1fr)").set("gap", "14px")
                .set("margin-bottom", "6px");
        this.add(kpiRow2);

        // Row 3: Sessions per Day (wider) + Completion Rate Distribution (narrower)
        this.sessionsChart = new ChartCard("Sessions per Day (30 days)");
        this.sessionsChart.getStyle().set("min-width", "50%").set("max-width", "75%").set("flex-grow", "3");
        this.completionRateChart = new ChartCard("Completion Rate Distribution");
        this.completionRateChart.getStyle().set("min-width", "25%").set("max-width", "50%").set("flex-grow", "1");
        final var chartRow1 = new HorizontalLayout(this.sessionsChart, this.completionRateChart);
        chartRow1.setWidthFull();
        chartRow1.setSpacing(true);
        this.add(chartRow1);

        // Row 4: Top Exercises by Completion + Hints Usage Distribution
        this.topExercisesChart = new ChartCard("Top Categories by Completion");
        this.hintsChart = new ChartCard("Hints Usage Distribution");
        final var chartRow2 = new HorizontalLayout(this.topExercisesChart, this.hintsChart);
        chartRow2.setWidthFull();
        chartRow2.setSpacing(true);
        chartRow2.setFlexGrow(1, this.topExercisesChart, this.hintsChart);
        this.add(chartRow2);

        // Row 4: Activity Feed + Top Students
        this.activityFeedContainer = new VerticalLayout();
        this.activityFeedContainer.setWidthFull();

        this.topStudentsContainer = new VerticalLayout();
        this.topStudentsContainer.setWidthFull();

        final var bottomRow = new HorizontalLayout(this.activityFeedContainer, this.topStudentsContainer);
        bottomRow.setWidthFull();
        bottomRow.setSpacing(true);
        bottomRow.setFlexGrow(1, this.activityFeedContainer, this.topStudentsContainer);
        this.add(bottomRow);
    }

    private void loadDashboardData() {
        AsyncDataLoader.load(() -> {
            final var totalSessions = this.analyticsService.getTotalSessionsCount();
            final var completedSessions = this.analyticsService.getCompletedSessionsCount();
            final var activeStudents = this.analyticsService.getActiveStudentsCount();
            final var todaySessions = this.analyticsService.getTodaySessionsCount();
            final var totalUsers = this.analyticsService.getUserCount();
            final var publishedExercises = this.analyticsService.getPublishedExerciseCount();

            final var summaries = this.analyticsService.getAllUsersProgressSummary();
            final var avgSuccessRate = summaries.stream().filter(s -> s.successRate != null)
                    .mapToDouble(s -> s.successRate).average().orElse(0.0);

            final var dailyCounts = this.analyticsService.getDailySessionCounts(30);
            final var topExercises = this.analyticsService.getProblemCategoryStats();
            final var completionHistogram = this.analyticsService.getCompletionRateHistogram();
            final var hintBuckets = this.analyticsService.getHintUsageBuckets();
            final var recentSessions = this.analyticsService.getRecentSessions(10);
            final var topStudents = this.analyticsService.getTopStudentsByCompletion(5);
            final var trends = this.analyticsService.getTrendData();

            return new DashboardData(totalSessions, completedSessions, activeStudents, todaySessions, totalUsers,
                    publishedExercises, avgSuccessRate, dailyCounts, topExercises, completionHistogram, hintBuckets,
                    recentSessions, topStudents, trends);
        }, this, this::renderDashboard, "Failed to load dashboard data");
    }

    @SuppressWarnings("NullAway")
    private void renderDashboard(final DashboardData data) {
        this.totalSessionsCard.setValue(formatNumber(data.totalSessions));
        this.totalSessionsCard.setTrend(data.trends.totalSessionsChange());

        this.completedSessionsCard.setValue(formatNumber(data.completedSessions));
        this.completedSessionsCard.setTrend(data.trends.completedSessionsChange());

        this.activeStudentsCard.setValue(formatNumber(data.activeStudents));
        this.activeStudentsCard.setTrend(data.trends.activeStudentsChange());

        this.todaySessionsCard.setValue(formatNumber(data.todaySessions));
        this.todaySessionsCard.setTrend(data.trends.todaySessionsChange());

        this.totalUsersCard.setValue(formatNumber(data.totalUsers));
        this.totalUsersCard.setTrend(data.trends.totalUsersChange());

        this.publishedExercisesCard.setValue(formatNumber(data.publishedExercises));
        this.publishedExercisesCard.setTrend(data.trends.publishedExercisesChange());

        final var avgRatePct = String.format("%.1f%%", data.avgSuccessRate * 100);
        this.avgSuccessRateCard.setValue(avgRatePct);

        this.sessionsChart.setChartContent(ChartUtil.lineChart(data.dailySessionCounts, null, 800, 300));
        this.topExercisesChart
                .setChartContent(ChartUtil.horizontalBarChart(toLinkedHashMap(data.topExercises), null, 600, 250));
        this.completionRateChart
                .setChartContent(ChartUtil.donutChart(data.completionRateHistogram, "sessions", null, 220));
        this.hintsChart.setChartContent(ChartUtil.horizontalBarChart(data.hintUsageBuckets, null, 600, 250));

        this.activityFeedContainer.removeAll();
        this.activityFeedContainer.add(new RecentActivityFeed(data.recentSessions));

        this.topStudentsContainer.removeAll();
        this.topStudentsContainer.add(new TopStudentsList(data.topStudents));
    }

    private static String formatNumber(final long n) {
        if (n >= 1000) {
            return String.format("%,d", n);
        }
        return String.valueOf(n);
    }

    private static Map<String, Integer> toLinkedHashMap(final Map<String, Integer> map) {
        if (map instanceof LinkedHashMap) {
            return (LinkedHashMap<String, Integer>) map;
        }
        final var result = new LinkedHashMap<String, Integer>();
        if (map != null) {
            result.putAll(map);
        }
        return result;
    }

    private record DashboardData(long totalSessions, long completedSessions, long activeStudents, long todaySessions,
            long totalUsers, long publishedExercises, double avgSuccessRate, Map<LocalDate, Long> dailySessionCounts,
            Map<String, Integer> topExercises, Map<String, Integer> completionRateHistogram,
            Map<String, Integer> hintUsageBuckets, List<StudentSessionViewDto> recentSessions,
            List<StudentProgressSummaryDto> topStudents, DashboardTrendDto trends) {
    }
}
