package com.rox.roundabout.memory;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.Ui;
import io.github.humbleui.skija.TextLine;

import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Virtualised hex view of a whole address space. Only visible rows are drawn; cell backgrounds
 * glow with write heat from {@link MemoryHeat}.
 */
public final class HexGrid {
    private static final int BYTES_PER_ROW = 16;
    private static final float ROW_HEIGHT = 20;
    private static final float HEADER_HEIGHT = 24;
    private static final float GUTTER_CHAR = 9; // mono, plus the gutter's 10px inset and a gap
    private static final float CELL_WIDTH = 26;
    private static final float GROUP_GAP = 8;
    private static final float ASCII_GAP = 18;
    private static final float ASCII_WIDTH = 9;
    private static final float SCROLLBAR_WIDTH = 8;

    private final List<MemoryRegion> regions;
    private final AddressFormat format;
    private final float gutterWidth;
    private final int rows;
    private TextLine[] hexText;
    private TextLine[] asciiText;

    private float scroll;
    private float targetScroll;
    private int visibleRows = 1;
    private int selected = 0;
    private int hovered = -1;
    private int activated = -1;

    public HexGrid(int size, List<MemoryRegion> regions, AddressFormat format) {
        this.rows = size / BYTES_PER_ROW;
        this.regions = regions;
        this.format = format;
        this.gutterWidth = 24 + GUTTER_CHAR * format.address(0).length();
    }

    public float preferredWidth() {
        return gutterWidth + BYTES_PER_ROW * CELL_WIDTH + GROUP_GAP + ASCII_GAP
                + BYTES_PER_ROW * ASCII_WIDTH + SCROLLBAR_WIDTH + 100;
    }

    public int selected() {
        return selected;
    }

    /** Address under the mouse this frame, or -1. */
    public int hovered() {
        return hovered;
    }

    /** Address double-clicked this frame, or -1. */
    public int activated() {
        return activated;
    }

    public int firstVisibleAddress() {
        return (int) scroll * BYTES_PER_ROW;
    }

    public int lastVisibleAddress() {
        return Math.min(rows, (int) scroll + visibleRows) * BYTES_PER_ROW - 1;
    }

    /** Scrolls so {@code address} is roughly centred. */
    public void scrollTo(int address) {
        targetScroll = clampScroll(address / BYTES_PER_ROW - visibleRows / 2f);
    }

    public void draw(Ui ui, Box area, MemoryHeat heat, float dt) {
        Theme t = ui.theme;
        ensureText(t);
        activated = -1;
        hovered = -1;

        Box box = area.copy();
        Box scrollbar = box.cutRight(SCROLLBAR_WIDTH);
        Box header = box.cutTop(HEADER_HEIGHT);
        visibleRows = Math.max(1, (int) (box.h / ROW_HEIGHT));

        handleInput(ui, box, scrollbar);
        scroll += (targetScroll - scroll) * Math.min(1f, dt * 18f);
        if (Math.abs(targetScroll - scroll) < 0.01f) {
            scroll = targetScroll;
        }

        int hoverCol = columnAt(box, ui.input.mouseX);
        int hoverRow = box.contains(ui.input.mouseX, ui.input.mouseY)
                ? (int) (scroll + (ui.input.mouseY - box.y) / ROW_HEIGHT) : -1;
        if (hoverCol >= 0 && hoverRow >= 0 && hoverRow < rows && ui.hovered(box)) {
            hovered = hoverRow * BYTES_PER_ROW + hoverCol;
        }

        drawHeader(ui, header, hovered >= 0 ? hovered % BYTES_PER_ROW : selected % BYTES_PER_ROW);
        ui.clip(box);
        int firstRow = (int) scroll;
        float offset = (scroll - firstRow) * ROW_HEIGHT;
        for (int r = 0; r <= visibleRows + 1; r++) {
            int row = firstRow + r;
            if (row >= rows) {
                break;
            }
            drawRow(ui, heat, row, box.y + r * ROW_HEIGHT - offset, box);
        }
        ui.unclip();

        if (hovered >= 0) {
            ui.setTooltip(tooltip(heat, hovered));
        }
    }

