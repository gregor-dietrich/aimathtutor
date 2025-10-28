package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button used to open an edit form for an entity. Shows an edit icon and
 * delegates click handling to the provided listener.
 */
public class EditButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Edit";

    /**
     * Constructs an EditButton with the specified action and tooltip.
     */
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
