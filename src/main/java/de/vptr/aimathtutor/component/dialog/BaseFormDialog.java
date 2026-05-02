package de.vptr.aimathtutor.component.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;

/**
 * Reusable base dialog for create/edit operations.
 *
 * <p>Provides a standard layout with title, form area, and save/cancel buttons.
 * Subclasses populate the form and handle the save action.</p>
 *
 * @param <T> the type of bean managed by this dialog
 */
public abstract class BaseFormDialog<T> extends Dialog {

    protected final Binder<T> binder;
    protected final FormLayout formLayout;
    protected final HorizontalLayout buttonLayout;

    /**
     * Creates a new base form dialog.
     *
     * @param beanType the class of the bean to bind
     * @param title    the dialog title
     */
    protected BaseFormDialog(final Class<T> beanType, final String title) {
        this.binder = new Binder<>(beanType);
        this.formLayout = new FormLayout();
        this.formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var titleLabel = new H3(title);

        this.buttonLayout = new HorizontalLayout();
        this.buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.onSave());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        final var cancelButton = new Button("Cancel", e -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        this.buttonLayout.add(saveButton, cancelButton);

        final var layout = new VerticalLayout(titleLabel, this.formLayout, this.buttonLayout);
        layout.setSpacing(true);
        layout.setPadding(false);
        this.add(layout);
    }

    /**
     * Builds the form fields. Called by subclasses after construction.
     */
    protected abstract void buildForm();

    /**
     * Called when the save button is clicked.
     */
    protected abstract void onSave();

    /**
     * Opens the dialog and reads the given bean into the binder.
     *
     * @param bean the bean to edit, or null for a new instance
     */
    public void open(final T bean) {
        this.binder.readBean(bean);
        super.open();
    }
}
