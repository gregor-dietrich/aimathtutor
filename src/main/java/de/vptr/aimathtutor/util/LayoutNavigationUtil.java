package de.vptr.aimathtutor.util;

import java.util.Optional;
import java.util.function.Predicate;

import com.vaadin.flow.component.UI;

/**
 * Utility for handling navigation-based button visibility in layouts.
 */
public final class LayoutNavigationUtil {

    private LayoutNavigationUtil() {
    }

    /**
     * Updates button visibility based on the current route path.
     *
     * @param ui
     *            the current UI optional
     * @param authenticated
     *            whether the user is authenticated
     * @param shouldShowButtons
     *            predicate to determine if buttons should be shown for a given path
     * @param showButtons
     *            action to show buttons
     * @param hideButtons
     *            action to hide buttons
     */
    public static void updateButtonVisibility(final Optional<UI> ui, final boolean authenticated,
            final Predicate<String> shouldShowButtons, final Runnable showButtons, final Runnable hideButtons) {
        if (authenticated) {
            if (ui.isEmpty()) {
                hideButtons.run();
            } else {
                ui.ifPresent(currentUi -> {
                    final var location = currentUi.getInternals().getActiveViewLocation();
                    if (location == null) {
                        hideButtons.run();
                    } else {
                        final var path = location.getPath();
                        if (shouldShowButtons.test(path)) {
                            showButtons.run();
                        } else {
                            hideButtons.run();
                        }
                    }
                });
            }
        } else {
            hideButtons.run();
        }
    }
}
