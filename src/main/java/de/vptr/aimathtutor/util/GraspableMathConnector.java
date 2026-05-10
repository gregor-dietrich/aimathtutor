package de.vptr.aimathtutor.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

/**
 * Utility for registering the Graspable Math server-side connector. Shared between views that embed the Graspable Math
 * canvas.
 */
public final class GraspableMathConnector {

    private GraspableMathConnector() {
        // Utility class
    }

    /**
     * Creates and styles the Graspable Math canvas container div with the standard workspace dimensions and borders.
     *
     * @return a new {@code Div} with id {@code graspable-canvas} ready to embed the Graspable Math widget
     */
    public static Div createCanvas() {
        final var canvas = new Div();
        canvas.setId("graspable-canvas");
        canvas.getStyle().set("width", "100%").set("height", AppConstants.CANVAS_HEIGHT_WORKSPACE)
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "var(--lumo-border-radius-m)").set("background-color", "var(--lumo-base-color)")
                .set("margin-top", "1rem");
        return canvas;
    }

    /**
     * Registers a server-side connector that JavaScript can call. Exposes
     * {@code window.graspableViewConnector.onMathAction} which forwards to the component's {@code onMathAction} server
     * method.
     *
     * @param component
     *            the Vaadin component hosting the Graspable Math canvas
     */
    public static void register(final Component component) {
        final var ui = component.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        ui.getPage().executeJs("window.graspableViewConnector = { onMathAction: function(type, before, after) { "
                + "   $0.$server.onMathAction(type, before, after); " + "}}", component.getElement());
    }
}
