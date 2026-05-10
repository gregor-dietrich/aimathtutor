package de.vptr.aimathtutor.component.dashboard;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.annotation.Nullable;

/**
 * Utility for generating SVG chart strings for the admin dashboard. Produces line, horizontal bar, and donut charts
 * styled with Lumo design tokens. No external dependencies.
 */
public final class ChartUtil {

    private static final String LUMO_PRIMARY = "var(--lumo-primary-color)";
    private static final String LUMO_PRIMARY_50 = "var(--lumo-primary-color-50pct)";
    private static final String LUMO_CONTRAST_10 = "var(--lumo-contrast-10pct)";
    private static final String LUMO_CONTRAST_50 = "var(--lumo-contrast-50pct)";
    private static final String LUMO_SECONDARY_TEXT = "var(--lumo-secondary-text-color)";

    private static final String[] DONUT_COLORS = { LUMO_PRIMARY, "var(--lumo-error-color)", "var(--lumo-success-color)",
            "var(--lumo-warning-color)", "#9c27b0", "#00bcd4" };

    private ChartUtil() {
    }

    /**
     * Generates an SVG line chart for time-series data.
     *
     * @param data
     *            ordered map of dates to values
     * @param title
     *            optional chart title
     * @param width
     *            viewBox width
     * @param height
     *            viewBox height
     * @return SVG string
     */
    public static String lineChart(final Map<LocalDate, Long> data, @Nullable final String title, final int width,
            final int height) {
        if (data == null || data.isEmpty()) {
            return emptySvg(width, height, "No data");
        }

        final var dates = List.copyOf(data.keySet());
        final var values = data.values().stream().mapToLong(v -> v).toArray();
        final long maxVal = Math.max(1, Arrays.stream(values).max().orElse(1));
        final var padding = new int[] { 40, 20, 20, 40 };
        final int plotW = width - padding[1] - padding[3];
        final int plotH = height - padding[0] - padding[2];
        final int steps = dates.size() - 1;

        final var sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(width).append(" ").append(height)
                .append("\" style=\"width:100%;height:100%;\">");
        sb.append("<style>.chart-text{font-family:var(--lumo-font-family);font-size:11px;fill:")
                .append(LUMO_SECONDARY_TEXT).append(";}.chart-line{fill:none;stroke:").append(LUMO_PRIMARY)
                .append(";stroke-width:2;stroke-linejoin:round;}.chart-dot{fill:").append(LUMO_PRIMARY)
                .append(";}.chart-grid{stroke:").append(LUMO_CONTRAST_10).append(";stroke-width:1;}</style>");

        if (title != null && !title.isEmpty()) {
            sb.append("<text x=\"").append(padding[3])
                    .append("\" y=\"16\" class=\"chart-text\" font-size=\"13\" font-weight=\"600\">")
                    .append(escapeXml(title)).append("</text>");
        }

        final int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            final int y = padding[0] + (plotH * i / gridLines);
            sb.append("<line x1=\"").append(padding[3]).append("\" y1=\"").append(y).append("\" x2=\"")
                    .append(width - padding[1]).append("\" y2=\"").append(y).append("\" class=\"chart-grid\"/>");
            final long tickVal = maxVal - (maxVal * i / gridLines);
            sb.append("<text x=\"").append(padding[3] - 4).append("\" y=\"").append(y + 4)
                    .append("\" class=\"chart-text\" text-anchor=\"end\">").append(tickVal).append("</text>");
        }

        final var path = new StringBuilder();
        final var gradientFill = new StringBuilder();
        for (int i = 0; i <= steps; i++) {
            final int x = padding[3] + (plotW * i / Math.max(steps, 1));
            final int y = padding[0] + plotH - (int) (plotH * values[i] / maxVal);
            if (i == 0) {
                path.append("M").append(x).append(",").append(y);
                gradientFill.append("M").append(x).append(",").append(padding[0] + plotH).append("L").append(x)
                        .append(",").append(y);
            } else {
                path.append("L").append(x).append(",").append(y);
                gradientFill.append("L").append(x).append(",").append(y);
            }
        }
        gradientFill.append("L").append(padding[3] + (plotW * steps / Math.max(steps, 1))).append(",")
                .append(padding[0] + plotH).append("Z");

