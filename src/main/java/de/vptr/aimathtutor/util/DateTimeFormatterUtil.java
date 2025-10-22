package de.vptr.aimathtutor.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Utility class for formatting dates and datetimes according to application
 * configuration. Configurable via application.properties properties.
 */
@ApplicationScoped
public class DateTimeFormatterUtil {

    @ConfigProperty(name = "app.date.format", defaultValue = "yyyy-MM-dd")
    String dateFormat;

    @ConfigProperty(name = "app.datetime.format", defaultValue = "yyyy-MM-dd HH:mm:ss")
    String dateTimeFormat;

    /**
     * Format a LocalDate using the configured date format
     *
     * @param date the LocalDate to format (may be null)
     * @return formatted date string, or null if input is null
     */
    public String formatDate(final LocalDate date) {
        if (date == null) {
            return null;
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.dateFormat);
        return date.format(formatter);
    }

    /**
     * Format a LocalDateTime using the configured datetime format
     *
     * @param dateTime the LocalDateTime to format (may be null)
     * @return formatted datetime string, or null if input is null
     */
    public String formatDateTime(final LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(this.dateTimeFormat);
        return dateTime.format(formatter);
    }

    /**
     * Get the configured date format pattern
     */
    public String getDateFormat() {
        return this.dateFormat;
    }

    /**
     * Get the configured datetime format pattern
     */
    public String getDateTimeFormat() {
        return this.dateTimeFormat;
    }
}
