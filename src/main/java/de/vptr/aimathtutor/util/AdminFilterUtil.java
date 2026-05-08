package de.vptr.aimathtutor.util;

import java.time.LocalDate;
import java.util.Optional;

import com.vaadin.flow.component.datepicker.DatePicker;

/**
 * Utilities for admin view filter operations.
 */
public final class AdminFilterUtil {

    private AdminFilterUtil() {
    }

    /**
     * Validates date range pickers and returns the selected range if valid.
     *
     * @param startPicker
     *            the start date picker
     * @param endPicker
     *            the end date picker
     * @return the validated date range, or empty if validation failed (warning shown)
     */
    public static Optional<DateRange> validateDateRange(final DatePicker startPicker, final DatePicker endPicker) {
        final LocalDate startDate = startPicker.getValue();
        final LocalDate endDate = endPicker.getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return Optional.empty();
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return Optional.empty();
        }

        return Optional.of(new DateRange(startDate, endDate));
    }

    /**
     * Immutable holder for a validated date range.
     */
    public record DateRange(LocalDate start, LocalDate end) {
    }
}
