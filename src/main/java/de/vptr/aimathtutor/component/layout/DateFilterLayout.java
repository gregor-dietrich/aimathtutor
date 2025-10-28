package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.vptr.aimathtutor.component.button.FilterButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Small composite layout containing two date pickers and a filter button.
 * Parent views can read the selected dates and trigger the filter action via
 * the exposed button.
 */
public class DateFilterLayout extends HorizontalLayout {

    private static final String DEFAULT_TOOLTIP = "Filter by Date";
    private static final String DEFAULT_WIDTH = "150px";

    private final Button button;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;

    public DateFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final String tooltipText, final String fieldWidth) {
        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.startDatePicker = new DatePicker("Start Date");
        this.startDatePicker.setWidth(fieldWidth);

        this.endDatePicker = new DatePicker("End Date");
        this.endDatePicker.setWidth(fieldWidth);

        this.button = new FilterButton(filterAction, tooltipText);

        this.add(this.startDatePicker, this.endDatePicker, this.button);
    }

    /**
     * Construct a date filter layout with custom tooltip and field size.
     *
     * @param filterAction listener invoked when the filter button is pressed
     * @param tooltipText  tooltip for the filter button
     */
    public DateFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final String tooltipText) {
        this(filterAction, tooltipText, DEFAULT_WIDTH);
    }

    public DateFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction) {
        this(filterAction, DEFAULT_TOOLTIP);
    }

    /**
     * Get the internal filter button so callers can attach listeners or
     * programmatically trigger the filter action.
     *
     * @return filter button contained in this layout
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "This layout intentionally exposes internal components for composing into larger UIs")
    public Button getButton() {
        return this.button;
    }

    /**
     * Get the start date picker component. The returned instance is the live
     * component used by the layout.
     *
     * @return start date picker
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose DatePicker so parent views can wire listeners and read values")
    public DatePicker getStartDatePicker() {
        return this.startDatePicker;
    }

    /**
     * Get the end date picker component. The returned instance is the live
     * component used by the layout.
     *
     * @return end date picker
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose DatePicker so parent views can wire listeners and read values")
    public DatePicker getEndDatePicker() {
        return this.endDatePicker;
    }
}
