package de.vptr.aimathtutor.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.vaadin.flow.component.badge.BadgeVariant;

import de.vptr.aimathtutor.dto.ExerciseDto.DifficultyLevel;

@DisplayName("DifficultyBadge")
class DifficultyBadgeTest {

    private static final Map<DifficultyLevel,
            BadgeVariant> EXPECTED = Map.of(DifficultyLevel.BEGINNER, BadgeVariant.SUCCESS,
                    DifficultyLevel.INTERMEDIATE, BadgeVariant.CONTRAST, DifficultyLevel.ADVANCED, BadgeVariant.ERROR,
                    DifficultyLevel.EXPERT, BadgeVariant.ERROR);

    @ParameterizedTest
    @EnumSource(DifficultyLevel.class)
    @DisplayName("shows the text and exactly the variant mapped to the level")
    void testConstructor_textAndVariant(final DifficultyLevel level) {
        final var expected = EXPECTED.get(level);
        assertNotNull(expected, () -> "no expected variant for " + level);

        final var badge = new DifficultyBadge("text " + level, level);

        assertEquals("text " + level, badge.getText());
        final var themes = badge.getThemeNames();
        assertTrue(themes.contains(expected.getVariantName()), () -> "theme names: " + themes);
        for (final var other : List.of(BadgeVariant.SUCCESS, BadgeVariant.CONTRAST, BadgeVariant.ERROR)) {
            if (other != expected) {
                assertFalse(themes.contains(other.getVariantName()), () -> "theme names: " + themes);
            }
        }
    }
}
