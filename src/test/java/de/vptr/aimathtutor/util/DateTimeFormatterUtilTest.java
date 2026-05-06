package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class DateTimeFormatterUtilTest {

    @Inject
    DateTimeFormatterUtil util;

    @Test
    @DisplayName("formatDate returns null for null input")
    void testFormatDate_null() {
        assertNull(this.util.formatDate(null));
    }

    @Test
    @DisplayName("formatDate returns non-null string for a valid date")
    void testFormatDate_valid() {
        final String result = this.util.formatDate(LocalDate.of(2025, 6, 15));
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("formatDate uses configured dd.MM.yyyy format")
    void testFormatDate_defaultFormat() {
        final String result = this.util.formatDate(LocalDate.of(2025, 6, 15));
        assertEquals("15.06.2025", result);
    }

    @Test
    @DisplayName("formatDateTime returns null for null input")
    void testFormatDateTime_null() {
        assertNull(this.util.formatDateTime(null));
    }

    @Test
    @DisplayName("formatDateTime returns non-null string for a valid datetime")
    void testFormatDateTime_valid() {
        final String result = this.util.formatDateTime(LocalDateTime.of(2025, 6, 15, 10, 30, 0));
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("formatDateTime uses configured dd.MM.yyyy HH:mm:ss format")
    void testFormatDateTime_defaultFormat() {
        final String result = this.util.formatDateTime(LocalDateTime.of(2025, 6, 15, 10, 30, 45));
        assertEquals("15.06.2025 10:30:45", result);
    }

    @Test
    @DisplayName("getDateFormat returns non-blank pattern")
    void testGetDateFormat() {
        final String format = this.util.getDateFormat();
        assertNotNull(format);
        assertFalse(format.isBlank());
    }

    @Test
    @DisplayName("getDateTimeFormat returns non-blank pattern")
    void testGetDateTimeFormat() {
        final String format = this.util.getDateTimeFormat();
        assertNotNull(format);
        assertFalse(format.isBlank());
    }

    @Test
    @DisplayName("formatDate and formatDateTime outputs contain year component")
    void testOutputsContainYear() {
        final String date = this.util.formatDate(LocalDate.of(2025, 1, 1));
        final String dateTime = this.util.formatDateTime(LocalDateTime.of(2025, 1, 1, 0, 0, 0));
        assertTrue(date.contains("2025"), "Formatted date should contain the year");
        assertTrue(dateTime.contains("2025"), "Formatted datetime should contain the year");
    }

}
