package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

public class ManageUsersButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Manage Users";

    public ManageUsersButton(final ComponentEventListener<ClickEvent<Button>> addUserAction, final String tooltipText) {
        super("", addUserAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_WARNING);
        this.setIcon(LineAwesomeIcon.USERS_COG_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public ManageUsersButton(final ComponentEventListener<ClickEvent<Button>> addUserAction) {
        this(addUserAction, null);
    }
}
