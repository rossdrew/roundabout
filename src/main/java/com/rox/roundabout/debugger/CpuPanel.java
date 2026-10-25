package com.rox.roundabout.debugger;

import com.rox.roundabout.Box;
import com.rox.roundabout.Theme;
import com.rox.roundabout.Ui;
import com.rox.roundabout.debugger.DebugTarget.CpuState;
import com.rox.roundabout.debugger.DebugTarget.Register;

import java.util.ArrayList;
import java.util.List;

/**
 * A target's registers as readout tiles, wrapped into rows and sized by width, with its flags as chips below.
 * Use {@link #height} to size the panel for the target's CPU.
 */
public final class CpuPanel {
    private static final float HEADER = 28 + 2 * 8; // ui.panel's title bar and content inset
    private static final float REGISTER_ROW = 48;
    private static final float FLAG_ROW = 26;
    private static final float GAP = 6;
    private static final float FLAG_GAP = 4;
    private static final float SECTION_GAP = 10;
    private static final float DIGIT_WIDTH = 10; // monoLarge, with room for the tile's padding
    private static final float TILE_PADDING = 22;
    private static final float MIN_FLAG_WIDTH = 24;

    private CpuPanel() {
    }

    /** The panel height that fits {@code cpu}'s registers and flags at {@code width}. */
    public static float height(float width, CpuState cpu) {
        float content = width - 16;
        int registerRows = rows(cpu.registers(), content).size();
        int flagRows = flagRows(cpu.flags().size(), content);
        float h = HEADER + stack(registerRows, REGISTER_ROW, GAP);
        if (flagRows > 0) {
            h += SECTION_GAP + stack(flagRows, FLAG_ROW, FLAG_GAP);
        }
        return h;
    }

    public static void draw(Ui ui, Box area, CpuState cpu) {
        Theme t = ui.theme;
        Box content = ui.panel(area, "CPU");
        boolean first = true;
        for (List<Register> row : rows(cpu.registers(), content.w)) {
            if (!first) {
                content.cutTop(GAP);
            }
            Box line = content.cutTop(REGISTER_ROW);
            // stretch the tiles to fill the row, keeping their proportions
            float gaps = GAP * (row.size() - 1);
            float scale = (line.w - gaps) / (minWidth(row) - gaps);
            for (int i = 0; i < row.size(); i++) {
                Register r = row.get(i);
                ui.readout(line.cutLeft(tileWidth(r) * scale), r.name(), r.hex(), first && i == 0 ? t.accent : t.text);
                line.cutLeft(GAP);
            }
            first = false;
        }

        List<DebugTarget.Flag> flags = cpu.flags();
        int perRow = flagsPerRow(content.w);
        if (!flags.isEmpty()) {
            content.cutTop(SECTION_GAP);
        }
        for (int start = 0; start < flags.size(); start += perRow) {
            if (start > 0) {
                content.cutTop(FLAG_GAP);
            }
            Box line = content.cutTop(FLAG_ROW);
            int count = Math.min(perRow, flags.size() - start);
            float w = (line.w - FLAG_GAP * (count - 1)) / count;
            for (int i = start; i < start + count; i++) {
                ui.chip(line.cutLeft(w), flags.get(i).name(), flags.get(i).set());
                line.cutLeft(FLAG_GAP);
            }
        }
    }

    /** Greedily packs registers into rows no wider than {@code width} at their minimum tile widths. */
    static List<List<Register>> rows(List<Register> registers, float width) {
        List<List<Register>> rows = new ArrayList<>();
        List<Register> row = new ArrayList<>();
        float used = 0;
        for (Register r : registers) {
            float w = tileWidth(r);
            if (!row.isEmpty() && used + GAP + w > width) {
                rows.add(row);
                row = new ArrayList<>();
                used = 0;
            }
            used += (row.isEmpty() ? 0 : GAP) + w;
            row.add(r);
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }

    private static float tileWidth(Register r) {
        return TILE_PADDING + DIGIT_WIDTH * Math.max(2, r.hex().length());
    }

    private static float minWidth(List<Register> row) {
        float w = GAP * (row.size() - 1);
        for (Register r : row) {
            w += tileWidth(r);
        }
        return w;
    }

    private static int flagsPerRow(float width) {
        return Math.max(1, (int) ((width + FLAG_GAP) / (MIN_FLAG_WIDTH + FLAG_GAP)));
    }

    private static int flagRows(int flags, float width) {
        int perRow = flagsPerRow(width);
        return (flags + perRow - 1) / perRow;
    }

    private static float stack(int rows, float rowHeight, float gap) {
        return rows == 0 ? 0 : rows * rowHeight + (rows - 1) * gap;
    }
}
