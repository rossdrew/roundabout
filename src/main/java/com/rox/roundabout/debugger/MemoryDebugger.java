package com.rox.roundabout.debugger;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.Ui;
import com.rox.roundabout.chart.Graphs;
import com.rox.roundabout.chart.RingSeries;
import com.rox.roundabout.memory.AddressFormat;
import com.rox.roundabout.memory.MemoryPanel;
import com.rox.roundabout.memory.MemoryRegion;
import com.rox.roundabout.timing.FrameTimes;
import com.rox.roundabout.timing.FrameTimingGraph;
import com.rox.roundabout.window.GlfwWindow;
import com.rox.roundabout.window.Input;
import io.github.humbleui.skija.Canvas;

import java.util.List;
import java.util.Locale;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Live debugger window over a {@link DebugTarget}: a {@link MemoryPanel}, CPU registers,
 * frame timing, and run controls.
 */
public final class MemoryDebugger {
    private final DebugTarget target;
    private final String title;
    private final Ui ui = new Ui(Theme.dark());
    private final MemoryPanel memory;

    private final RingSeries frameMs = new RingSeries(120);
    private final RingSeries writesPerFrame = new RingSeries(120);
    private long lastFrame = System.nanoTime();
    private volatile GlfwWindow window;
    private volatile boolean closeRequested;

    public MemoryDebugger(DebugTarget target, String title) {
        this.target = target;
        this.title = title;
        memory = new MemoryPanel(target.memory(), target.regions(), target.labels(), target.numberStyle());
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

        memory.update(dt);
        writesPerFrame.add(memory.heat().changedLastFrame());
        for (FrameTimes times : target.frameTimings()) {
            times.update();
        }

        Theme t = ui.theme;
        canvas.clear(t.background);
        ui.begin(canvas, input, width, height);
        handleKeys(input);

        Box screen = new Box(0, 0, width, height);
        topBar(screen.cutTop(48));
        statusBar(screen.cutBottom(28));
        Box body = screen.inset(10, 6);

        Box right = body.cutRight(340);
        body.cutRight(10);
        DebugTarget.CpuState cpu = target.cpu();
        CpuPanel.draw(ui, right.cutTop(CpuPanel.height(right.w, cpu)), cpu);
        List<FrameTimes> timings = target.frameTimings();
        if (!timings.isEmpty()) {
            right.cutTop(10);
            frameTiming(right.cutTop(40 + 150 * timings.size()), timings);
        }

        memory.draw(ui, body, dt);

        ui.end();
        frameMs.add((System.nanoTime() - start) / 1e6f);
    }

    private void handleKeys(Input input) {
        if (input.keyPressed(GLFW_KEY_SPACE)) {
            togglePause();
        }
        for (DebugTarget.Step step : target.steps()) {
            if (step.key() != GLFW_KEY_UNKNOWN && input.keyPressed(step.key()) && target.isPaused()) {
                step.run().run();
            }
        }
        memory.handleKeys(input);
    }

    private void togglePause() {
        if (target.isPaused()) {
            target.resume();
        } else {
            target.pause();
        }
    }

    // ---- sections -----------------------------------------------------------------------------

    private void topBar(Box bar) {
        Theme t = ui.theme;
        ui.fill(bar, t.panel);
        ui.line(bar.x, bar.bottom() - 0.5f, bar.right(), bar.bottom() - 0.5f, t.border);
        Box row = bar.inset(14, 9);

        ui.text(row.cutLeft(100), "Roundabout", t.uiBold, t.accent);

        boolean paused = target.isPaused();
        if (ui.button("run", row.cutLeft(92), paused ? "▶  Run" : "❚❚  Pause", paused)) {
            togglePause();
        }
        for (DebugTarget.Step step : target.steps()) {
            row.cutLeft(8);
            float w = Math.max(100, t.ui.measureTextWidth(step.label()) + 28);
            if (ui.button("step:" + step.label(), row.cutLeft(w), step.label()) && paused) {
                step.run().run();
            }
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
        int address = memory.focusAddress();
        int v = memory.heat().value(address);
        MemoryRegion region = MemoryRegion.find(target.regions(), address);
        AddressFormat format = memory.format();
        ui.text(row, String.format("%s  %-14s %s  %3d  %s   %s", format.address(address),
                region == null ? "" : region.name(), format.value(v), v, format.binary(v),
                target.memory().isEditable(address) ? "writable" : "read-only"), t.mono, t.textDim);
        ui.textRight(row, keyHints(target.steps()), t.uiSmall, t.textFaint);
    }

    static String keyHints(List<DebugTarget.Step> steps) {
        StringBuilder hints = new StringBuilder("Space run/pause");
        for (DebugTarget.Step step : steps) {
            if (step.key() != GLFW_KEY_UNKNOWN) {
                hints.append("   ").append(Input.keyName(step.key()))
                        .append(' ').append(step.label().toLowerCase(Locale.ROOT));
            }
        }
        return hints.append("   ↑↓ PgUp PgDn navigate   W / double-click watch   double-click watch to jump   Del unwatch")
                .toString();
    }

    private void frameTiming(Box area, List<FrameTimes> timings) {
        Box content = ui.panel(area, "Frame timing  ·  ms between frames");
        float rowH = (content.h - 10 * (timings.size() - 1)) / timings.size();
        for (int i = 0; i < timings.size(); i++) {
            if (i > 0) {
                content.cutTop(10);
            }
            FrameTimingGraph.draw(ui, content.cutTop(rowH), timings.get(i));
        }
    }
}
