package de.vptr.aimathtutor.component.layout;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.aimathtutor.component.button.SearchButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Composite layout containing a search text field and a search button. The
 * component wires default Enter key handling and exposes internal
 * components for parent views to reuse.
 */
public class SearchLayout extends HorizontalLayout {

    private static final String DEFAULT_LABEL = "Search";
    private static final String DEFAULT_PLACEHOLDER = "Search...";
    private static final String DEFAULT_TOOLTIP = "Search";
    private static final String DEFAULT_WIDTH = "300px";

    private Button button;
    private final TextField textField;

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText, final String fieldLabel, final String fieldWidth) {
        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.textField = new TextField(fieldLabel);
        this.textField.setClearButtonVisible(true);
        this.textField.setPlaceholder(placeholderText);
        this.textField.setWidth(fieldWidth);
        this.textField.addValueChangeListener(listener);

        this.textField.addKeyPressListener(Key.ENTER, event -> {
            if (searchAction != null) {
                searchAction.onComponentEvent(new ClickEvent<>(this.button));
            }
        });

        this.button = new SearchButton(searchAction, tooltipText);

        this.add(this.textField, this.button);
    }

    /**
     * Create a search layout combining a text field and a search button.
     *
     * @param listener        value change listener for the text field
     * @param searchAction    click listener for the search button
     * @param placeholderText placeholder text shown in the text field
     * @param tooltipText     tooltip text for the button
     * @param fieldLabel      label for the text field
     */

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText, final String fieldLabel) {
        this(listener, searchAction, placeholderText, tooltipText, fieldLabel, DEFAULT_WIDTH);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText) {
        this(listener, searchAction, placeholderText, tooltipText, DEFAULT_LABEL);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText) {
        this(listener, searchAction, placeholderText, DEFAULT_TOOLTIP);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction) {
        this(listener, searchAction, DEFAULT_PLACEHOLDER);
    }

    /**
     * Return the internal search button. Useful to trigger a search from the
     * parent view or to attach additional listeners.
     *
     * @return the search button
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose button intentionally so parent views can trigger searches or reuse it")
    public Button getButton() {
        return this.button;
    }

    /**
     * Return the text field used for entering the search term. Parent views
     * can read or set the value and attach listeners as needed.
     *
     * @return the search text field
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "Expose textField intentionally so parent views can read/set value and attach listeners")
    public TextField getTextfield() {
        return this.textField;
    }
}
