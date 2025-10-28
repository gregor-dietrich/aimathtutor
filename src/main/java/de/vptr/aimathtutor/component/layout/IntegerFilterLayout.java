package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import de.vptr.aimathtutor.component.button.FilterButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Composite layout containing an integer input field and a filter button.
 * Parent views can read the entered integer and trigger the filter using the
 * exposed button. Designed for compact numeric filters in list views.
 */
public class IntegerFilterLayout extends HorizontalLayout {

    private static final String DEFAULT_LABEL = "Filter";
    private static final String DEFAULT_TOOLTIP = "Filter";
    private static final String DEFAULT_WIDTH = "150px";

    private final Button button;
    private final IntegerField integerField;

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final String tooltipText, final String fieldLabel, final String fieldWidth) {
        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.integerField = new IntegerField(fieldLabel);
        this.integerField.setWidth(fieldWidth);

        this.button = new FilterButton(filterAction, tooltipText);

        this.add(this.integerField, this.button);
    }

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final String tooltipText, final String fieldLabel) {
        this(filterAction, tooltipText, fieldLabel, DEFAULT_WIDTH);
    }

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final String tooltipText) {
        this(filterAction, tooltipText, DEFAULT_LABEL);
    }

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction) {
        this(filterAction, DEFAULT_TOOLTIP);
    }

    /**
     * Return the internal filter button so callers can attach listeners or
     * programmatically trigger the filter action.
     *
     * @return filter button contained in this layout
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose button intentionally for parent views to trigger filter actions")
    public Button getButton() {
        return this.button;
    }

    /**
     * Return the integer field used to enter the filter value. Parent views
     * can read or set the value and attach listeners as needed.
     *
     * @return the integer input field
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose IntegerField intentionally for parent views to read/set values")
    public IntegerField getIntegerField() {
        return this.integerField;
    }
}
