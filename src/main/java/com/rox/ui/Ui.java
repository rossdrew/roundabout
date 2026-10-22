package com.rox.ui;

import com.rox.ui.window.Input;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.FontMetrics;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.PaintMode;
import io.github.humbleui.types.RRect;

import java.util.ArrayList;
import java.util.List;

/**
 * Immediate-mode UI context. Call {@link #begin} once per frame, then draw widgets;
 * widgets return their interaction result directly.
 */
public final class Ui {
    public final Theme theme;
    public Canvas canvas;
    public Input input;

    private final Paint fill = new Paint().setAntiAlias(true);
    private final Paint stroke = new Paint().setAntiAlias(true).setMode(PaintMode.STROKE).setStrokeWidth(1);

    /** The widget holding the mouse (pressed on it and not yet released). */
    private String active;
    private String tooltip;
    private final List<Runnable> overlays = new ArrayList<>();
    private float width, height;

    public Ui(Theme theme) {
        this.theme = theme;
    }

    public void begin(Canvas canvas, Input input, float width, float height) {
        this.canvas = canvas;
        this.input = input;
        this.width = width;
        this.height = height;
        this.tooltip = null;
        if (!input.mouseDown && !input.mouseReleased) {
            active = null;
        }
    }

    public void end() {
        for (Runnable overlay : overlays) {
            overlay.run();
        }
        overlays.clear();
        if (tooltip != null) {
            drawTooltip(tooltip);
        }
        if (input.mouseReleased) {
            active = null;
        }
    }

    // ---- interaction -------------------------------------------------------------------------

    public boolean hovered(Box box) {
        return active == null && box.contains(input.mouseX, input.mouseY);
    }

    /** Claims the mouse for {@code id} when pressed inside {@code box}; true while held. */
    public boolean drag(String id, Box box) {
        if (input.mousePressed && box.contains(input.mouseX, input.mouseY) && active == null) {
            active = id;
        }
        return id.equals(active) && input.mouseDown;
    }

    /** True on the frame the mouse is released over {@code box} after being pressed on it. */
    public boolean clicked(String id, Box box) {
        drag(id, box);
        return isActive(id) && input.mouseReleased && box.contains(input.mouseX, input.mouseY);
    }

    public boolean isActive(String id) {
        return id.equals(active);
    }

    public void setTooltip(String text) {
        tooltip = text;
    }

    /** Draws {@code draw} after everything else this frame (under the tooltip), e.g. a decoration straddling two panels. */
    public void overlay(Runnable draw) {
        overlays.add(draw);
    }

    // ---- primitives --------------------------------------------------------------------------

    public void fill(Box box, int colour) {
        fill.setColor(colour);
        canvas.drawRect(box.rect(), fill);
    }

    public void fillRound(Box box, float radius, int colour) {
        fill.setColor(colour);
        canvas.drawRRect(RRect.makeXYWH(box.x, box.y, box.w, box.h, radius), fill);
    }

    public void strokeRound(Box box, float radius, int colour) {
        stroke.setColor(colour);
        canvas.drawRRect(RRect.makeXYWH(box.x + 0.5f, box.y + 0.5f, box.w - 1, box.h - 1, radius), stroke);
    }

    public void line(float x0, float y0, float x1, float y1, int colour) {
        stroke.setColor(colour);
        canvas.drawLine(x0, y0, x1, y1, stroke);
    }

    public Paint paint(int colour) {
        return fill.setColor(colour);
    }

    public Paint strokePaint(int colour, float width) {
        return stroke.setColor(colour).setStrokeWidth(width);
    }

    /** Draws text left-aligned, vertically centred in {@code box}. Returns the text width. */
    public float text(Box box, String text, Font font, int colour) {
        canvas.drawString(text, box.x, baseline(box, font), font, paint(colour));
        return font.measureTextWidth(text);
    }

    public void textRight(Box box, String text, Font font, int colour) {
        float width = font.measureTextWidth(text);
        canvas.drawString(text, box.right() - width, baseline(box, font), font, paint(colour));
    }

    public void textCentred(Box box, String text, Font font, int colour) {
        float width = font.measureTextWidth(text);
        canvas.drawString(text, box.x + (box.w - width) / 2, baseline(box, font), font, paint(colour));
    }

    public static float baseline(Box box, Font font) {
        FontMetrics m = font.getMetrics();
        return box.centerY() - (m.getAscent() + m.getDescent()) / 2;
    }