        sb.append("<defs><linearGradient id=\"lineGrad\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">");
        sb.append("<stop offset=\"0%\" stop-color=\"").append(LUMO_PRIMARY).append("\" stop-opacity=\"0.2\"/>");
        sb.append("<stop offset=\"100%\" stop-color=\"").append(LUMO_PRIMARY_50).append("\" stop-opacity=\"0.02\"/>");
        sb.append("</linearGradient></defs>");

        sb.append("<path d=\"").append(gradientFill).append("\" fill=\"url(#lineGrad)\"/>");
        sb.append("<path d=\"").append(path).append("\" class=\"chart-line\"/>");

        for (int i = 0; i <= steps; i++) {
            final int x = padding[3] + (plotW * i / Math.max(steps, 1));
            final int y = padding[0] + plotH - (int) (plotH * values[i] / maxVal);
            sb.append("<circle cx=\"").append(x).append("\" cy=\"").append(y)
                    .append("\" r=\"3\" class=\"chart-dot\"/>");
        }

        final var fmt = DateTimeFormatter.ofPattern("MM/dd", Locale.US);
        for (int i = 0; i <= steps; i += Math.max(1, steps / 6)) {
            final int x = padding[3] + (plotW * i / Math.max(steps, 1));
            sb.append("<text x=\"").append(x).append("\" y=\"").append(height - 8)
                    .append("\" class=\"chart-text\" text-anchor=\"middle\">").append(dates.get(i).format(fmt))
                    .append("</text>");
        }

        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * Generates an SVG horizontal bar chart for labeled numeric data.
     *
     * @param data
     *            ordered map of labels to values
     * @param title
     *            optional chart title
     * @param width
     *            viewBox width
     * @param height
     *            viewBox height
     * @return SVG string
     */
    public static String horizontalBarChart(final Map<String, Integer> data, @Nullable final String title,
            final int width, final int height) {
        if (data == null || data.isEmpty()) {
            return emptySvg(width, height, "No data");
        }

        final var items = data.entrySet().stream().filter(e -> e.getValue() != null && e.getValue() > 0).toList();
        if (items.isEmpty()) {
            return emptySvg(width, height, "No data");
        }

        final int maxVal = items.stream().mapToInt(Map.Entry::getValue).max().orElse(1);
        final int barH = Math.max(20, Math.min(30, (height - 40) / items.size()));
        final int labelW = 140;

        final var sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(width).append(" ").append(height)
                .append("\" style=\"width:100%;height:100%;\">");
        sb.append("<style>.chart-bar-text{font-family:var(--lumo-font-family);font-size:12px;fill:")
                .append(LUMO_SECONDARY_TEXT).append(";}.chart-bar{fill:").append(LUMO_PRIMARY)
                .append(";opacity:0.8;}.chart-bar-val{font-family:var(--lumo-font-family);font-size:11px;fill:")
                .append(LUMO_CONTRAST_50).append(";}</style>");

        if (title != null && !title.isEmpty()) {
            sb.append("<text x=\"4\" y=\"16\" class=\"chart-bar-text\" font-size=\"13\" font-weight=\"600\">")
                    .append(escapeXml(title)).append("</text>");
        }

        int y = 28;
        for (final var entry : items) {
            final int barW = (int) ((double) entry.getValue() / maxVal * (width - labelW - 60));
            sb.append("<text x=\"4\" y=\"").append(y + 13).append("\" class=\"chart-bar-text\" text-anchor=\"start\">")
                    .append(escapeXml(truncateLabel(entry.getKey(), 22))).append("</text>");
            sb.append("<rect x=\"").append(labelW).append("\" y=\"").append(y).append("\" width=\"")
                    .append(Math.max(barW, 4)).append("\" height=\"").append(barH - 4)
                    .append("\" rx=\"3\" class=\"chart-bar\"/>");
            sb.append("<text x=\"").append(labelW + Math.max(barW, 4) + 6).append("\" y=\"").append(y + 13)
                    .append("\" class=\"chart-bar-val\">").append(entry.getValue()).append("</text>");
            y += barH;
        }

        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * Generates an SVG donut chart for categorical distribution data.
     *
     * @param data
     *            ordered map of category labels to values
     * @param centerLabel
     *            optional text displayed in the center (below the value)
     * @param centerValue
     *            optional value displayed in the center (large text)
     * @param size
     *            viewBox size (square)
     * @return SVG string
     */
    public static String donutChart(final Map<String, Integer> data, @Nullable final String centerLabel,
            @Nullable final String centerValue, final int size) {
        final int legendPanelW = 160;
        final int totalW = size + legendPanelW;

        if (data == null || data.isEmpty()) {
            return emptySvg(totalW, size, "No data");
        }

        final var items = data.entrySet().stream().filter(e -> e.getValue() != null && e.getValue() > 0).toList();
        if (items.isEmpty()) {
            return emptySvg(totalW, size, "No data");
        }

        final int total = items.stream().mapToInt(Map.Entry::getValue).sum();
        if (total == 0) {
            return emptySvg(totalW, size, "No data");
        }

        final int cx = size / 2;
        final int cy = size / 2;
        final int outerR = size / 2 - 10;
        final int innerR = outerR / 2;

        final var sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(totalW).append(" ").append(size)
                .append("\" style=\"width:100%;height:100%;\">");
        sb.append("<style>.donut-text{font-family:var(--lumo-font-family);font-size:10px;fill:")
                .append(LUMO_SECONDARY_TEXT)
                .append(";text-anchor:middle;}.donut-center{font-family:var(--lumo-font-family);")
                .append("font-size:18px;font-weight:700;fill:").append(LUMO_PRIMARY)
                .append(";text-anchor:middle;}.donut-legend{font-family:var(--lumo-font-family);font-size:10px;fill:")
                .append(LUMO_SECONDARY_TEXT).append(";}</style>");

        double startAngle = -90.0;
        int idx = 0;
        for (final var entry : items) {
            final double angle = 360.0 * entry.getValue() / total;
            final String color = DONUT_COLORS[idx % DONUT_COLORS.length];

            if (angle >= 359.99 || items.size() == 1) {
                sb.append("<circle cx=\"").append(cx).append("\" cy=\"").append(cy).append("\" r=\"").append(outerR)
                        .append("\" fill=\"").append(color).append("\"/>");
                sb.append("<circle cx=\"").append(cx).append("\" cy=\"").append(cy).append("\" r=\"").append(innerR)
                        .append("\" fill=\"var(--lumo-base-color)\"/>");
            } else {
                final int largeArc = angle > 180 ? 1 : 0;

                final int endX = cx + (int) (outerR * Math.cos(Math.toRadians(startAngle + angle)));
                final int endY = cy + (int) (outerR * Math.sin(Math.toRadians(startAngle + angle)));
                final int innerEndX = cx + (int) (innerR * Math.cos(Math.toRadians(startAngle + angle)));
                final int innerEndY = cy + (int) (innerR * Math.sin(Math.toRadians(startAngle + angle)));
                final int startX = cx + (int) (outerR * Math.cos(Math.toRadians(startAngle)));
                final int startY = cy + (int) (outerR * Math.sin(Math.toRadians(startAngle)));
                final int innerStartX = cx + (int) (innerR * Math.cos(Math.toRadians(startAngle)));
                final int innerStartY = cy + (int) (innerR * Math.sin(Math.toRadians(startAngle)));

                sb.append("<path d=\"M").append(startX).append(",").append(startY).append(" A").append(outerR)
                        .append(",").append(outerR).append(" 0 ").append(largeArc).append(",1 ").append(endX)
                        .append(",").append(endY).append(" L").append(innerEndX).append(",").append(innerEndY)
                        .append(" A").append(innerR).append(",").append(innerR).append(" 0 ").append(largeArc)
                        .append(",0 ").append(innerStartX).append(",").append(innerStartY).append(" Z\" fill=\"")
                        .append(color).append("\"/>");
            }

            startAngle += angle;
            idx++;
        }

        sb.append("<text x=\"").append(cx).append("\" y=\"").append(cy - 8).append("\" class=\"donut-center\">")
                .append(escapeXml(centerValue != null ? centerValue : String.valueOf(total))).append("</text>");
        sb.append("<text x=\"").append(cx).append("\" y=\"").append(cy + 12).append("\" class=\"donut-text\">")
                .append(escapeXml(centerLabel != null ? centerLabel : "")).append("</text>");

        // Legend panel to the right of the donut
        final int legendX = size + 10;
        final int itemH = 20;
        final int legendStartY = Math.max(16, cy - (items.size() * itemH) / 2);
        idx = 0;
        for (final var entry : items) {
            final String color = DONUT_COLORS[idx % DONUT_COLORS.length];
            final int pct = Math.round(100.0f * entry.getValue() / total);
            final int itemY = legendStartY + idx * itemH;
            sb.append("<rect x=\"").append(legendX).append("\" y=\"").append(itemY - 8)
                    .append("\" width=\"10\" height=\"10\" rx=\"2\" fill=\"").append(color).append("\"/>");
            sb.append("<text x=\"").append(legendX + 14).append("\" y=\"").append(itemY)
                    .append("\" class=\"donut-legend\">")
                    .append(escapeXml(truncateLabel(entry.getKey(), 14)))
                    .append(" (").append(pct).append("%)").append("</text>");
            idx++;
        }

        sb.append("</svg>");
        return sb.toString();
    }

    private static String emptySvg(final int width, final int height, final String text) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + width + " " + height
                + "\" style=\"width:100%;height:100%;\">" + "<text x=\"" + (width / 2) + "\" y=\"" + (height / 2)
                + "\" text-anchor=\"middle\" dominant-baseline=\"central\" "
                + "font-family=\"var(--lumo-font-family)\" font-size=\"14\" fill=\"var(--lumo-secondary-text-color)\">"
                + escapeXml(text) + "</text></svg>";
    }

    /**
     * Escapes XML special characters in user-provided strings.
     *
     * @param s
     *            input string
     * @return escaped string safe for inclusion in XML/SVG content
     */
    static String escapeXml(final String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String truncateLabel(final String label, final int maxLen) {
        if (label == null || label.length() <= maxLen) {
            return label != null ? label : "";
        }
        return label.substring(0, maxLen - 1) + "…";
    }
}
