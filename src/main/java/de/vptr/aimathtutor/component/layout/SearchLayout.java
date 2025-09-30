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

public class SearchLayout extends HorizontalLayout {

    private final static String DEFAULT_LABEL = "Search";
    private final static String DEFAULT_PLACEHOLDER = "Search...";
    private final static String DEFAULT_TOOLTIP = "Search";
    private final static String DEFAULT_WIDTH = "300px";

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

    public Button getButton() {
        return this.button;
    }

    public TextField getTextfield() {
        return this.textField;
    }
}
