package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;

/**
 * Adds a date filter and reset button to a SearchLayout, eliminating
 * duplication across admin views that share this filter pattern.
 */
public final class SearchFilterBar {

    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;
    private final Button resetButton;

    /**
     * Creates a date filter and reset button, attaching them to the given
     * SearchLayout.
     *
     * @param searchLayout   the SearchLayout to augment
     * @param onDateFilter   callback invoked when date range changes
     * @param onResetFilters callback invoked when reset button is clicked
     */
    public SearchFilterBar(final SearchLayout searchLayout,
            final Runnable onDateFilter, final Runnable onResetFilters) {
        final var dateFilterLayout = new DateFilterLayout(ignored -> onDateFilter.run());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        this.resetButton = new Button("Reset Filters", ignored -> onResetFilters.run());
        this.resetButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        searchLayout.add(dateFilterLayout, this.resetButton);
    }

    public DatePicker getStartDatePicker() {
        return this.startDatePicker;
    }

    public DatePicker getEndDatePicker() {
        return this.endDatePicker;
    }

    public Button getResetButton() {
        return this.resetButton;
    }
}
