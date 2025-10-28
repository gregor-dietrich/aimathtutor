package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

/**
 * TODO: Class documentation.
 */
public class AdminViewButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Admin Panel";

    /**
     * Constructs an AdminViewButton with the specified action and tooltip.
     */
    public AdminViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction, final String tooltipText) {
        super("", viewAction);
        this.setIcon(LineAwesomeIcon.TOOLS_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public AdminViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction) {
        this(viewAction, null);
    }
}
