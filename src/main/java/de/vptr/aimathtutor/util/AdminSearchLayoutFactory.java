package de.vptr.aimathtutor.util;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.aimathtutor.component.button.RefreshButton;
import de.vptr.aimathtutor.component.layout.SearchFilterBar;
import de.vptr.aimathtutor.component.layout.SearchLayout;

/**
 * Factory for creating admin search layouts with consistent structure.
 */
public final class AdminSearchLayoutFactory {

    private AdminSearchLayoutFactory() {
    }

    /**
     * Mutable holder for components created by the factory.
     */
    public static final class Components {
        /** Search text field. */
        public TextField searchField;
        /** Start date picker. */
        public DatePicker startDatePicker;
        /** End date picker. */
        public DatePicker endDatePicker;
        /** Button layout. */
        public HorizontalLayout buttonLayout;
    }

    /**
     * Creates a search layout with filter bar and button layout.
     *
     * @param onClear
     *            action when search is cleared
     * @param onSearch
     *            action when search is triggered
     * @param placeholder
     *            search field placeholder text
     * @param buttonLabel
     *            search button label
     * @param onDateFilter
     *            action when date filter is applied
     * @param onReset
     *            action when filters are reset
     * @param onRefresh
     *            action when refresh button is clicked
     * @param out
     *            mutable holder to receive created components
     * @return the search layout
     */
    public static SearchLayout create(final Runnable onClear, final Runnable onSearch, final String placeholder,
            final String buttonLabel, final Runnable onDateFilter, final Runnable onReset, final Runnable onRefresh,
            final Components out) {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().isBlank()) {
                onClear.run();
            }
        }, ignored -> onSearch.run(), placeholder, buttonLabel);
        out.searchField = searchLayout.getTextfield();
        final var filterBar = new SearchFilterBar(searchLayout, onDateFilter, onReset);
        out.startDatePicker = filterBar.getStartDatePicker();
        out.endDatePicker = filterBar.getEndDatePicker();
        final var buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        buttons.add(new RefreshButton(ignored -> onRefresh.run()));
        out.buttonLayout = buttons;
        return searchLayout;
    }
}
