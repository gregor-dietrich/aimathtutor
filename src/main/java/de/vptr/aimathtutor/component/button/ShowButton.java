package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

public class ShowButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Show";

    public ShowButton(final ComponentEventListener<ClickEvent<Button>> showAction, final String tooltipText) {
        super("", showAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        this.setIcon(LineAwesomeIcon.EYE_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public ShowButton(final ComponentEventListener<ClickEvent<Button>> showAction) {
        this(showAction, null);
    }
}
