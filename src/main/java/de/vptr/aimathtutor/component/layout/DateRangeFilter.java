package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reusable date-range filter component for admin views.
 *
 * <p>Provides start/end date pickers with a clear button. Useful for
 * search/filter operations that need date boundaries.</p>
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Vaadin UI components intentionally exposed for data binding")
public class DateRangeFilter extends HorizontalLayout {

    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Button clearButton;

    /**
     * Creates a new date range filter with the given labels.
     */
    public DateRangeFilter() {
        this.setSpacing(true);
        this.setAlignItems(Alignment.END);

        this.startDatePicker = new DatePicker("From");
        this.endDatePicker = new DatePicker("To");
        this.clearButton = new Button("Clear", e -> this.clear());
        this.clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        this.add(this.startDatePicker, this.endDatePicker, this.clearButton);
    }

    /**
     * Returns the selected start date, or null if none selected.
     */
    public DatePicker getStartDatePicker() {
        return this.startDatePicker;
    }

    /**
     * Returns the selected end date, or null if none selected.
     */
    public DatePicker getEndDatePicker() {
        return this.endDatePicker;
    }

    /**
     * Clears both date pickers.
     */
    public void clear() {
        this.startDatePicker.clear();
        this.endDatePicker.clear();
    }

    /**
     * Returns true if either date picker has a value.
     */
    public boolean hasValue() {
        return this.startDatePicker.getValue() != null || this.endDatePicker.getValue() != null;
    }
}
