package de.vptr.aimathtutor.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.dto.Appearance.Base;
import de.vptr.aimathtutor.dto.Appearance.ColorMode;
import de.vptr.aimathtutor.dto.Appearance.Design;

class AppearanceTest {

    @Test
    @DisplayName("The constructor rejects a null design and a null colour mode")
    @SuppressWarnings("NullAway")
    void constructorRejectsNulls() {
        assertThrows(NullPointerException.class, () -> new Appearance(null, ColorMode.DARK));
        assertThrows(NullPointerException.class, () -> new Appearance(Design.VELLUM, null));
    }

    @Test
    @DisplayName("ColorMode.next cycles light, dark, system")
    void colorModeCycles() {
        assertEquals(ColorMode.DARK, ColorMode.LIGHT.next());
        assertEquals(ColorMode.SYSTEM, ColorMode.DARK.next());
        assertEquals(ColorMode.LIGHT, ColorMode.SYSTEM.next());
    }

    @Test
    @DisplayName("Designs map to their base, attribute value and display name")
    void designMapping() {
        assertEquals(Base.AURA, Design.VELLUM.getBase());
        assertEquals(Base.LUMO, Design.POWER_UP.getBase());
        assertEquals(Base.LUMO, Design.VIBRANIUM.getBase());
        assertEquals(Base.LUMO, Design.CHALKBOARD.getBase());
        assertEquals("vellum", Design.VELLUM.getValue());
        assertEquals("powerup", Design.POWER_UP.getValue());
        assertEquals("vibranium", Design.VIBRANIUM.getValue());
        assertEquals("chalkboard", Design.CHALKBOARD.getValue());
        assertEquals("Power-Up", Design.POWER_UP.getDisplayName());
        assertEquals("lumo", Base.LUMO.getValue());
        assertEquals("aura", Base.AURA.getValue());
    }

    @Test
    @DisplayName("ColorMode attribute values and display names")
    void colorModeValues() {
        assertEquals("light", ColorMode.LIGHT.getValue());
        assertEquals("dark", ColorMode.DARK.getValue());
        assertEquals("system", ColorMode.SYSTEM.getValue());
        assertEquals("Follow system", ColorMode.SYSTEM.getDisplayName());
    }

    @Test
    @DisplayName("DEFAULT is Vellum following the system")
    void defaultAppearance() {
        assertEquals(new Appearance(Design.VELLUM, ColorMode.SYSTEM), Appearance.DEFAULT);
    }
}
