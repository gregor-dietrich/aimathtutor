package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button that hides or conceals an item. Displays an eye-slash icon and
 * delegates the action to the provided click listener.
 */
public class HideButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Hide";

    public HideButton(final ComponentEventListener<ClickEvent<Button>> hideAction, final String tooltipText) {
        super("", hideAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_WARNING);
        this.setIcon(LineAwesomeIcon.EYE_SLASH_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public HideButton(final ComponentEventListener<ClickEvent<Button>> hideAction) {
        this(hideAction, null);
    }
}
