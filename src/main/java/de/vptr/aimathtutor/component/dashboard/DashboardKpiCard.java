package de.vptr.aimathtutor.component.dashboard;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.vptr.aimathtutor.component.GlassAccentBar;
import de.vptr.aimathtutor.util.AppStyles;

/**
 * Premium KPI card with glass-morphism styling, gradient accent bar, hover elevation, and trend indicator. Looks like
 * it belongs in a VC-funded analytics platform.
 */
public class DashboardKpiCard extends VerticalLayout {

    private final Span valueLabel;
    private final Span trendLabel;

    /**
     * Creates a dashboard KPI card.
     *
     * @param label
     *            display label (e.g. "Total Sessions")
     * @param initialValue
     *            initial placeholder while data loads
     */
    public DashboardKpiCard(final String label, final String initialValue) {
        this.setPadding(false);
        this.setSpacing(false);
        this.setWidthFull();

        this.addClassNames(AppStyles.GLASS_CARD, AppStyles.LIFT_ON_HOVER);
        this.getStyle().set("overflow", "hidden").set("cursor", "default");

        final var accentBar = new GlassAccentBar();

        final var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("padding", "14px 16px 12px");

        final var titleLabel = new Span(label);
        titleLabel.getStyle().set("font-size", "11px").set("color", "var(--vaadin-text-color-secondary)")
                .set("text-transform", "uppercase").set("font-weight", "700").set("letter-spacing", "0.8px")
                .set("margin-bottom", "6px");

        this.valueLabel = new Span(initialValue);
        this.valueLabel.getStyle().set("font-size", "30px").set("font-weight", "800")
                .set("color", AppStyles.ACCENT_TEXT).set("line-height", "1.2").set("margin-bottom", "2px");

        this.trendLabel = new Span();
        this.trendLabel.getStyle().set("font-size", "12px").set("font-weight", "500");

        content.add(titleLabel, this.valueLabel, this.trendLabel);
        this.add(accentBar, content);
    }

    /**
     * Updates the displayed value.
     *
     * @param value
     *            the new value string
     */
    public void setValue(final String value) {
        this.valueLabel.setText(value);
    }

    /**
     * Updates the trend indicator with the percentage change.
     *
     * @param percentChange
     *            the percentage change (positive, negative, or zero)
     */
    public void setTrend(final double percentChange) {
        if (Double.isNaN(percentChange)) {
            this.trendLabel.setText("—");
            this.trendLabel.getStyle().set("color", "var(--vaadin-text-color-secondary)");
            return;
        }
        final String formatted = String.format("%+.1f%%", percentChange);
        final String arrow = percentChange > 0 ? "▲" : percentChange < 0 ? "▼" : "—";
        final String color;
        if (percentChange > 0) {
            color = AppStyles.SUCCESS;
        } else if (percentChange < 0) {
            color = AppStyles.ERROR;
        } else {
            color = "var(--vaadin-text-color-secondary)";
        }
        this.trendLabel.setText(arrow + " " + formatted + " vs last 7d");
        this.trendLabel.getStyle().set("color", color);
    }
}
