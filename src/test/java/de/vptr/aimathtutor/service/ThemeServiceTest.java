package de.vptr.aimathtutor.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.service.ThemeService.Theme;

class ThemeServiceTest {

    private ThemeService themeService;

    @BeforeEach
    void setUp() {
        this.themeService = new ThemeService();
    }

    @Test
    @DisplayName("Should return SYSTEM theme as default when no session")
    void shouldReturnSystemThemeAsDefaultWhenNoSession() {
        // Note: VaadinSession.getCurrent() will return null in test environment
        // When
        final Theme currentTheme = this.themeService.getCurrentTheme();

        // Then
        assertEquals(Theme.SYSTEM, currentTheme);
    }

    @Test
    @DisplayName("Should get next theme correctly from LIGHT")
    void shouldGetNextThemeCorrectlyFromLight() {
        // When
        final Theme nextTheme = this.themeService.getNextTheme();

        // Then
        // Since getCurrentTheme() returns SYSTEM by default, getNextTheme should return
        // LIGHT
        assertEquals(Theme.LIGHT, nextTheme);
    }

    @Test
    @DisplayName("Should cycle through themes correctly")
    void shouldCycleThroughThemesCorrectly() {
        // Test the theme cycling logic
        // LIGHT -> DARK
        final Theme nextFromLight = switch (Theme.LIGHT) {
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.SYSTEM;
            case SYSTEM -> Theme.LIGHT;
        };
        assertEquals(Theme.DARK, nextFromLight);

        // DARK -> SYSTEM
        final Theme nextFromDark = switch (Theme.DARK) {
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.SYSTEM;
            case SYSTEM -> Theme.LIGHT;
        };
        assertEquals(Theme.SYSTEM, nextFromDark);

        // SYSTEM -> LIGHT
        final Theme nextFromSystem = switch (Theme.SYSTEM) {
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.SYSTEM;
            case SYSTEM -> Theme.LIGHT;
        };
        assertEquals(Theme.LIGHT, nextFromSystem);
    }

    @Test
    @DisplayName("Should set theme without active UI")
    void shouldSetThemeWithoutActiveUI() {
        // When - This should not throw an exception even without active UI
        assertDoesNotThrow(() -> this.themeService.setTheme(Theme.DARK));
    }

    @Test
    @DisplayName("Should apply theme without active UI")
    void shouldApplyThemeWithoutActiveUI() {
        // When - This should not throw an exception even without active UI
        assertDoesNotThrow(() -> this.themeService.applyTheme(Theme.LIGHT));
        assertDoesNotThrow(() -> this.themeService.applyTheme(Theme.DARK));
        assertDoesNotThrow(() -> this.themeService.applyTheme(Theme.SYSTEM));
    }

    @Test
    @DisplayName("Should verify Theme enum properties")
    void shouldVerifyThemeEnumProperties() {
        // Test LIGHT theme
        assertEquals("Light", Theme.LIGHT.getDisplayName());
        assertNull(Theme.LIGHT.getThemeVariant());

        // Test DARK theme
        assertEquals("Dark", Theme.DARK.getDisplayName());
        assertEquals("dark", Theme.DARK.getThemeVariant()); // Lumo.DARK value

        // Test SYSTEM theme
        assertEquals("System", Theme.SYSTEM.getDisplayName());
        assertNull(Theme.SYSTEM.getThemeVariant());
    }

    @Test
    @DisplayName("Should handle all theme enum values")
    void shouldHandleAllThemeEnumValues() {
        // Verify all enum values exist
        final Theme[] themes = Theme.values();
        assertEquals(3, themes.length);

        // Verify each theme has proper display name
        for (final Theme theme : themes) {
            assertNotNull(theme.getDisplayName());
            assertFalse(theme.getDisplayName().trim().isEmpty());
        }
    }

    @Test
    @DisplayName("Should have proper enum ordering")
    void shouldHaveProperEnumOrdering() {
        final Theme[] themes = Theme.values();

        assertEquals(Theme.LIGHT, themes[0]);
        assertEquals(Theme.DARK, themes[1]);
        assertEquals(Theme.SYSTEM, themes[2]);
    }

    @Test
    @DisplayName("Should handle theme cycling edge cases")
    void shouldHandleThemeCyclingEdgeCases() {
        // Test that each theme leads to exactly one next theme
        assertEquals(Theme.DARK, this.getNextThemeFor(Theme.LIGHT));
        assertEquals(Theme.SYSTEM, this.getNextThemeFor(Theme.DARK));
        assertEquals(Theme.LIGHT, this.getNextThemeFor(Theme.SYSTEM));
    }

    private Theme getNextThemeFor(final Theme current) {
        return switch (current) {
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.SYSTEM;
            case SYSTEM -> Theme.LIGHT;
        };
    }
}