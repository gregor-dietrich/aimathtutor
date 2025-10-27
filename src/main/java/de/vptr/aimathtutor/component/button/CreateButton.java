package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button used to create a new resource. Shows a + icon and applies a success
 * theme. Callers provide a click listener to handle the creation action.
 */
public class CreateButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Create";

    public CreateButton(final ComponentEventListener<ClickEvent<Button>> createAction, final String tooltipText) {
        super("", createAction);
        this.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        this.setIcon(LineAwesomeIcon.PLUS_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public CreateButton(final ComponentEventListener<ClickEvent<Button>> createAction) {
        this(createAction, null);
    }
}
