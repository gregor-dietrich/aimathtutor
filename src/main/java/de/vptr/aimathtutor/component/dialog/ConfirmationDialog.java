package de.vptr.aimathtutor.component.dialog;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;

/**
 * TODO: Class documentation.
 */
public class ConfirmationDialog extends ConfirmDialog {
    private static final String DEFAULT_CANCEL_BUTTON_TEXT = "Cancel";
    private static final String DEFAULT_CONFIRM_BUTTON_TEXT = "Delete";
    private static final String DEFAULT_DIALOG_TITLE = "Confirm";
    private static final String DEFAULT_DIALOG_TEXT = "Are you sure?";

    public ConfirmationDialog(final ComponentEventListener<ConfirmDialog.ConfirmEvent> confirmAction,
            final String dialogTitle, final String dialogText, final String confirmButtonText,
            final String cancelButtonText) {
        this.setHeader(dialogTitle != null ? dialogTitle : DEFAULT_DIALOG_TITLE);
        this.setText(dialogText != null ? dialogText : DEFAULT_DIALOG_TEXT);
        this.setConfirmText(confirmButtonText != null ? confirmButtonText : DEFAULT_CONFIRM_BUTTON_TEXT);
        this.setCancelText(cancelButtonText != null ? cancelButtonText : DEFAULT_CANCEL_BUTTON_TEXT);
        this.setCancelable(true);
        this.setCloseOnEsc(true);
        this.setConfirmButtonTheme("error primary");
        this.addConfirmListener(confirmAction);
    }

    public ConfirmationDialog(final ComponentEventListener<ConfirmDialog.ConfirmEvent> confirmAction,
            final String dialogTitle, final String dialogText, final String confirmButtonText) {
        this(confirmAction, dialogTitle, dialogText, confirmButtonText, null);
    }

    public ConfirmationDialog(final ComponentEventListener<ConfirmDialog.ConfirmEvent> confirmAction,
            final String dialogTitle, final String dialogText) {
        this(confirmAction, dialogTitle, dialogText, null);
    }

    public ConfirmationDialog(final ComponentEventListener<ConfirmDialog.ConfirmEvent> confirmAction,
            final String dialogTitle) {
        this(confirmAction, dialogTitle, null);
    }

    public ConfirmationDialog(final ComponentEventListener<ConfirmDialog.ConfirmEvent> confirmAction) {
        this(confirmAction, null);
    }
}
