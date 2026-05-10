package de.vptr.aimathtutor.component.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.vptr.aimathtutor.component.GlassAccentBar;

/**
 * Premium chart card with glass-morphism styling, matching the DashboardKpiCard design language. Wraps SVG chart
 * content with a title header.
 */
public class ChartCard extends VerticalLayout {

    private final Div chartContainer;

    /**
     * Creates a chart card with the given title.
     *
     * @param title
     *            the chart title (may be null or empty to omit)
     */
    public ChartCard(final String title) {
        this.setPadding(false);
        this.setSpacing(false);
        this.setWidthFull();
        this.setHeightFull();

        final String bgGradient = "linear-gradient(135deg, var(--lumo-base-color),"
                + " color-mix(in srgb, var(--lumo-base-color) 95%, var(--lumo-primary-color-10pct)))";
        this.getStyle().set("background", bgGradient).set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)")
                .set("transition", "all 0.25s cubic-bezier(0.4, 0, 0.2, 1)").set("position", "relative")
                .set("overflow", "hidden");

        final var accentBar = new Div();
        final String accentGradient = "linear-gradient(90deg, var(--lumo-primary-color),"
                + " var(--lumo-primary-color-50pct), var(--lumo-primary-color-10pct))";
        accentBar.getStyle().set("height", "3px").set("width", "100%").set("background", accentGradient)
                .set("flex-shrink", "0");

        if (title != null && !title.isEmpty()) {
            final var titleSpan = new Span(title);
            titleSpan.getStyle().set("font-size", "13px").set("font-weight", "700")
                    .set("color", "var(--lumo-primary-text-color)").set("letter-spacing", "0.3px")
                    .set("padding", "12px 16px 0").set("display", "block");
            this.add(accentBar, titleSpan);
        } else {
            this.add(accentBar);
        }

        this.chartContainer = new Div();
        this.chartContainer.setWidthFull();
        this.chartContainer.setHeightFull();
        this.chartContainer.getStyle().set("min-height", "200px").set("padding", "8px 8px 4px");
        this.add(this.chartContainer);
        GlassAccentBar.addHoverEffect(this);
    }

    /**
     * Replaces the chart content with the given SVG string.
     *
     * @param svgContent
     *            raw SVG markup or null/empty to show "No data available"
     */
    public void setChartContent(final String svgContent) {
        this.chartContainer.removeAll();
        if (svgContent == null || svgContent.isEmpty()) {
            this.chartContainer.add(new Span("No data available"));
            return;
        }
        this.chartContainer.getElement().setProperty("innerHTML", svgContent);
    }
}
