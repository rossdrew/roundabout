package com.rox.ui.debugger;

import com.rox.ui.Box;
import com.rox.ui.Theme;
import com.rox.ui.Ui;
import com.rox.ui.data.Graphs;
import com.rox.ui.data.HexGrid;
import com.rox.ui.data.MemoryHeat;
import com.rox.ui.data.MemoryMap;
import com.rox.ui.data.MemoryRegion;
import com.rox.ui.data.RingSeries;
import com.rox.ui.window.GlfwWindow;
import com.rox.ui.window.Input;
import io.github.humbleui.skija.Canvas;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Live memory debugger window over a {@link DebugTarget}: memory map, heat-coloured hex grid,
 * CPU registers, watches with history graphs, and run controls.
 */
public final class MemoryDebugger {
    private static final int HISTORY = 600; // 10s at 60fps

    private record Watch(String label, int address, RingSeries history) {
        Watch(String label, int address) {
            this(label, address, new RingSeries(HISTORY));
        }
    }

    private final DebugTarget target;
    private final String title;
    private final Ui ui = new Ui(Theme.dark());
    private final MemoryHeat heat;
    private final HexGrid hex;
    private final MemoryMap map;
    private final List<Watch> watches = new ArrayList<>();
    private int selectedWatch;

    private final RingSeries frameMs = new RingSeries(120);
    private final RingSeries writesPerFrame = new RingSeries(120);
    private long lastFrame = System.nanoTime();
    private volatile GlfwWindow window;
    private volatile boolean closeRequested;

    public MemoryDebugger(DebugTarget target, String title) {
        this.target = target;
        this.title = title;
        heat = new MemoryHeat(target.memory(), 0.35f);
        hex = new HexGrid(target.memory().size(), target.regions());
        map = new MemoryMap(target.regions());
        for (DebugTarget.Label label : target.labels()) {
            watches.add(new Watch(label.name(), label.address()));
        }
    }

    /** Opens the window and blocks until it is closed (by the user or {@link #close()}). */
    public void run() {
        try (GlfwWindow w = new GlfwWindow(title, 1440, 860)) {
            window = w;
            if (closeRequested) {
                w.requestClose();
            }
            w.run(this::frame);
        } finally {
            window = null;
        }
    }

    /** Asks the window to close; safe from any thread. */
    public void close() {
        closeRequested = true;
        GlfwWindow w = window;
        if (w != null) {
            w.requestClose();
        }
    }

    private void frame(Canvas canvas, Input input, float width, float height) {
        long start = System.nanoTime();
        float dt = Math.min(0.1f, (start - lastFrame) / 1e9f);
        lastFrame = start;

        heat.update(dt);
        writesPerFrame.add(heat.changedLastFrame());
        for (Watch w : watches) {
            w.history().add(heat.value(w.address()));
        }

        Theme t = ui.theme;
        canvas.clear(t.background);
        ui.begin(canvas, input, width, height);
        handleKeys(input);

        Box screen = new Box(0, 0, width, height);
        topBar(screen.cutTop(48));
        statusBar(screen.cutBottom(28));
        Box body = screen.inset(10, 6);

        Box left = body.cutLeft(200);
        body.cutLeft(10);
        Box right = body.cutRight(340);
        body.cutRight(10);

        int jump = map.draw(ui, ui.panel(left, "Memory map"), heat, hex.firstVisibleAddress(), hex.lastVisibleAddress());
        if (jump >= 0) {
            hex.scrollTo(jump);
        }

        hex.draw(ui, ui.panel(body, String.format("Memory  ·  $0000 – $%04X", heat.size() - 1)), heat, dt);
        if (hex.activated() >= 0) {
            addWatch(hex.activated());
        }

        registers(right.cutTop(150));
        right.cutTop(10);
        Box graph = right.cutBottom(230);
        right.cutBottom(10);
        watchList(right);
        watchGraph(graph);

        ui.end();
        frameMs.add((System.nanoTime() - start) / 1e6f);
    }

    private void handleKeys(Input input) {
        if (input.keyPressed(GLFW_KEY_SPACE)) {
            togglePause();
        }
        if (input.keyPressed(GLFW_KEY_F) && target.isPaused()) {
            target.stepFrame();
        }
        if (input.keyPressed(GLFW_KEY_I) && target.isPaused()) {
            target.stepInstruction();
        }
        if (input.keyPressed(GLFW_KEY_W)) {
            addWatch(hex.selected());
        }
        if (input.keyPressed(GLFW_KEY_DELETE) && !watches.isEmpty()) {
            watches.remove(selectedWatch);
            selectedWatch = Math.max(0, Math.min(selectedWatch, watches.size() - 1));
        }
    }

    private void togglePause() {
        if (target.isPaused()) {
            target.resume();
        } else {
            target.pause();
        }
    }

    private void addWatch(int address) {
        for (int i = 0; i < watches.size(); i++) {
            if (watches.get(i).address() == address) {
                selectedWatch = i;
                return;
            }
        }
        MemoryRegion region = MemoryRegion.find(target.regions(), address);
        watches.add(new Watch(region == null ? "Watch" : region.name(), address));
        selectedWatch = watches.size() - 1;
    }

    // ---- sections -----------------------------------------------------------------------------

