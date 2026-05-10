package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * A 3px accent bar with a primary-color gradient, absolutely positioned at the top of its parent.
 *
 * <p>
 * Place inside a {@code position: relative} container to render a coloured top border similar to the KPI cards on the
 * admin dashboard.
 * </p>
 */
public class GlassAccentBar extends Div {

    private static final String ACCENT_GRADIENT = "linear-gradient(90deg, var(--lumo-primary-color),"
            + " var(--lumo-primary-color-50pct), var(--lumo-primary-color-10pct))";

    private static final String INITIAL_SHADOW = "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)";

    private static final String HOVER_SHADOW = "0 6px 12px rgba(0,0,0,0.1)";

    public GlassAccentBar() {
        this.getStyle().set("position", "absolute").set("top", "0").set("left", "0").set("right", "0")
                .set("height", "3px").set("background", ACCENT_GRADIENT).set("border-radius", "12px 12px 0 0");
    }

    /**
     * Adds a subtle lift-and-shadow hover effect to the given component. The component should have the standard glass
     * transition ({@code all 0.25s cubic-bezier(0.4, 0, 0.2, 1)}) already applied for a smooth animation.
     */
    public static void addHoverEffect(final Component component) {
        component.getElement().addEventListener("mouseenter", e -> {
            component.getStyle().set("box-shadow", HOVER_SHADOW).set("transform", "translateY(-2px)");
        });
        component.getElement().addEventListener("mouseleave", e -> {
            component.getStyle().set("box-shadow", INITIAL_SHADOW).set("transform", "translateY(0)");
        });
    }
}
