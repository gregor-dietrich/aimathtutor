package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

public class LogoutButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Logout";

    public LogoutButton(final ComponentEventListener<ClickEvent<Button>> logoutAction, final String tooltipText) {
        super("", logoutAction);
        this.addThemeVariants(ButtonVariant.LUMO_ERROR);
        this.setIcon(LineAwesomeIcon.POWER_OFF_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public LogoutButton(final ComponentEventListener<ClickEvent<Button>> logoutAction) {
        this(logoutAction, null);
    }
}
