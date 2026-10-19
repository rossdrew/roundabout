package com.rox.ui.data;

import com.rox.ui.Box;
import com.rox.ui.Theme;
import com.rox.ui.Ui;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Point;

import java.util.function.DoubleFunction;

/** Line graphs over {@link RingSeries}. */
public final class Graphs {
    private Graphs() {
    }

    /** Compact line with a soft fill, scaled to [min, max]. Series fills the box right-aligned. */
    public static void sparkline(Ui ui, Box box, RingSeries series, float min, float max, int colour) {
        if (series.size() < 2) {
            return;
        }
        float step = box.w / (series.capacity() - 1);
        float x0 = box.right() - (series.size() - 1) * step;
        float range = Math.max(1e-6f, max - min);

        // at most one point per pixel; the fill is a vertex-coloured triangle strip so nothing
        // needs tessellating on the CPU
        int stride = Math.max(1, (int) Math.ceil(series.size() / Math.max(1f, box.w)));
        int points = (series.size() - 1) / stride + 1;
        float[] xy = new float[points * 2];
        Point[] strip = new Point[points * 2];
        for (int p = 0; p < points; p++) {
            int i = p == points - 1 ? series.size() - 1 : p * stride;
            float x = x0 + i * step;
            float v = series.get(i);
            for (int j = i + 1; j < Math.min(series.size(), i + stride); j++) {
                v = Math.max(v, series.get(j)); // a one-sample spike must survive downsampling
            }
            float f = (Math.clamp(v, min, max) - min) / range;
            float y = box.bottom() - f * box.h;
            xy[p * 2] = x;
            xy[p * 2 + 1] = y;
            strip[p * 2] = new Point(x, y);
            strip[p * 2 + 1] = new Point(x, box.bottom());
        }
        try (Shader gradient = Shader.makeLinearGradient(0, box.y, 0, box.bottom(),
                new int[]{Theme.withAlpha(colour, 0.28f), Theme.withAlpha(colour, 0f)});
             Paint fill = new Paint().setShader(gradient)) {
            ui.canvas.drawTriangleStrip(strip, null, fill);
        }
        ui.canvas.drawPolygon(xy, ui.strokePaint(colour, 1.5f));
        ui.strokePaint(colour, 1);
    }

    /** Full graph of byte values (hex axis and readout) - see the general overload. */
    public static Box lineGraph(Ui ui, Box area, RingSeries series, float min, float max, int colour,
                                float sampleSeconds) {
        return lineGraph(ui, area, series, min, max, colour, sampleSeconds,
                v -> String.format("%02X", (int) v), v -> String.format("$%02X  %d", (int) v, (int) v));
    }

    /**
     * Full graph with grid lines, value axis, latest-value marker and hover readout.
     *
     * @param axisLabel formats the value axis's tick labels
     * @param valueLabel formats the hovered sample in the tooltip
     * @return the plot area, for drawing reference lines with {@link #guide}
     */
    public static Box lineGraph(Ui ui, Box area, RingSeries series, float min, float max, int colour,
                                float sampleSeconds, DoubleFunction<String> axisLabel,
                                DoubleFunction<String> valueLabel) {
        Theme t = ui.theme;
        Box box = area.copy();
        Box axis = box.cutLeft(34);
        box.cutRight(6);
        box = box.inset(0, 6);

        for (int i = 0; i <= 4; i++) {
            float v = min + (max - min) * i / 4;
            float y = box.bottom() - box.h * i / 4;
            ui.line(box.x, y, box.right(), y, i == 0 ? t.border : Theme.withAlpha(t.border, 0.6f));
            ui.textRight(new Box(axis.x, y - 7, axis.w - 6, 14), axisLabel.apply(v), t.uiSmall, t.textFaint);
        }
        sparkline(ui, box, series, min, max, colour);

        if (series.size() > 0) {
            float y = box.bottom() - (Math.clamp(series.latest(), min, max) - min) / (max - min) * box.h;
            ui.canvas.drawCircle(box.right(), y, 3.5f, ui.paint(colour));
        }

        var in = ui.input;
        if (box.contains(in.mouseX, in.mouseY) && series.size() > 1) {
            float step = box.w / (series.capacity() - 1);
            int fromEnd = Math.round((box.right() - in.mouseX) / step);
            int i = series.size() - 1 - fromEnd;
            if (i >= 0) {
                float x = box.right() - fromEnd * step;
                float v = series.get(i);
                float y = box.bottom() - (Math.clamp(v, min, max) - min) / (max - min) * box.h;
                ui.line(x, box.y, x, box.bottom(), Theme.withAlpha(t.text, 0.25f));
                ui.canvas.drawCircle(x, y, 4, ui.paint(t.background));
                ui.canvas.drawCircle(x, y, 3, ui.paint(colour));
                ui.setTooltip(String.format("%s%n-%.2fs", valueLabel.apply(v), fromEnd * sampleSeconds));
            }
        }
        return box;
    }

    /** A horizontal reference line at {@code value} across a {@link #lineGraph}'s plot area. */
    public static void guide(Ui ui, Box plot, float value, float min, float max, int colour) {
        float y = plot.bottom() - (Math.clamp(value, min, max) - min) / (max - min) * plot.h;
        ui.line(plot.x, y, plot.right(), y, Theme.withAlpha(colour, 0.5f));
    }
}
