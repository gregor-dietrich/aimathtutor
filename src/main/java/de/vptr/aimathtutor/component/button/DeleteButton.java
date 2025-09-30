package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

import de.vptr.aimathtutor.component.dialog.ConfirmationDialog;

public class DeleteButton extends Button {
    private final static String DEFAULT_TOOLTIP = "Delete";

    private final ComponentEventListener<ClickEvent<Button>> deleteAction;

    public DeleteButton(final ComponentEventListener<ClickEvent<Button>> deleteAction, final String tooltipText) {
        super("");
        this.deleteAction = deleteAction;
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        this.setIcon(LineAwesomeIcon.TRASH_ALT_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
        this.addClickListener(this::showConfirmationDialog);
    }

    public DeleteButton(final ComponentEventListener<ClickEvent<Button>> deleteAction) {
        this(deleteAction, null);
    }

    private void showConfirmationDialog(final ClickEvent<Button> event) {
        final var confirmDialog = new ConfirmationDialog(e -> {
            if (this.deleteAction != null) {
                this.deleteAction.onComponentEvent(event);
            }
        });
        confirmDialog.open();
    }
}
