package de.vptr.aimathtutor.component.dashboard;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.vptr.aimathtutor.dto.StudentSessionViewDto;
import jakarta.annotation.Nullable;

/**
 * Styled list of recent student sessions with relative timestamps.
 */
public class RecentActivityFeed extends VerticalLayout {

    /**
     * Creates an activity feed with the given sessions.
     *
     * @param sessions
     *            the list of recent sessions (may be null or empty)
     */
    public RecentActivityFeed(@Nullable final List<StudentSessionViewDto> sessions) {
        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "8px").set("background",
                "var(--lumo-base-color)");

        final var title = new Span("Recent Activity (7 days)");
        title.getStyle().set("font-size", "14px").set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)").set("margin-bottom", "8px");
        add(title);

        if (sessions == null || sessions.isEmpty()) {
            final var empty = new Span("No recent activity");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "13px");
            add(empty);
            return;
        }

        for (final var session : sessions) {
            final var item = new VerticalLayout();
            item.setPadding(false);
            item.setSpacing(false);
            item.getStyle().set("padding", "6px 0").set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

            final var line = new Span();
            final var userName = ChartUtil.escapeXml(session.username != null ? session.username : "Unknown");
            final var exTitle = ChartUtil.escapeXml(session.exerciseTitle != null ? session.exerciseTitle : "Unknown");
            final var time = formatRelativeTime(session.endTime != null ? session.endTime : session.startTime);
            line.getElement().setProperty("innerHTML", "<strong>" + userName + "</strong> — " + exTitle + " — " + time);
            line.getStyle().set("font-size", "13px").set("color", "var(--lumo-body-text-color)");

            item.add(line);
            add(item);
        }
    }

    private static String formatRelativeTime(@Nullable final LocalDateTime time) {
        if (time == null) {
            return "unknown";
        }
        final var now = LocalDateTime.now();
        final var minutes = ChronoUnit.MINUTES.between(time, now);
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        final var hours = ChronoUnit.HOURS.between(time, now);
        if (hours < 24) {
            return hours + "h ago";
        }
        final var days = ChronoUnit.DAYS.between(time, now);
        return days + "d ago";
    }
}
