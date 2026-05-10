package de.vptr.aimathtutor.component.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

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
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        final String bgGradient = "linear-gradient(135deg, var(--lumo-base-color),"
                + " color-mix(in srgb, var(--lumo-base-color) 95%, var(--lumo-primary-color-10pct)))";
        getStyle().set("background", bgGradient).set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)")
                .set("transition", "all 0.25s cubic-bezier(0.4, 0, 0.2, 1)").set("position", "relative")
                .set("overflow", "hidden").set("cursor", "default");

        final var accentBar = new Div();
        final String accentGradient = "linear-gradient(90deg, var(--lumo-primary-color),"
                + " var(--lumo-primary-color-50pct), var(--lumo-primary-color-10pct))";
        accentBar.getStyle().set("height", "3px").set("width", "100%").set("background", accentGradient)
                .set("flex-shrink", "0");

        final var content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(false);
        content.getStyle().set("padding", "14px 16px 12px");

        final var titleLabel = new Span(label);
        titleLabel.getStyle().set("font-size", "11px").set("color", "var(--lumo-secondary-text-color)")
                .set("text-transform", "uppercase").set("font-weight", "700").set("letter-spacing", "0.8px")
                .set("margin-bottom", "6px");

        this.valueLabel = new Span(initialValue);
        this.valueLabel.getStyle().set("font-size", "30px").set("font-weight", "800")
                .set("color", "var(--lumo-primary-text-color)").set("line-height", "1.2").set("margin-bottom", "2px");

        this.trendLabel = new Span();
        this.trendLabel.getStyle().set("font-size", "12px").set("font-weight", "500");

        content.add(titleLabel, this.valueLabel, this.trendLabel);
        add(accentBar, content);
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
            this.trendLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            return;
        }
        final String formatted = String.format("%+.1f%%", percentChange);
        final String arrow = percentChange > 0 ? "▲" : percentChange < 0 ? "▼" : "—";
        final String color;
        if (percentChange > 0) {
            color = "var(--lumo-success-color)";
        } else if (percentChange < 0) {
            color = "var(--lumo-error-color)";
        } else {
            color = "var(--lumo-secondary-text-color)";
        }
        this.trendLabel.setText(arrow + " " + formatted + " vs last 7d");
        this.trendLabel.getStyle().set("color", color);
    }
}
