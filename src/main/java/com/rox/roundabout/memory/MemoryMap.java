package com.rox.roundabout.memory;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.Ui;

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
    private final AddressFormat format;

    /** A map of {@code regions}, labelled with {@code format}. */
    public MemoryMap(List<MemoryRegion> regions, AddressFormat format) {
        this.regions = regions;
        this.format = format;
    }

    /** Returns the address the user clicked/dragged to, or -1. */
    public int draw(Ui ui, Box containerBox, MemoryHeat heat, int viewFirst, int viewLast) {
        Theme t = ui.theme;
        float[] bandY = layout(containerBox);
        int target = -1;

        Box heatColumn = new Box(containerBox.right() - HEAT_WIDTH, containerBox.y, HEAT_WIDTH, containerBox.h);
        for (int regionIndex = 0; regionIndex < regions.size(); regionIndex++) {
            drawMemoryRegion(ui, containerBox, heat, regionIndex, bandY, t, heatColumn);
        }

        // viewport bracket
        float y0 = yOf(bandY, viewFirst), y1 = Math.max(y0 + 3, yOf(bandY, viewLast));
        Box view = new Box(containerBox.x + 1, y0, containerBox.w - 2, y1 - y0);
        ui.fillRound(view, 3, Theme.withAlpha(t.accent, 0.10f));
        ui.strokeRound(view, 3, Theme.withAlpha(t.accent, 0.8f));

        if (ui.drag("memmap", containerBox)) {
            target = addressAt(bandY, Math.clamp(ui.input.mouseY, containerBox.y, containerBox.bottom() - 1));
        } else if (containerBox.contains(ui.input.mouseX, ui.input.mouseY) && ui.hovered(containerBox)) {
            int a = addressAt(bandY, ui.input.mouseY);
            MemoryRegion r = MemoryRegion.find(regions, a);
            ui.setTooltip(String.format("%s%n%s  %s-%s", format.address(a), r.name(),
                    format.address(r.start()), format.address(r.end())));
        }
        return target;
    }

    private void drawMemoryRegion(Ui ui, Box containerBox, MemoryHeat heat, int regionIndex, float[] bandY, Theme t, Box heatColumn) {
        final MemoryRegion region = regions.get(regionIndex);
        final Box memoryBand = new Box(containerBox.x, bandY[regionIndex], containerBox.w, bandY[regionIndex + 1] - bandY[regionIndex]);
        boolean hover = ui.hovered(memoryBand);
        ui.fill(memoryBand, Theme.withAlpha(region.colour(), hover ? 0.10f : 0.05f));
        ui.fill(new Box(memoryBand.x, memoryBand.y, 3, memoryBand.h), Theme.withAlpha(region.colour(), 0.8f));
        ui.line(memoryBand.x, memoryBand.y + 0.5f, memoryBand.right(), memoryBand.y + 0.5f, t.panel);

        final Box bandLabel = new Box(memoryBand.x + 10, memoryBand.y + 3, memoryBand.w - HEAT_WIDTH - 14, 15);
        ui.text(bandLabel, region.name(), t.uiSmall, hover ? t.text : Theme.mix(t.textDim, region.colour(), 0.35f));
        if (memoryBand.h >= 34) {
            final Box bandRangeLabel = new Box(bandLabel.x, bandLabel.bottom(), bandLabel.w, 14);
            ui.text(bandRangeLabel, format.address(region.start()), t.uiSmall, t.textFaint);
        }

        // peak heat per pixel row
        int rowsPx = Math.max(1, (int) memoryBand.h);
        float perPx = (float) region.size() / rowsPx;
        for (int py = 0; py < rowsPx; py++) {
            int a0 = region.start() + (int) (py * perPx);
            int a1 = Math.min(region.end(), region.start() + (int) ((py + 1) * perPx) - 1);
            float peak = 0;
            for (int a = a0; a <= Math.max(a0, a1); a++) {
                peak = Math.max(peak, heat.heat(a));
            }
            if (peak > 0.02f) {
                float w = (HEAT_WIDTH - 8) * (0.25f + 0.75f * peak);
                ui.fill(new Box(heatColumn.right() - 4 - w, memoryBand.y + py, w, 1), t.heat(Math.min(1, peak * 1.2f)));
            }
        }
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
