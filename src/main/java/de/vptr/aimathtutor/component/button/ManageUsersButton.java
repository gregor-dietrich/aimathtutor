package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button used to open the manage users view. Shows an appropriate icon and
 * optional tooltip.
 */
public class ManageUsersButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Manage Users";

    /**
     * Constructs a ManageUsersButton with the specified action and tooltip.
     */
    public ManageUsersButton(final ComponentEventListener<ClickEvent<Button>> addUserAction, final String tooltipText) {
        super("", addUserAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_WARNING);
        this.setIcon(LineAwesomeIcon.USERS_COG_SOLID.create());
        final String label = tooltipText != null && !tooltipText.trim().isEmpty() ? tooltipText : DEFAULT_TOOLTIP;
        this.setTooltipText(label);
        this.getElement().setAttribute("aria-label", label);
    }

    public ManageUsersButton(final ComponentEventListener<ClickEvent<Button>> addUserAction) {
        this(addUserAction, null);
    }
}
