package com.rox.ui.debugger;

import com.rox.ui.Box;
import com.rox.ui.Theme;
import com.rox.ui.Ui;

/** 6502-style registers and NV-BDIZC flags. */
public final class CpuPanel {
    private CpuPanel() {
    }

    public static void draw(Ui ui, Box area, DebugTarget.CpuState cpu) {
        Theme t = ui.theme;
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
}
