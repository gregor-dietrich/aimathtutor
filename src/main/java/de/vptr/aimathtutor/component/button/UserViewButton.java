package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class UserViewButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Return to Homepage";

    public UserViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction, final String tooltipText) {
        super("", viewAction);
        this.setIcon(LineAwesomeIcon.HOME_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public UserViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction) {
        this(viewAction, null);
    }
}
