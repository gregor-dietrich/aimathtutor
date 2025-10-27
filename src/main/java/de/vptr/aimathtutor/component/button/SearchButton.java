package de.vptr.aimathtutor.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/**
 * Button that triggers a search action. Displays a search icon and calls the
 * provided click listener when pressed.
 */
public class SearchButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Search";

    public SearchButton(final ComponentEventListener<ClickEvent<Button>> searchAction, final String tooltipText,
            final ButtonVariant... variants) {
        super("", searchAction);
        this.setIcon(LineAwesomeIcon.SEARCH_SOLID.create());
        this.setTooltipText(tooltipText);
        this.addThemeVariants(variants);
    }

    public SearchButton(final ComponentEventListener<ClickEvent<Button>> searchAction,
            final ButtonVariant... variants) {
        this(searchAction, DEFAULT_TOOLTIP, variants);
    }

    public SearchButton(final ComponentEventListener<ClickEvent<Button>> searchAction) {
        this(searchAction, DEFAULT_TOOLTIP, ButtonVariant.LUMO_PRIMARY);
    }
}
