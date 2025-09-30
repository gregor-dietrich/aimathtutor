package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

public class EditButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Edit";

    public EditButton(final ComponentEventListener<ClickEvent<Button>> editAction, final String tooltipText) {
        super("", editAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        this.setIcon(LineAwesomeIcon.EDIT_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public EditButton(final ComponentEventListener<ClickEvent<Button>> editAction) {
        this(editAction, null);
    }
}
