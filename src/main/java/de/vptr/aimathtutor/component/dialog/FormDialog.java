package de.vptr.aimathtutor.component.dialog;

import com.vaadin.flow.component.dialog.Dialog;

/**
 * Base dialog for forms with predefined sizing and close behavior.
 */
public class FormDialog extends Dialog {

    private static final String DEFAULT_WIDTH = "800px";

    /**
     * Constructs a FormDialog with default width.
     */
    public FormDialog() {
        this(DEFAULT_WIDTH);
    }

    /**
     * Constructs a FormDialog with the specified width.
     */
    public FormDialog(final String width) {
        this.setWidth(width);
        this.setCloseOnEsc(true);
        this.setCloseOnOutsideClick(false);
    }

    public FormDialog(final String width, final String height) {
        this(width);
        this.setHeight(height);
    }
}
