package de.vptr.aimathtutor.component;

import com.vaadin.flow.component.html.Div;

import de.vptr.aimathtutor.util.AppStyles;

/**
 * A 3px accent bar along the top edge of its parent, drawn by the {@code accent-bar} class.
 *
 * <p>
 * The bar is absolutely positioned, so its parent must be positioned, for example a {@code glass-card}.
 * </p>
 */
public class GlassAccentBar extends Div {

    public GlassAccentBar() {
        this.addClassName(AppStyles.ACCENT_BAR);
    }
}
