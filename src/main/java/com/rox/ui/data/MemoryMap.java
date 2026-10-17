package com.rox.ui.data;

import com.rox.ui.Box;
import com.rox.ui.Theme;
import com.rox.ui.Ui;

import java.util.List;

/**
 * Vertical overview of the whole address space. Each region gets a band (height ~ sqrt(size), so
 * small hot regions like zero page stay visible) showing peak write heat per pixel row.
 * Click or drag to navigate.
 */
public final class MemoryMap {
    private static final float MIN_BAND = 22;
    private static final float HEAT_WIDTH = 46;

    private final List<MemoryRegion> regions;

    public MemoryMap(List<MemoryRegion> regions) {
        this.regions = regions;
    }

    /** Returns the address the user clicked/dragged to, or -1. */
    public int draw(Ui ui, Box box, MemoryHeat heat, int viewFirst, int viewLast) {
        Theme t = ui.theme;
        float[] bandY = layout(box);
        int target = -1;

        Box heatColumn = new Box(box.right() - HEAT_WIDTH, box.y, HEAT_WIDTH, box.h);
        for (int i = 0; i < regions.size(); i++) {
            MemoryRegion r = regions.get(i);
            Box band = new Box(box.x, bandY[i], box.w, bandY[i + 1] - bandY[i]);
            boolean hover = ui.hovered(band);
            ui.fill(band, Theme.withAlpha(r.colour(), hover ? 0.10f : 0.05f));
            ui.fill(new Box(band.x, band.y, 3, band.h), Theme.withAlpha(r.colour(), 0.8f));
            ui.line(band.x, band.y + 0.5f, band.right(), band.y + 0.5f, t.panel);

            Box label = new Box(band.x + 10, band.y + 3, band.w - HEAT_WIDTH - 14, 15);
            ui.text(label, r.name(), t.uiSmall, hover ? t.text : Theme.mix(t.textDim, r.colour(), 0.35f));
            if (band.h >= 34) {
                Box range = new Box(label.x, label.bottom(), label.w, 14);
                ui.text(range, String.format("$%04X", r.start()), t.uiSmall, t.textFaint);
            }

            // peak heat per pixel row
            int rowsPx = Math.max(1, (int) band.h);
            float perPx = (float) r.size() / rowsPx;
            for (int py = 0; py < rowsPx; py++) {
                int a0 = r.start() + (int) (py * perPx);
                int a1 = Math.min(r.end(), r.start() + (int) ((py + 1) * perPx) - 1);
                float peak = 0;
                for (int a = a0; a <= Math.max(a0, a1); a++) {
                    peak = Math.max(peak, heat.heat(a));
                }
                if (peak > 0.02f) {
                    float w = (HEAT_WIDTH - 8) * (0.25f + 0.75f * peak);
                    ui.fill(new Box(heatColumn.right() - 4 - w, band.y + py, w, 1), t.heat(Math.min(1, peak * 1.2f)));
                }
            }
        }

        // viewport bracket
        float y0 = yOf(bandY, viewFirst), y1 = Math.max(y0 + 3, yOf(bandY, viewLast));
        Box view = new Box(box.x + 1, y0, box.w - 2, y1 - y0);
        ui.fillRound(view, 3, Theme.withAlpha(t.accent, 0.10f));
        ui.strokeRound(view, 3, Theme.withAlpha(t.accent, 0.8f));

        if (ui.drag("memmap", box)) {
            target = addressAt(bandY, Math.clamp(ui.input.mouseY, box.y, box.bottom() - 1));
        } else if (box.contains(ui.input.mouseX, ui.input.mouseY) && ui.hovered(box)) {
            int a = addressAt(bandY, ui.input.mouseY);
            MemoryRegion r = MemoryRegion.find(regions, a);
            ui.setTooltip(String.format("$%04X%n%s  $%04X-$%04X", a, r.name(), r.start(), r.end()));
        }
        return target;
    }

    private float[] layout(Box box) {
        float[] weights = new float[regions.size()];
        float total = 0;
        for (int i = 0; i < weights.length; i++) {
            weights[i] = (float) Math.sqrt(regions.get(i).size());
            total += weights[i];
        }
        float available = box.h - MIN_BAND * weights.length;
        float[] y = new float[weights.length + 1];
        y[0] = box.y;
        for (int i = 0; i < weights.length; i++) {
            y[i + 1] = y[i] + MIN_BAND + Math.max(0, available) * weights[i] / total;
        }
        return y;
    }

    private float yOf(float[] bandY, int address) {
        for (int i = 0; i < regions.size(); i++) {
            MemoryRegion r = regions.get(i);
            if (r.contains(address)) {
                return bandY[i] + (bandY[i + 1] - bandY[i]) * (address - r.start()) / r.size();
            }
        }
        return bandY[bandY.length - 1];
    }

    private int addressAt(float[] bandY, float y) {
        for (int i = 0; i < regions.size(); i++) {
            if (y < bandY[i + 1]) {
                MemoryRegion r = regions.get(i);
                float f = (y - bandY[i]) / (bandY[i + 1] - bandY[i]);
                return Math.clamp(r.start() + (int) (f * r.size()), r.start(), r.end());
            }
        }
        return regions.getLast().end();
    }
}
