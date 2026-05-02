package de.vptr.aimathtutor.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration of difficulty levels for exercises and math problems.
 * Maps to string values stored in the database and used in UI components.
 */
public enum DifficultyLevel {

    BEGINNER("beginner"),
    INTERMEDIATE("intermediate"),
    ADVANCED("advanced"),
    EXPERT("expert");

    private final String value;

    DifficultyLevel(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    /**
     * Converts a string value to the corresponding DifficultyLevel enum.
     *
     * @param value the string value to convert
     * @return the matching DifficultyLevel, or null if no match
     */
    public static DifficultyLevel fromString(final String value) {
        if (value == null) {
            return null;
        }
        for (final DifficultyLevel level : values()) {
            if (level.value.equalsIgnoreCase(value)) {
                return level;
            }
        }
        return null;
    }
}
