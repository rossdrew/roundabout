package com.rox.roundabout.memory;

import com.rox.roundabout.Box;
import com.rox.roundabout.Dock;
import com.rox.roundabout.Theme;
import com.rox.roundabout.Ui;
import com.rox.roundabout.chart.Graphs;
import com.rox.roundabout.chart.RingSeries;
import com.rox.roundabout.window.Input;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Everything about one memory: a heat-coloured hex grid, with a memory map docked to its left and
 * watches (list + selected watch's history) docked below. The map and watches fold away
 * independently, giving their space to the hex grid and skipping their drawing.
 */
public final class MemoryPanel {
    private static final float SAMPLE_SECONDS = 1 / 60f;
    private static final int HISTORY_SECONDS = 10;
    private static final int HISTORY = Math.round(HISTORY_SECONDS / SAMPLE_SECONDS);
    private static final float WATCH_ROW = 40;

    private record Watch(String label, int address, RingSeries history) {
        Watch(String label, int address) {
            this(label, address, new RingSeries(HISTORY));
        }
    }

    private final MemorySource memory;
    private final List<MemoryRegion> regions;
    private final AddressFormat format;
    private final MemoryHeat heat;
    private final SampleClock sampleClock = new SampleClock(SAMPLE_SECONDS, HISTORY);
    private final HexGrid hex;
    private final MemoryMap map;
    private final List<Watch> watches = new ArrayList<>();
    private int selectedWatch;
    private float watchScroll;
    private boolean revealWatch;

    private final Dock mapDock = new Dock("Memory map", Dock.Side.LEFT);
    private final Dock watchDock = new Dock("Watches", Dock.Side.BOTTOM);
    private final Dock historyDock = new Dock("History", Dock.Side.RIGHT);

    public MemoryPanel(MemorySource memory, List<MemoryRegion> regions, List<NamedAddress> labels) {
        this(memory, regions, labels, AddressFormat.Style.C);
    }

    public MemoryPanel(MemorySource memory, List<MemoryRegion> regions, List<NamedAddress> labels,
                       AddressFormat.Style style) {
        this.memory = memory;
        this.regions = regions;
        format = AddressFormat.forSize(memory.size(), style);
        heat = new MemoryHeat(memory, 0.35f);
        hex = new HexGrid(memory.size(), regions, format);
        map = new MemoryMap(regions, format);
        for (NamedAddress label : labels) {
            watches.add(new Watch(label.name(), label.address()));
        }
    }

    public MemoryHeat heat() {
        return heat;
    }

    public AddressFormat format() {
        return format;
    }

    public MemorySource memory() {
        return memory;
    }

    /** The address under the mouse in the hex grid, else the selected one. */
    public int focusAddress() {
        return hex.hovered() >= 0 ? hex.hovered() : hex.selected();
    }

    /**
     * Samples memory; call once per frame, before {@link #draw}. Watch history is sampled every
     * {@link #SAMPLE_SECONDS} whatever the frame rate, and keeps recording while folded.
     */
    public void update(float dt) {
        heat.update(dt);
        for (int n = sampleClock.advance(dt); n > 0; n--) {
            for (Watch w : watches) {
                w.history().add(heat.value(w.address()));
            }
        }
    }

    public void handleKeys(Input input) {
        if (input.keyPressed(GLFW_KEY_W)) {
            addWatch(hex.selected());
        }
        if (input.keyPressed(GLFW_KEY_DELETE) && !watches.isEmpty()) {
            watches.remove(selectedWatch);
            selectedWatch = Math.clamp(selectedWatch, 0, watches.size() - 1);
            revealWatch = true;
        }
    }

    public void draw(Ui ui, Box area, float dt) {
        Box main = area.copy();
        Box mapBox = mapDock.cut(ui, main, 200);
        if (mapBox != null) {
            int jump = map.draw(ui, ui.panel(mapBox, mapDock.title()), heat, hex.firstVisibleAddress(), hex.lastVisibleAddress());
            if (jump >= 0) {
                hex.scrollTo(jump);
            }
        }

        Box watchRow = watchDock.cut(ui, main, 230);
        hex.draw(ui, ui.panel(main, "Memory  ·  " + format.address(0) + " – " + format.address(heat.size() - 1)), heat, dt);
        if (hex.activated() >= 0) {
            addWatch(hex.activated());
        }

        if (watchRow != null) {
            Box graph = historyDock.cut(ui, watchRow, Math.max(0, watchRow.w - 340 - Dock.GUTTER));
            watchList(ui, watchRow);
            if (graph != null) {
                watchGraph(ui, graph);
            }
        }
    }

    private void addWatch(int address) {
        for (int i = 0; i < watches.size(); i++) {
            if (watches.get(i).address() == address) {
                selectedWatch = i;
                revealWatch = true;
                return;
            }
        }
        MemoryRegion region = MemoryRegion.find(regions, address);
        watches.add(new Watch(region == null ? "Watch" : region.name(), address));
        selectedWatch = watches.size() - 1;
        revealWatch = true;
    }

    private void watchList(Ui ui, Box area) {
        Theme t = ui.theme;
        Box content = ui.panel(area, watchDock.title());
        if (watches.isEmpty()) {
            ui.text(content.cutTop(20), "Double-click a byte, or press W, to watch it", t.uiSmall, t.textFaint);
            return;
        }
        float total = watches.size() * WATCH_ROW;
        if (total > content.h) {
            Box scrollbar = content.cutRight(8);
            content.cutRight(4);
            if (ui.hovered(content) && ui.input.scrollY != 0) {
                watchScroll -= ui.input.scrollY * WATCH_ROW;
            }
            if (revealWatch) {
                float top = selectedWatch * WATCH_ROW;
                watchScroll = Math.clamp(watchScroll, top + WATCH_ROW - content.h, top);
            }
            watchScroll = Math.clamp(watchScroll, 0, total - content.h);
            watchScroll = ui.scrollbar("watches.scrollbar", scrollbar, watchScroll, total, content.h);
        } else {
            watchScroll = 0;
        }
        revealWatch = false;

        ui.clip(content);
        Box list = new Box(content.x, content.y - watchScroll, content.w, total);
        for (int i = 0; i < watches.size(); i++) {
            Box row = list.cutTop(WATCH_ROW - 2);
            list.cutTop(2);
            if (row.bottom() >= content.y && row.y <= content.bottom()) {
                watchRow(ui, i, row, content);
            }
        }
        ui.unclip();
    }

    private void watchRow(Ui ui, int index, Box row, Box visible) {
        Theme t = ui.theme;
        Watch watch = watches.get(index);
        boolean selected = index == selectedWatch;
        float hitTop = Math.max(row.y, visible.y);
        Box hit = new Box(row.x, hitTop, row.w, Math.min(row.bottom(), visible.bottom()) - hitTop);
        if (ui.clicked("watch" + index, hit)) {
            selectedWatch = index;
        }
        // double-click flags the press, so check it while the row holds the mouse
        if (ui.input.doubleClicked && ui.isActive("watch" + index)) {
            hex.scrollTo(watch.address());
        }
        if (selected || ui.hovered(hit)) {
            ui.fillRound(row, 5, selected ? Theme.mix(t.panel, t.accent, 0.12f) : t.panelRaised);
        }
        Box inner = row.inset(10, 4);
        Box spark = inner.cutRight(100).inset(0, 4);
        Graphs.sparkline(ui, spark, watch.history(), 0, 255, selected ? t.accent : t.textDim);
        inner.cutRight(10);
        int value = heat.value(watch.address());
        float valueHeat = heat.heat(watch.address());
        Box valueBox = inner.cutRight(40);
        ui.textRight(valueBox, String.format("%02X", value), t.monoLarge, Theme.mix(t.text, t.warn, valueHeat));
        ui.text(inner.cutTop(16), watch.label(), t.ui, t.text);
        ui.text(inner, format.address(watch.address()), t.uiSmall, t.textDim);
    }

    private void watchGraph(Ui ui, Box area) {
        Theme t = ui.theme;
        if (watches.isEmpty()) {
            ui.panel(area, historyDock.title());
            return;
        }
        Watch w = watches.get(Math.min(selectedWatch, watches.size() - 1));
        Box content = ui.panel(area, w.label() + "  ·  " + format.address(w.address()) + "  ·  last " + HISTORY_SECONDS + "s");
        Graphs.lineGraph(ui, content, w.history(), 0, 255, t.accent, SAMPLE_SECONDS,
                v -> String.format("%02X", (int) v), v -> format.value((int) v) + "  " + (int) v);
    }
}
