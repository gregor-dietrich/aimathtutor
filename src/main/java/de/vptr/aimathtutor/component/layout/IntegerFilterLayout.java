package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import de.vptr.aimathtutor.component.button.FilterButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class IntegerFilterLayout extends HorizontalLayout {

    private final static String DEFAULT_LABEL = "Filter";
    private final static String DEFAULT_TOOLTIP = "Filter";
    private final static String DEFAULT_WIDTH = "150px";

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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose button intentionally for parent views to trigger filter actions")
    public Button getButton() {
        return this.button;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose IntegerField intentionally for parent views to read/set values")
    public IntegerField getIntegerField() {
        return this.integerField;
    }
}
