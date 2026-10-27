package com.rox.roundabout.debugger;

import com.rox.roundabout.debugger.DebugTarget.CpuState;
import com.rox.roundabout.debugger.DebugTarget.Flag;
import com.rox.roundabout.debugger.DebugTarget.Register;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CpuPanelTest {
    private static final List<Register> MOS_6502 = List.of(new Register("PC", 0x8000, 16), new Register("A", 1, 8),
            new Register("X", 2, 8), new Register("Y", 3, 8), new Register("SP", 0xFD, 8), new Register("P", 0x24, 8));

    @Test
    void registersShowOneHexDigitPerFourBits() {
        assertEquals("8000", new Register("PC", 0x8000, 16).hex());
        assertEquals("0F", new Register("A", 0x0F, 8).hex());
        assertEquals("FFF", new Register("R", -1, 12).hex(), "bits above the width are ignored");
        assertEquals("1", new Register("C", 1, 1).hex());
        assertEquals("FFFFFFFFFFFFFFFF", new Register("RAX", -1, 64).hex());
        assertThrows(IllegalArgumentException.class, () -> new Register("R", 0, 0));
    }

    @Test
    void flagsFromBitsReadMostSignificantFirst() {
        List<Flag> flags = Flag.fromBits("NV-BDIZC", 0xA1);

        assertEquals(List.of(new Flag("N", true), new Flag("V", false), new Flag("-", true), new Flag("B", false),
                new Flag("D", false), new Flag("I", false), new Flag("Z", false), new Flag("C", true)), flags);
    }

    @Test
    void a6502FitsOneRowOfRegistersAndOneOfFlags() {
        CpuState cpu = new CpuState(MOS_6502, Flag.fromBits("NV-BDIZC", 0x24));

        assertEquals(1, CpuPanel.rows(cpu.registers(), 324).size());
        assertEquals(44 + 48 + 10 + 26, CpuPanel.height(340, cpu));
    }

    @Test
    void wideRegistersWrapOntoMoreRows() {
        List<Register> registers = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            registers.add(new Register("R" + i, i, 32));
        }

        List<List<Register>> rows = CpuPanel.rows(registers, 324);

        assertEquals(List.of(3, 3, 2), rows.stream().map(List::size).toList());
        assertEquals(44 + 3 * 48 + 2 * 6, CpuPanel.height(340, new CpuState(registers, List.of())));
    }

    @Test
    void manyFlagsWrap() {
        CpuState cpu = new CpuState(List.of(new Register("PC", 0, 16)), Flag.fromBits("ABCDEFGHIJKLMNOP", 0));

        assertEquals(44 + 48 + 10 + 2 * 26 + 4, CpuPanel.height(340, cpu));
    }

    @Test
    void aRowFitsExactlyItsWidth() {
        List<Register> two = List.of(new Register("A", 0, 8), new Register("X", 0, 8)); // 42 wide each, 6 apart

        assertEquals(1, CpuPanel.rows(two, 90).size());
        assertEquals(2, CpuPanel.rows(two, 89).size());
        assertEquals(List.of(), CpuPanel.rows(List.of(), 90));
    }

    @Test
    void heightAllowsForThePanelInset() {
        List<Register> two = List.of(new Register("A", 0, 8), new Register("X", 0, 8));

        assertEquals(44 + 48, CpuPanel.height(106, new CpuState(two, List.of())));
        assertEquals(44 + 2 * 48 + 6, CpuPanel.height(105, new CpuState(two, List.of())));
    }

    @Test
    void flagsFillTheirRowExactly() {
        CpuState eight = new CpuState(List.of(), Flag.fromBits("NV-BDIZC", 0)); // 24 wide each, 4 apart

        assertEquals(44 + 10 + 26, CpuPanel.height(16 + 220, eight));
        assertEquals(44 + 10 + 2 * 26 + 4, CpuPanel.height(16 + 219, eight));
    }
}
