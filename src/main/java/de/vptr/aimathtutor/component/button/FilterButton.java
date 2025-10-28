package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button that triggers a filter action. Displays a filter icon and applies
 * any provided theme variants. Parent layouts can use this button to start
 * search or filter operations.
 */
public class FilterButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Search";

    public FilterButton(final ComponentEventListener<ClickEvent<Button>> filterAction, final String tooltipText,
            final ButtonVariant... variants) {
        super("", filterAction);
        this.setIcon(LineAwesomeIcon.FILTER_SOLID.create());
        this.setTooltipText(tooltipText);
        this.addThemeVariants(variants);
    }

    public FilterButton(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final ButtonVariant... variants) {
        this(filterAction, DEFAULT_TOOLTIP, variants);
    }

    public FilterButton(final ComponentEventListener<ClickEvent<Button>> filterAction) {
        this(filterAction, DEFAULT_TOOLTIP, ButtonVariant.LUMO_PRIMARY);
    }
}