    private void handleInput(Ui ui, Box body, Box scrollbar) {
        var in = ui.input;
        if (body.contains(in.mouseX, in.mouseY) && in.scrollY != 0) {
            targetScroll = clampScroll(targetScroll - in.scrollY * 3);
        }
        float dragged = ui.scrollbar("hex.scrollbar", scrollbar, scroll, rows, visibleRows);
        if (dragged != scroll) {
            targetScroll = clampScroll(dragged);
            scroll = targetScroll;
        }
        if (in.mousePressed && ui.hovered(body)) {
            int col = columnAt(body, in.mouseX);
            int row = (int) (scroll + (in.mouseY - body.y) / ROW_HEIGHT);
            if (col >= 0 && row < rows) {
                selected = row * BYTES_PER_ROW + col;
                if (in.doubleClicked) {
                    activated = selected;
                }
            }
        }
        int move = 0;
        if (in.keyPressed(GLFW_KEY_LEFT)) move = -1;
        if (in.keyPressed(GLFW_KEY_RIGHT)) move = 1;
        if (in.keyPressed(GLFW_KEY_UP)) move = -BYTES_PER_ROW;
        if (in.keyPressed(GLFW_KEY_DOWN)) move = BYTES_PER_ROW;
        if (in.keyPressed(GLFW_KEY_PAGE_UP)) move = -BYTES_PER_ROW * visibleRows;
        if (in.keyPressed(GLFW_KEY_PAGE_DOWN)) move = BYTES_PER_ROW * visibleRows;
        if (in.keyPressed(GLFW_KEY_HOME)) move = -selected;
        if (in.keyPressed(GLFW_KEY_END)) move = rows * BYTES_PER_ROW - 1 - selected;
        if (in.keyPressed(GLFW_KEY_ENTER)) activated = selected;
        if (move != 0) {
            selected = Math.clamp(selected + move, 0, rows * BYTES_PER_ROW - 1);
            int row = selected / BYTES_PER_ROW;
            if (row < targetScroll) targetScroll = row;
            if (row >= targetScroll + visibleRows) targetScroll = row - visibleRows + 1;
            targetScroll = clampScroll(targetScroll);
        }
    }

    private float clampScroll(float s) {
        return Math.clamp(s, 0, Math.max(0, rows - visibleRows));
    }

    private int columnAt(Box body, float mx) {
        float x = mx - body.x - gutterWidth;
        if (x < 0) return -1;
        if (x < 8 * CELL_WIDTH) return (int) (x / CELL_WIDTH);
        x -= 8 * CELL_WIDTH + GROUP_GAP;
        if (x >= 0 && x < 8 * CELL_WIDTH) return 8 + (int) (x / CELL_WIDTH);
        x -= 8 * CELL_WIDTH + ASCII_GAP;
        if (x >= 0 && x < BYTES_PER_ROW * ASCII_WIDTH) return (int) (x / ASCII_WIDTH);
        return -1;
    }

    private float cellX(Box body, int col) {
        return body.x + gutterWidth + col * CELL_WIDTH + (col >= 8 ? GROUP_GAP : 0);
    }

    private float asciiX(Box body, int col) {
        return body.x + gutterWidth + BYTES_PER_ROW * CELL_WIDTH + GROUP_GAP + ASCII_GAP + col * ASCII_WIDTH;
    }

    private void drawHeader(Ui ui, Box header, int focusCol) {
        Theme t = ui.theme;
        for (int c = 0; c < BYTES_PER_ROW; c++) {
            Box cell = new Box(cellX(header, c), header.y, CELL_WIDTH, header.h);
            ui.textCentred(cell, String.format("%X", c), t.mono, c == focusCol ? t.accent : t.textFaint);
        }
        Box ascii = new Box(asciiX(header, 0), header.y, BYTES_PER_ROW * ASCII_WIDTH, header.h);
        ui.text(ascii, "ASCII", t.uiSmall, t.textFaint);
        ui.line(header.x, header.bottom() - 0.5f, header.right(), header.bottom() - 0.5f, t.border);
    }

