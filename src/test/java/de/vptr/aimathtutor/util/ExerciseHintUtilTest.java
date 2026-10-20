package de.vptr.aimathtutor.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExerciseHintUtilTest {

    @Test
    @DisplayName("parseHints returns an empty list for null, empty and blank input")
    void parseHintsReturnsEmptyForNullOrBlank() {
        assertEquals(List.of(), ExerciseHintUtil.parseHints(null));
        assertEquals(List.of(), ExerciseHintUtil.parseHints(""));
        assertEquals(List.of(), ExerciseHintUtil.parseHints("  \n \t"));
    }

    @Test
    @DisplayName("parseHints returns one hint per line")
    void parseHintsSplitsOnLines() {
        assertEquals(List.of("a", "b"), ExerciseHintUtil.parseHints("a\nb"));
    }

    @Test
    @DisplayName("parseHints accepts CRLF and lone CR line breaks")
    void parseHintsAcceptsAllLineBreaks() {
        assertEquals(List.of("a", "b"), ExerciseHintUtil.parseHints("a\r\nb"));
        assertEquals(List.of("a", "b"), ExerciseHintUtil.parseHints("a\rb"));
    }

    @Test
    @DisplayName("parseHints strips surrounding whitespace and skips blank lines")
    void parseHintsStripsAndSkipsBlankLines() {
        assertEquals(List.of("a", "b"), ExerciseHintUtil.parseHints("  a  \n\tb "));
        assertEquals(List.of("a", "b"), ExerciseHintUtil.parseHints("a\n\n  \nb"));
    }

    @Test
    @DisplayName("parseHints keeps separator-like punctuation inside one hint")
    void parseHintsKeepsPunctuation() {
        assertEquals(List.of("a; b | c"), ExerciseHintUtil.parseHints("a; b | c"));
    }

    @Test
    @DisplayName("parseHints returns an unmodifiable list")
    void parseHintsIsUnmodifiable() {
        final var hints = ExerciseHintUtil.parseHints("a\nb");
        assertThrows(UnsupportedOperationException.class, () -> hints.add("c"));
        assertThrows(UnsupportedOperationException.class, () -> ExerciseHintUtil.parseHints(null).add("c"));
    }
}