    private void topBar(Box bar) {
        Theme t = ui.theme;
        ui.fill(bar, t.panel);
        ui.line(bar.x, bar.bottom() - 0.5f, bar.right(), bar.bottom() - 0.5f, t.border);
        Box row = bar.inset(14, 9);

        Box brand = row.cutLeft(170);
        float w = ui.text(brand, "roxUI", t.uiBold, t.accent);
        ui.text(new Box(brand.x + w + 8, brand.y, 120, brand.h), "memory debugger", t.ui, t.textDim);

        boolean paused = target.isPaused();
        if (ui.button("run", row.cutLeft(92), paused ? "▶  Run" : "❚❚  Pause", paused)) {
            togglePause();
        }
        row.cutLeft(8);
        if (ui.button("step", row.cutLeft(100), "Step frame") && paused) {
            target.stepFrame();
        }
        row.cutLeft(8);
        if (ui.button("stepi", row.cutLeft(100), "Step instr") && paused) {
            target.stepInstruction();
        }
        row.cutLeft(16);
        Box state = row.cutLeft(380);
        ui.fillRound(new Box(state.x, state.centerY() - 4, 8, 8), 4, paused ? t.warn : t.good);
        ui.text(new Box(state.x + 16, state.y, state.w - 16, state.h),
                (paused ? "Paused" : "Running") + "  ·  " + target.status(), t.ui, t.textDim);

        // right: UI frame time + changes/frame sparklines
        Box perf = row.cutRight(170);
        Box spark = perf.cutRight(70).inset(0, 4);
        Graphs.sparkline(ui, spark, frameMs, 0, Math.max(4, frameMs.max()), t.accent);
        ui.textRight(perf.inset(8, 0), String.format("UI %.2f ms", frameMs.average()), t.mono, t.textDim);
        row.cutRight(14);
        Box writes = row.cutRight(180);
        Box spark2 = writes.cutRight(70).inset(0, 4);
        Graphs.sparkline(ui, spark2, writesPerFrame, 0, Math.max(50, writesPerFrame.max()), t.warn);
        ui.textRight(writes.inset(8, 0), String.format("%d changes/f", (int) writesPerFrame.latest()), t.mono, t.textDim);
    }

    private void statusBar(Box bar) {
        Theme t = ui.theme;
        ui.fill(bar, t.panel);
        ui.line(bar.x, bar.y + 0.5f, bar.right(), bar.y + 0.5f, t.border);
        Box row = bar.inset(14, 0);
        int address = hex.hovered() >= 0 ? hex.hovered() : hex.selected();
        int v = heat.value(address);
        MemoryRegion region = MemoryRegion.find(target.regions(), address);
        String bin = String.format("%8s", Integer.toBinaryString(v)).replace(' ', '0');
        ui.text(row, String.format("$%04X  %-14s $%02X  %3d  %%%s   %s",
                address, region == null ? "" : region.name(), v, v, bin,
                target.memory().isEditable(address) ? "writable" : "read-only"), t.mono, t.textDim);
        ui.textRight(row, "Space run/pause   F step frame   I step instr   ↑↓ PgUp PgDn navigate   W / double-click watch   Del unwatch",
                t.uiSmall, t.textFaint);
    }

    private void registers(Box area) {
        Theme t = ui.theme;
        DebugTarget.CpuState cpu = target.cpu();
        Box content = ui.panel(area, "CPU");
        Box top = content.cutTop(48);
        float unit = (top.w - 5 * 6) / 6.4f;
        String[][] regs = {
                {"PC", String.format("%04X", cpu.pc())}, {"A", String.format("%02X", cpu.a())},
                {"X", String.format("%02X", cpu.x())}, {"Y", String.format("%02X", cpu.y())},
                {"SP", String.format("%02X", cpu.sp())}, {"P", String.format("%02X", cpu.p())}};
        for (int i = 0; i < regs.length; i++) {
            Box cell = top.cutLeft(i == 0 ? unit * 1.4f : unit);
            ui.readout(cell, regs[i][0], regs[i][1], i == 0 ? t.accent : t.text);
            top.cutLeft(6);
        }
        content.cutTop(10);
        Box flags = content.cutTop(26);
        String names = "NV-BDIZC";
        float fw = (flags.w - 7 * 4) / 8;
        for (int i = 0; i < 8; i++) {
            ui.chip(flags.cutLeft(fw), String.valueOf(names.charAt(i)), (cpu.p() & (0x80 >> i)) != 0);
            flags.cutLeft(4);
        }
    }

    private void watchList(Box area) {
        Theme t = ui.theme;
        Box content = ui.panel(area, "Watches");
        if (watches.isEmpty()) {
            ui.text(content.cutTop(20), "Double-click a byte, or press W, to watch it", t.uiSmall, t.textFaint);
            return;
        }
        ui.clip(content);
        for (int i = 0; i < watches.size(); i++) {
            Watch w = watches.get(i);
            Box row = content.cutTop(38);
            if (row.h <= 0) break;
            boolean selected = i == selectedWatch;
            if (ui.clicked("watch" + i, row)) {
                selectedWatch = i;
                hex.scrollTo(w.address());
            }
            if (selected || ui.hovered(row)) {
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
            content.cutTop(2);
        }
        ui.unclip();
    }

    private void watchGraph(Box area) {
        Theme t = ui.theme;
        if (watches.isEmpty()) {
            ui.panel(area, "History");
            return;
        }
        Watch w = watches.get(Math.min(selectedWatch, watches.size() - 1));
        Box content = ui.panel(area, String.format("%s  ·  $%04X  ·  last 10s", w.label(), w.address()));
        Graphs.lineGraph(ui, content, w.history(), 0, 255, t.accent, 1 / 60f);
    }
}
