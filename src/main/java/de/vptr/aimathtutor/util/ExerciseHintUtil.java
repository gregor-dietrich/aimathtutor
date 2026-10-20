package de.vptr.aimathtutor.util;

import java.util.List;

import jakarta.annotation.Nullable;

/**
 * Helpers for reading the hints an exercise stores in a single text field. The field holds one hint per line, so a hint
 * may contain any punctuation (including {@code ;} and {@code |}) but never a line break.
 */
public final class ExerciseHintUtil {

    private ExerciseHintUtil() {
        // Utility class
    }

    /**
     * Splits the stored hints into one entry per line. Each hint is stripped of surrounding whitespace and blank lines
     * are skipped. Line breaks may be {@code \n}, {@code \r\n} or a lone {@code \r}.
     *
     * @param hints
     *            the raw hints text, possibly {@code null} or blank
     * @return an unmodifiable list of the hints in order, empty when there are none
     */
    public static List<String> parseHints(@Nullable final String hints) {
        return hints == null ? List.of() : hints.lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
    }
}
