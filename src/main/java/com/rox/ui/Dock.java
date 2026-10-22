package com.rox.ui;

import io.github.humbleui.types.RRect;

/**
 * A foldable panel docked to one side of another. {@link #cut} slices the panel and a gutter off
 * an area and draws a link across the gutter showing the docked panel is tied to what remains.
 * The link doubles as the fold toggle: when folded the panel shrinks to a strip (which also
 * unfolds it on click), its rings come apart, and the caller skips drawing the panel entirely.
 */
public final class Dock {
    public enum Side { LEFT, RIGHT, TOP, BOTTOM }

    public static final float GUTTER = 10;
    public static final float FOLDED = 24;
    /** Where the link sits along the shared edge: level with a panel header, or in from the corner. */
    private static final float LINK_OFFSET = 14;

    private final String title;
    private final Side side;
    private boolean open = true;

    public Dock(String title, Side side) {
        this.title = title;
        this.side = side;
    }

    public String title() {
        return title;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void toggle() {
        open = !open;
    }

    /**
     * Cuts the docked panel ({@code size} deep, or a strip when folded) and its gutter from
     * {@code area}. Returns the panel's box, or null when folded.
     */
    public Box cut(Ui ui, Box area, float size) {
        Box panel = cut(area, open ? size : FOLDED);
        Box gutter = cut(area, GUTTER);
        if (open) {
            link(ui, gutter);
            return panel;
        }
        strip(ui, panel);
        link(ui, gutter);
        return null;
    }

    private Box cut(Box area, float amount) {
        return switch (side) {
            case LEFT -> area.cutLeft(amount);
            case RIGHT -> area.cutRight(amount);
            case TOP -> area.cutTop(amount);
            case BOTTOM -> area.cutBottom(amount);
        };
    }

    private boolean vertical() {
        return side == Side.LEFT || side == Side.RIGHT;
    }

    private void link(Ui ui, Box gutter) {
        Theme t = ui.theme;
        // spans the gutter and overlaps both panels' borders
        float across = GUTTER + 8, along = 12;
        Box glyph = vertical()
                ? new Box(gutter.centerX() - across / 2, gutter.y + LINK_OFFSET - along / 2, across, along)
                : new Box(gutter.x + LINK_OFFSET + 10 - along / 2, gutter.centerY() - across / 2, along, across);
        String id = "dock." + title;
        boolean hover = ui.hovered(glyph.inset(-3));
        if (ui.clicked(id, glyph.inset(-3))) {
            toggle();
        }
        if (hover) {
            ui.setTooltip((open ? "Fold " : "Show ") + title.toLowerCase());
        }
        int colour = hover ? t.accent : open ? Theme.withAlpha(t.accent, 0.55f) : t.textDim;
        boolean linked = open;
        ui.overlay(() -> drawLink(ui, glyph, colour, linked));
    }

    /** Two interlocked rings when linked; pulled apart when folded. */
    private void drawLink(Ui ui, Box glyph, int colour, boolean linked) {
        Theme t = ui.theme;
        ui.fillRound(glyph.inset(-1), 6, t.background);
        float ring = linked ? 10 : 7;
        float gap = linked ? -3 : 3;
        boolean v = vertical();
        float span = 2 * ring + gap;
        var paint = ui.strokePaint(colour, 1.5f);
        for (int i = 0; i < 2; i++) {
            float offset = i * (ring + gap);
            Box r = v
                    ? new Box(glyph.centerX() - span / 2 + offset, glyph.y + 2, ring, glyph.h - 4)
                    : new Box(glyph.x + 2, glyph.centerY() - span / 2 + offset, glyph.w - 4, ring);
            ui.canvas.drawRRect(RRect.makeXYWH(r.x, r.y, r.w, r.h, Math.min(r.w, r.h) / 2), paint);
        }
        ui.strokePaint(colour, 1);
    }

    /** The folded panel: a thin strip with its title, which unfolds on click. */
    private void strip(Ui ui, Box box) {
        Theme t = ui.theme;
        boolean hover = ui.hovered(box);
        if (ui.clicked("dock.strip." + title, box)) {
            toggle();
        }
        ui.fillRound(box, 6, hover ? t.panelRaised : t.panel);
        ui.strokeRound(box, 6, t.border);
        String label = title.toUpperCase();
        int colour = hover ? t.text : t.textDim;
        if (vertical()) {
            // reads bottom-to-top, starting just below the link
            float w = t.uiSmall.measureTextWidth(label);
            ui.canvas.save();
            ui.canvas.translate(box.centerX(), box.y + LINK_OFFSET + 16 + w / 2);
            ui.canvas.rotate(-90);
            ui.textCentred(new Box(-w / 2 - 4, -box.w / 2, w + 8, box.w), label, t.uiSmall, colour);
            ui.canvas.restore();
        } else {
            ui.text(box.inset(LINK_OFFSET + 26, 0), label, t.uiSmall, colour);
        }
        if (hover) {
            ui.setTooltip("Show " + title.toLowerCase());
        }
    }
}
