package de.vptr.aimathtutor.component.dashboard;

import java.util.List;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.vptr.aimathtutor.dto.StudentProgressSummaryDto;
import jakarta.annotation.Nullable;

/**
 * Compact ranked list of top students by session completion count.
 */
public class TopStudentsList extends VerticalLayout {

    /**
     * Creates a top students list with the given student summaries.
     *
     * @param students
     *            ordered list of top students (may be null or empty)
     */
    public TopStudentsList(@Nullable final List<StudentProgressSummaryDto> students) {
        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("border", "1px solid var(--lumo-contrast-10pct)").set("border-radius", "8px").set("background",
                "var(--lumo-base-color)");

        final var title = new Span("Top Students by Completion");
        title.getStyle().set("font-size", "14px").set("font-weight", "600")
                .set("color", "var(--lumo-primary-text-color)").set("margin-bottom", "8px");
        add(title);

        if (students == null || students.isEmpty()) {
            final var empty = new Span("No student data available");
            empty.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "13px");
            add(empty);
            return;
        }

        int rank = 1;
        for (final var student : students) {
            if (student == null) {
                continue;
            }
            final var row = new HorizontalLayout();
            row.setWidthFull();
            row.setPadding(false);
            row.setSpacing(true);
            row.getStyle().set("padding", "6px 0").set("border-bottom", "1px solid var(--lumo-contrast-5pct)");

            final var rankSpan = new Span(String.valueOf(rank++));
            rankSpan.getStyle().set("font-size", "13px").set("font-weight", "700")
                    .set("color", "var(--lumo-primary-color)").set("width", "24px");

            final var nameSpan = new Span(student.username != null ? student.username : "Unknown");
            nameSpan.getStyle().set("font-size", "13px").set("flex", "1");

            final var countSpan =
                    new Span((student.completedSessions != null ? student.completedSessions : 0) + " sessions");
            countSpan.getStyle().set("font-size", "12px").set("color", "var(--lumo-secondary-text-color)");

            row.add(rankSpan, nameSpan, countSpan);
            add(row);
        }
    }
}
