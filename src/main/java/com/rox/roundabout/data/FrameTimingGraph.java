package com.rox.roundabout.data;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.Ui;

/**
 * One {@link FrameTimes} stream as a row: summary numbers beside a graph of every recent frame interval,
 * like a watch's history graph, with reference lines at the frame budget and at "late" (1.5x budget,
 * where a display refresh shows the previous frame again).
 */
public final class FrameTimingGraph {
    /** The graph's top; longer intervals are drawn at the top edge (the tooltip and max still show them exactly). */
    public static final float RANGE_MS = 50;

    private FrameTimingGraph() {
    }

    /** Draws {@code times} against its own budget; each interval is plotted as one budget-long sample. */
    public static void draw(Ui ui, Box area, FrameTimes times) {
        Theme t = ui.theme;
        RingSeries s = times.intervalsMs();
        float budgetMs = times.budgetMs();
        float sampleSeconds = budgetMs / 1000;
        float lateMs = budgetMs * 1.5f;
        Box row = area.copy();

        Box summary = row.cutLeft(140);
        row.cutLeft(8);
        ui.text(summary.cutTop(20), times.name(), t.uiBold, t.text);
        Box budgetRow = summary.cutTop(18);
        ui.text(budgetRow, "budget", t.uiSmall, t.textDim);
        ui.textRight(budgetRow, String.format("%.1f ms", budgetMs), t.mono, t.textDim);
        stat(ui, summary.cutTop(18), "mean", s.average(), budgetMs, lateMs);
        stat(ui, summary.cutTop(18), "p99", s.percentile(0.99f), budgetMs, lateMs);
        stat(ui, summary.cutTop(18), "max", s.max(), budgetMs, lateMs);
        int late = s.countAbove(lateMs);
        Box lateRow = summary.cutTop(18);
        ui.text(lateRow, "late", t.uiSmall, t.textDim);
        ui.textRight(lateRow, Integer.toString(late), t.mono, late > 0 ? t.hot : t.good);

        int colour = late > 0 ? t.warn : t.accent;
        Box plot = Graphs.lineGraph(ui, row, s, 0, RANGE_MS, colour, sampleSeconds,
                v -> String.format("%.0f", v), v -> String.format("%.1f ms", v));
        Graphs.guide(ui, plot, budgetMs, 0, RANGE_MS, t.good);
        Graphs.guide(ui, plot, lateMs, 0, RANGE_MS, t.hot);
    }

    private static void stat(Ui ui, Box box, String label, float ms, float budgetMs, float lateMs) {
        Theme t = ui.theme;
        int colour = ms > lateMs ? t.hot : ms > budgetMs * 1.1f ? t.warn : t.good;
        ui.text(box, label, t.uiSmall, t.textDim);
        ui.textRight(box, String.format("%.1f ms", ms), t.mono, colour);
    }
}