    public void clip(Box box) {
        canvas.save();
        canvas.clipRect(box.rect());
    }

    public void unclip() {
        canvas.restore();
    }

    // ---- widgets -----------------------------------------------------------------------------

    /** A panel with an optional header; returns the content area. */
    public Box panel(Box box, String title) {
        fillRound(box, 6, theme.panel);
        strokeRound(box, 6, theme.border);
        if (title == null) {
            return box.inset(8);
        }
        Box content = box.copy();
        Box header = content.cutTop(28).inset(10, 0);
        text(header, title.toUpperCase(), theme.uiSmall, theme.textDim);
        line(box.x + 1, content.y, box.right() - 1, content.y, theme.border);
        return content.inset(8);
    }

    public boolean button(String id, Box box, String label) {
        return button(id, box, label, false);
    }

    /** A button; {@code on} renders it in its toggled/primary state. Returns true when clicked. */
    public boolean button(String id, Box box, String label, boolean on) {
        boolean hover = box.contains(input.mouseX, input.mouseY);
        boolean clicked = clicked(id, box);
        boolean held = isActive(id) && input.mouseDown;
        int bg = on ? Theme.withAlpha(theme.accent, held ? 0.35f : hover ? 0.28f : 0.2f)
                : held ? theme.border : hover ? theme.panelRaised : theme.panel;
        fillRound(box, 5, bg);
        strokeRound(box, 5, on ? Theme.withAlpha(theme.accent, 0.6f) : hover ? theme.textFaint : theme.border);
        textCentred(box, label, theme.ui, on ? theme.accent : theme.text);
        return clicked;
    }

    /**
     * A vertical scrollbar for {@code visible} of {@code total} units, scrolled to {@code offset}.
     * Returns the offset to jump to while the thumb is dragged (centred on the mouse), else {@code offset}.
     */
    public float scrollbar(String id, Box track, float offset, float total, float visible) {
        Box inner = track.inset(2, 0);
        float max = Math.max(0, total - visible);
        if (drag(id, track)) {
            float f = (input.mouseY - track.y) / track.h;
            offset = Math.clamp(f * total - visible / 2, 0, max);
        }
        fillRound(inner, 3, theme.panelRaised);
        float thumbH = Math.min(inner.h, Math.max(24, inner.h * visible / Math.max(1, total)));
        float thumbY = inner.y + (inner.h - thumbH) * (offset / Math.max(1, max));
        boolean hot = isActive(id) || track.contains(input.mouseX, input.mouseY);
        fillRound(new Box(inner.x, thumbY, inner.w, thumbH), 3, hot ? theme.textDim : theme.textFaint);
        return offset;
    }

    /** A small label/value tile, e.g. a CPU register. */
    public void readout(Box box, String label, String value, int valueColour) {
        fillRound(box, 5, theme.panelRaised);
        Box inner = box.inset(8, 4);
        text(inner.cutTop(14), label, theme.uiSmall, theme.textDim);
        text(inner, value, theme.monoLarge, valueColour);
    }

    /** A small pill that is lit when {@code on}. */
    public void chip(Box box, String label, boolean on) {
        fillRound(box, 4, on ? Theme.withAlpha(theme.good, 0.18f) : theme.panelRaised);
        textCentred(box, label, theme.mono, on ? theme.good : theme.textFaint);
    }

    private void drawTooltip(String text) {
        String[] lines = text.split("\n");
        float textWidth = 0;
        for (String l : lines) {
            textWidth = Math.max(textWidth, theme.mono.measureTextWidth(l));
        }
        float lineHeight = 18;
        float w = textWidth + 20, h = lines.length * lineHeight + 12;
        float x = input.mouseX + 14 + w > width ? input.mouseX - 10 - w : input.mouseX + 14;
        float y = input.mouseY + 18 + h > height ? input.mouseY - 10 - h : input.mouseY + 18;
        Box box = new Box(x, y, w, h);
        canvas.drawRectShadow(box.rect(), 0, 4, 12, 0, 0x80000000);
        fillRound(box, 5, theme.panelRaised);
        strokeRound(box, 5, theme.textFaint);
        Box inner = box.inset(10, 6);
        for (int i = 0; i < lines.length; i++) {
            text(inner.cutTop(lineHeight), lines[i], theme.mono, i == 0 ? theme.text : theme.textDim);
        }
    }
}
