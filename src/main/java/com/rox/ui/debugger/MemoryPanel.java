package com.rox.ui.debugger;

import com.rox.ui.Box;
import com.rox.ui.Dock;
import com.rox.ui.Theme;
import com.rox.ui.Ui;
import com.rox.ui.data.Graphs;
import com.rox.ui.data.HexGrid;
import com.rox.ui.data.MemoryHeat;
import com.rox.ui.data.MemoryMap;
import com.rox.ui.data.MemoryRegion;
import com.rox.ui.data.MemorySource;
import com.rox.ui.data.RingSeries;
import com.rox.ui.window.Input;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Everything about one memory: a heat-coloured hex grid, with a memory map docked to its left and
 * watches (list + selected watch's history) docked below. The map and watches fold away
 * independently, giving their space to the hex grid and skipping their drawing.
 */
public final class MemoryPanel {
    private static final int HISTORY = 600; // 10s at 60fps
    private static final float WATCH_ROW = 40;

    private record Watch(String label, int address, RingSeries history) {
        Watch(String label, int address) {
            this(label, address, new RingSeries(HISTORY));
        }
    }

    private final MemorySource memory;
    private final List<MemoryRegion> regions;
    private final MemoryHeat heat;
    private final HexGrid hex;
    private final MemoryMap map;
    private final List<Watch> watches = new ArrayList<>();
    private int selectedWatch;
    private float watchScroll;
    private boolean revealWatch;

    private final Dock mapDock = new Dock("Memory map", Dock.Side.LEFT);
    private final Dock watchDock = new Dock("Watches", Dock.Side.BOTTOM);
    private final Dock historyDock = new Dock("History", Dock.Side.RIGHT);

    public MemoryPanel(MemorySource memory, List<MemoryRegion> regions, List<DebugTarget.Label> labels) {
        this.memory = memory;
        this.regions = regions;
        heat = new MemoryHeat(memory, 0.35f);
        hex = new HexGrid(memory.size(), regions);
        map = new MemoryMap(regions);
        for (DebugTarget.Label label : labels) {
            watches.add(new Watch(label.name(), label.address()));
        }
    }

    public MemoryHeat heat() {
        return heat;
    }

    public MemorySource memory() {
        return memory;
    }

    /** The address under the mouse in the hex grid, else the selected one. */
    public int focusAddress() {
        return hex.hovered() >= 0 ? hex.hovered() : hex.selected();
    }

    /** Samples memory; call once per frame, before {@link #draw}. Watch history keeps recording while folded. */
    public void update(float dt) {
        heat.update(dt);
        for (Watch w : watches) {
            w.history().add(heat.value(w.address()));
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
        hex.draw(ui, ui.panel(main, String.format("Memory  ·  $0000 – $%04X", heat.size() - 1)), heat, dt);
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
            Watch w = watches.get(i);
            Box row = list.cutTop(WATCH_ROW - 2);
            list.cutTop(2);
            if (row.bottom() < content.y || row.y > content.bottom()) {
                continue;
            }
            boolean selected = i == selectedWatch;
            float hitTop = Math.max(row.y, content.y);
            Box hit = new Box(row.x, hitTop, row.w, Math.min(row.bottom(), content.bottom()) - hitTop);
            if (ui.clicked("watch" + i, hit)) {
                selectedWatch = i;
            }
            // double-click flags the press, so check it while the row holds the mouse
            if (ui.input.doubleClicked && ui.isActive("watch" + i)) {
                hex.scrollTo(w.address());
            }
            if (selected || ui.hovered(hit)) {
                ui.fillRound(row, 5, selected ? Theme.mix(t.panel, t.accent, 0.12f) : t.panelRaised);
            }
            Box inner = row.inset(10, 4);
            Box spark = inner.cutRight(100).inset(0, 4);
            Graphs.sparkline(ui, spark, w.history(), 0, 255, selected ? t.accent : t.textDim);
            inner.cutRight(10);
            int v = heat.value(w.address());
            Box value = inner.cutRight(40);
            float h = heat.heat(w.address());
            ui.textRight(value, String.format("%02X", v), t.monoLarge, Theme.mix(t.text, t.warn, h));
            ui.text(inner.cutTop(16), w.label(), t.ui, t.text);
            ui.text(inner, String.format("$%04X", w.address()), t.uiSmall, t.textDim);
        }
        ui.unclip();
    }

    private void watchGraph(Ui ui, Box area) {
        Theme t = ui.theme;
        if (watches.isEmpty()) {
            ui.panel(area, historyDock.title());
            return;
        }
        Watch w = watches.get(Math.min(selectedWatch, watches.size() - 1));
        Box content = ui.panel(area, String.format("%s  ·  $%04X  ·  last 10s", w.label(), w.address()));
        Graphs.lineGraph(ui, content, w.history(), 0, 255, t.accent, 1 / 60f);
    }
}
