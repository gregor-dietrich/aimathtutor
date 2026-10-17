package de.vptr.aimathtutor.dto;

import java.io.Serializable;

/**
 * A user's chosen look of the app: which design is loaded and whether it renders light, dark or follows the system.
 * <p>
 * The enum constants are persisted by name ({@code EnumType.STRING} in {@code users.design} and
 * {@code users.color_mode}), so renaming or removing a constant needs a migration of the stored values.
 *
 * @param design
 *            the design to load
 * @param colorMode
 *            the colour mode to render it in
 */
public record Appearance(Design design, ColorMode colorMode) implements Serializable {

    /** The look of an account that never chose one; equals the column defaults of the migration. */
    public static final Appearance DEFAULT = new Appearance(Design.VELLUM, ColorMode.SYSTEM);

    /** The base stylesheet family a {@link Design} builds on. */
    public enum Base {
        LUMO("lumo"), AURA("aura");

        private final String value;

        Base(final String value) {
            this.value = value;
        }

        /**
         * Returns the value of the document attribute naming this base.
         *
         * @return the value of the document attribute naming this base
         */
        public String getValue() {
            return this.value;
        }
    }

    /** The selectable designs. */
    public enum Design {
        VELLUM("Vellum", "vellum", Base.AURA), POWER_UP("Power-Up", "powerup", Base.LUMO),
        VIBRANIUM("Vibranium", "vibranium", Base.LUMO), CHALKBOARD("Chalkboard", "chalkboard", Base.LUMO);

        private final String displayName;
        private final String value;
        private final Base base;

        Design(final String displayName, final String value, final Base base) {
            this.displayName = displayName;
            this.value = value;
            this.base = base;
        }

        /**
         * Returns the name shown to users.
         *
         * @return the name shown to users
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Returns the value of the document attribute naming this design.
         *
         * @return the value of the document attribute naming this design
         */
        public String getValue() {
            return this.value;
        }

        /**
         * Returns the base stylesheet family this design builds on.
         *
         * @return the base stylesheet family this design builds on
         */
        public Base getBase() {
            return this.base;
        }
    }

    /** The selectable colour modes. */
    public enum ColorMode {
        LIGHT("Light", "light"), DARK("Dark", "dark"), SYSTEM("Follow system", "system");

        private final String displayName;
        private final String value;

        ColorMode(final String displayName, final String value) {
            this.displayName = displayName;
            this.value = value;
        }

        /**
         * Returns the name shown to users.
         *
         * @return the name shown to users
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Returns the value of the document attribute naming this mode.
         *
         * @return the value of the document attribute naming this mode
         */
        public String getValue() {
            return this.value;
        }

        /**
         * Returns the next mode in the cycle light, dark, system.
         *
         * @return the next mode in the cycle light, dark, system
         */
        public ColorMode next() {
            return switch (this) {
                case LIGHT -> DARK;
                case DARK -> SYSTEM;
                case SYSTEM -> LIGHT;
            };
        }
    }

    /**
     * Returns a copy of this appearance with another colour mode.
     *
     * @param newColorMode
     *            the colour mode to switch to
     * @return a copy of this appearance with the given colour mode
     */
    public Appearance withColorMode(final ColorMode newColorMode) {
        return new Appearance(this.design, newColorMode);
    }
}
