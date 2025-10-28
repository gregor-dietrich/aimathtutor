package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

/**
 * Button that navigates the user to the main user/home view. Displays a
 * home icon and forwards click events to the provided listener.
 */
public class UserViewButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Return to Homepage";

    /**
     * Constructs a UserViewButton with the specified action and tooltip.
     */
    public UserViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction, final String tooltipText) {
        super("", viewAction);
        this.setIcon(LineAwesomeIcon.HOME_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public UserViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction) {
        this(viewAction, null);
    }
}
