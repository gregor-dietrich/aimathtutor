package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;

import de.vptr.aimathtutor.dto.ExerciseDto;

/**
 * Badge showing an exercise difficulty, coloured by level: green for beginner, neutral for intermediate and red for
 * advanced and expert.
 */
public final class DifficultyBadge extends Badge {

    /**
     * Constructs a badge with the given text, styled for the given difficulty level.
     *
     * @param text
     *            the badge text
     * @param level
     *            the difficulty level that picks the colour
     */
    public DifficultyBadge(final String text, final ExerciseDto.DifficultyLevel level) {
        super(text);
        this.addThemeVariants(switch (level) {
            case BEGINNER -> BadgeVariant.SUCCESS;
            case INTERMEDIATE -> BadgeVariant.CONTRAST;
            case ADVANCED, EXPERT -> BadgeVariant.ERROR;
        });
    }
}
