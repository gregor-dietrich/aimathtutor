package de.vptr.aimathtutor.util;

import com.vaadin.flow.component.Component;

/**
 * Utility for registering the Graspable Math server-side connector.
 * Shared between views that embed the Graspable Math canvas.
 */
public final class GraspableMathConnector {

    private GraspableMathConnector() {
        // Utility class
    }

    /**
     * Registers a server-side connector that JavaScript can call.
     * Exposes {@code window.graspableViewConnector.onMathAction} which forwards
     * to the component's {@code onMathAction} server method.
     *
     * @param component the Vaadin component hosting the Graspable Math canvas
     */
    public static void register(final Component component) {
        final var ui = component.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        ui.getPage().executeJs(
                "window.graspableViewConnector = { onMathAction: function(type, before, after) { "
                        + "   $0.$server.onMathAction(type, before, after); "
                        + "}}",
                component.getElement());
    }
}
