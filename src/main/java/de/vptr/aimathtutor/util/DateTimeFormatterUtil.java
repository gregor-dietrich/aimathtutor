package de.vptr.aimathtutor.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Utility class for formatting dates and datetimes according to application configuration. Configurable via
 * application.properties properties.
 */
@ApplicationScoped
public class DateTimeFormatterUtil {

    @ConfigProperty(name = "app.date.format", defaultValue = "yyyy-MM-dd")
    @Nullable
    String dateFormat;

    @ConfigProperty(name = "app.datetime.format", defaultValue = "yyyy-MM-dd HH:mm:ss")
    @Nullable
    String dateTimeFormat;

    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter dateTimeFormatter;

    @PostConstruct
    void init() {
        this.dateFormatter = DateTimeFormatter.ofPattern(this.dateFormat != null ? this.dateFormat : "yyyy-MM-dd");
        this.dateTimeFormatter =
                DateTimeFormatter.ofPattern(this.dateTimeFormat != null ? this.dateTimeFormat : "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Format a LocalDate using the configured date format
     *
     * @param date
     *            the LocalDate to format (may be null)
     * @return formatted date string, or null if input is null
     */
    @Nullable
    public String formatDate(final LocalDate date) {
        if (date == null || this.dateFormatter == null) {
            return null;
        }
        return date.format(this.dateFormatter);
    }

    /**
     * Format a LocalDateTime using the configured datetime format
     *
     * @param dateTime
     *            the LocalDateTime to format (may be null)
     * @return formatted datetime string, or null if input is null
     */
    @Nullable
    public String formatDateTime(final LocalDateTime dateTime) {
        if (dateTime == null || this.dateTimeFormatter == null) {
            return null;
        }
        return dateTime.format(this.dateTimeFormatter);
    }

    /**
     * Get the configured date format pattern
     */
    @Nullable
    public String getDateFormat() {
        return this.dateFormat;
    }

    /**
     * Get the configured datetime format pattern
     */
    @Nullable
    public String getDateTimeFormat() {
        return this.dateTimeFormat;
    }
}
