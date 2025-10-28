package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button used to restore previously deleted items. Shows a restore icon
 * and delegates click handling to the supplied listener.
 */
public class RestoreButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Restore";

    /**
     * Constructs a RestoreButton with the specified action and tooltip.
     */
    public RestoreButton(final ComponentEventListener<ClickEvent<Button>> restoreAction, final String tooltipText) {
        super("", restoreAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
        this.setIcon(LineAwesomeIcon.TRASH_RESTORE_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public RestoreButton(final ComponentEventListener<ClickEvent<Button>> restoreAction) {
        this(restoreAction, null);
    }
}