    private void drawRow(Ui ui, MemoryHeat heat, int row, float y, Box body) {
        Theme t = ui.theme;
        int base = row * BYTES_PER_ROW;
        MemoryRegion region = MemoryRegion.find(regions, base);
        int regionColour = region == null ? t.textFaint : region.colour();
        boolean selectedRow = selected / BYTES_PER_ROW == row;
        boolean hoveredRow = hovered >= 0 && hovered / BYTES_PER_ROW == row;

        if (selectedRow || hoveredRow) {
            ui.fill(new Box(body.x, y, body.w, ROW_HEIGHT), Theme.withAlpha(t.text, selectedRow ? 0.035f : 0.02f));
        }
        if (region != null && region.start() == base && base != 0) {
            ui.line(body.x, y + 0.5f, body.right(), y + 0.5f, Theme.withAlpha(regionColour, 0.35f));
            Box label = new Box(asciiX(body, BYTES_PER_ROW) + 12, y, 200, ROW_HEIGHT);
            ui.text(label, region.name(), t.uiSmall, Theme.withAlpha(regionColour, 0.8f));
        }
        ui.fill(new Box(body.x, y + 2, 3, ROW_HEIGHT - 4), Theme.withAlpha(regionColour, 0.7f));
        Box gutter = new Box(body.x + 10, y, gutterWidth - 10, ROW_HEIGHT);
        ui.text(gutter, format.address(base), t.mono, selectedRow ? t.text : t.textDim);

        float baseline = Ui.baseline(new Box(0, y, 0, ROW_HEIGHT), t.mono);
        for (int c = 0; c < BYTES_PER_ROW; c++) {
            int address = base + c;
            int value = heat.value(address);
            float h = heat.heat(address);
            float x = cellX(body, c);
            if (h > 0.01f) {
                ui.fillRound(new Box(x + 1, y + 2, CELL_WIDTH - 2, ROW_HEIGHT - 4), 3, t.heat(h));
            }
            if (address == hovered) {
                ui.fillRound(new Box(x + 1, y + 2, CELL_WIDTH - 2, ROW_HEIGHT - 4), 3, Theme.withAlpha(t.text, 0.12f));
            }
            if (address == selected) {
                ui.strokeRound(new Box(x, y + 1, CELL_WIDTH, ROW_HEIGHT - 2), 4, t.accent);
            }
            int colour = value == 0 ? t.textFaint : Theme.mix(t.textDim, t.text, 0.55f + h);
            if (h > 0.75f) colour = 0xFFFFFFFF;
            TextLine text = hexText[value];
            ui.canvas.drawTextLine(text, x + (CELL_WIDTH - text.getWidth()) / 2, baseline, ui.paint(colour));

            float ax = asciiX(body, c);
            if (address == selected) {
                ui.fillRound(new Box(ax, y + 2, ASCII_WIDTH, ROW_HEIGHT - 4), 2, Theme.withAlpha(t.accent, 0.35f));
            } else if (h > 0.01f) {
                ui.fill(new Box(ax, y + 2, ASCII_WIDTH, ROW_HEIGHT - 4), Theme.withAlpha(t.warn, h * 0.15f));
            }
            boolean printable = value >= 0x20 && value < 0x7F;
            ui.canvas.drawTextLine(asciiText[value], ax, baseline, ui.paint(printable ? t.textDim : t.textFaint));
        }
    }

    private String tooltip(MemoryHeat heat, int address) {
        int v = heat.value(address);
        MemoryRegion region = MemoryRegion.find(regions, address);
        return String.format("%s  %s%n%s  %d  %s%nwrites %d", format.address(address),
                region == null ? "" : region.name(), format.value(v), v, format.binary(v), heat.writes(address));
    }

    private void ensureText(Theme t) {
        if (hexText != null) {
            return;
        }
        hexText = new TextLine[256];
        asciiText = new TextLine[256];
        for (int v = 0; v < 256; v++) {
            hexText[v] = TextLine.make(String.format("%02X", v), t.mono);
            asciiText[v] = TextLine.make(v >= 0x20 && v < 0x7F ? String.valueOf((char) v) : "·", t.mono);
        }
    }
}
