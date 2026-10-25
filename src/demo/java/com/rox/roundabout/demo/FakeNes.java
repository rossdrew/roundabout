package com.rox.roundabout.demo;

import com.rox.roundabout.data.FrameTimes;
import com.rox.roundabout.data.MemoryRegion;
import com.rox.roundabout.data.MemorySource;
import com.rox.roundabout.debugger.DebugTarget;
import com.rox.roundabout.debugger.NesMemoryMap;

import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_I;

/**
 * Stand-in for the emulator: a 64K NES-like address space with a background thread that
 * writes plausible game activity at 60 frames per second.
 */
final class FakeNes implements MemorySource, DebugTarget {
    // the fake has no instruction granularity, so both advance a frame
    private final List<Step> steps = List.of(new Step("Step frame", GLFW_KEY_F, this::stepFrame),
            new Step("Step instr", GLFW_KEY_I, this::stepFrame));
    private static final int SIZE = 0x10000;


    private final int[] ram = new int[0x800];
    private final int[] ppuRegs = new int[8];
    private final int[] apu = new int[0x20];
    private final int[] prgRam = new int[0x2000];
    private final int[] prgRom = new int[0x8000];
    private final Random rng = new Random(6502);

    private final Object lock = new Object();
    private final FrameTimes frameTimes = new FrameTimes("emu frames", 600);
    private long simulatedFrames;
    private volatile boolean paused;
    private volatile long frames;
    private volatile double measuredFps;

    // fake CPU registers
    volatile int pc = 0xC000, a, x, y, sp = 0xFD, p = 0x24;

    FakeNes() {
        for (int i = 0; i < prgRom.length; i++) {
            prgRom[i] = rng.nextInt(10) < 3 ? rng.nextInt(256) : OPCODES[rng.nextInt(OPCODES.length)];
        }
        String title = "ROUNDABOUT SAVE ";
        for (int i = 0; i < title.length(); i++) {
            prgRam[i] = title.charAt(i);
        }
        for (int i = 0x10; i < 0x200; i++) {
            prgRam[i] = rng.nextInt(4) == 0 ? rng.nextInt(256) : 0;
        }
        for (int i = 0x400; i < 0x800; i++) {
            ram[i] = rng.nextInt(3) == 0 ? 0x20 + rng.nextInt(16) : 0;
        }
        // reset/NMI/IRQ vectors
        prgRom[0x7FFA] = 0x00; prgRom[0x7FFB] = 0xC1;
        prgRom[0x7FFC] = 0x00; prgRom[0x7FFD] = 0xC0;
        prgRom[0x7FFE] = 0x80; prgRom[0x7FFF] = 0xC1;
        ram[0x75] = 3;
        ram[0x76] = 0xC0;
    }

    void start() {
        Thread thread = new Thread(this::runLoop, "fake-nes");
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        long next = System.nanoTime();
        long windowStart = next, windowFrames = 0;
        while (true) {
            synchronized (lock) {
                while (paused) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                frame();
            }
            frameTimes.record(System.nanoTime());
            if (++simulatedFrames % 300 == 0) {
                next += 30_000_000L; // a simulated 30ms hitch every ~5s, so the frame-timing graph has something to show
            }
            windowFrames++;
            long now = System.nanoTime();
            if (now - windowStart > 500_000_000L) {
                measuredFps = windowFrames * 1e9 / (now - windowStart);
                windowStart = now;
                windowFrames = 0;
            }
            next += 16_639_267L; // NTSC ~60.0988 Hz
            long sleep = next - System.nanoTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep / 1_000_000, (int) (sleep % 1_000_000));
                } catch (InterruptedException e) {
                    return;
                }
            } else {
                next = System.nanoTime();
            }
        }
    }

    // ---- DebugTarget ------------------------------------------------------------------------

    @Override
    public List<FrameTimes> frameTimings() {
        return List.of(frameTimes);
    }

    @Override
    public MemorySource memory() {
        return this;
    }

    @Override
    public List<MemoryRegion> regions() {
        return NesMemoryMap.REGIONS;
    }

    @Override
    public List<Label> labels() {
        return List.of(
                new Label("Frame counter", 0x0010),
                new Label("Player X", 0x0030),
                new Label("Player Y", 0x0031),
                new Label("Enemy 0 Y", 0x0041),
                new Label("Health", 0x0076),
                new Label("RNG lo", 0x0080),
                new Label("PPUCTRL", 0x2000));
    }

    @Override
    public CpuState cpu() {
        return new CpuState(
                List.of(new Register("PC", pc, 16), new Register("A", a, 8), new Register("X", x, 8),
                        new Register("Y", y, 8), new Register("SP", sp, 8), new Register("P", p, 8)),
                Flag.fromBits("NV-BDIZC", p));
    }

    @Override
    public void pause() {
        setPaused(true);
    }

    @Override
    public void resume() {
        setPaused(false);
    }

    @Override
    public List<Step> steps() {
        return steps;
    }

    @Override
    public String status() {
        return String.format("frame %,d  ·  %.1f fps  ·  simulated data", frames, fps());
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    private void setPaused(boolean value) {
        synchronized (lock) {
            paused = value;
            if (!value) {
                lock.notifyAll();
            }
        }
    }

    private void stepFrame() {
        synchronized (lock) {
            if (paused) {
                frame();
            }
        }
    }

    long frames() {
        return frames;
    }

    double fps() {
        return paused ? 0 : measuredFps;
    }

    // ---- simulated game ---------------------------------------------------------------------

    private void frame() {
        long f = ++frames;
        double t = f / 60.0;

        // scratch temporaries
        for (int i = 0; i < 6; i++) {
            ram[rng.nextInt(16)] = rng.nextInt(256);
        }
        ram[0x10] = (int) (f & 0xFF);
        ram[0x11] = (int) ((f >> 8) & 0xFF);
        if (f % 45 == 0) {
            ram[0x12] = 1 << rng.nextInt(8);
        }

        // player
        int px = (int) (128 + 100 * Math.sin(t * 0.8));
        int py = (int) (120 + 60 * Math.sin(t * 1.3 + 1));
        ram[0x32] = (px - ram[0x30]) & 0xFF;
        ram[0x30] = px;
        ram[0x31] = py;
        ram[0x33] = (int) ((f / 8) % 4);

        // enemies: 8 x {x, y, state, timer}
        for (int e = 0; e < 8; e++) {
            int base = 0x40 + e * 4;
            ram[base] = (ram[base] + 1 + e % 3) & 0xFF;
            ram[base + 1] = (int) (60 + 40 * Math.sin(t * (0.5 + e * 0.2) + e)) & 0xFF;
            if (--ram[base + 3] < 0) {
                ram[base + 3] = 30 + rng.nextInt(90);
                ram[base + 2] = rng.nextInt(4);
            }
        }

        // score (BCD), lives, health
        if (f % 20 == 0) {
            bcdIncrement(0x70, 3);
        }
        ram[0x76] = Math.clamp(ram[0x76] + (rng.nextInt(10) < 3 ? -3 : 1), 0, 0xFF);

        // LFSR RNG
        int lfsr = ram[0x80] | (ram[0x81] << 8);
        if (lfsr == 0) lfsr = 0xACE1;
        for (int i = 0; i < 8; i++) {
            int bit = ((lfsr >> 0) ^ (lfsr >> 2) ^ (lfsr >> 3) ^ (lfsr >> 5)) & 1;
            lfsr = (lfsr >> 1) | (bit << 15);
        }
        ram[0x80] = lfsr & 0xFF;
        ram[0x81] = lfsr >> 8;

        // stack churn
        int depth = 3 + (int) (6 * (0.5 + 0.5 * Math.sin(t * 3)));
        sp = 0xFD - depth * 2;
        for (int i = 0; i < depth; i++) {
            int ret = 0xC000 + rng.nextInt(0x400);
            ram[0x100 + 0xFD - i * 2] = ret >> 8;
            ram[0x100 + 0xFC - i * 2] = ret & 0xFF;
        }

        // OAM shadow buffer rebuilt each frame
        writeSprite(0, py, 0x01 + ram[0x33], 0x00, px);
        writeSprite(1, py, 0x11 + ram[0x33], 0x00, px + 8);
        for (int e = 0; e < 8; e++) {
            int base = 0x40 + e * 4;
            writeSprite(2 + e, ram[base + 1], 0x40 + ram[base + 2], 0x01, ram[base]);
        }
        for (int s = 10; s < 64; s++) {
            ram[0x200 + s * 4] = 0xF8;
        }

        // sound engine: bursts every half second
        if (f % 30 < 4) {
            for (int i = 0; i < 12; i++) {
                ram[0x300 + rng.nextInt(0x40)] = rng.nextInt(256);
            }
            for (int i = 0; i < 4; i++) {
                apu[rng.nextInt(0x18)] = rng.nextInt(256);
            }
        }
        // occasional level tile updates
        if (rng.nextInt(6) == 0) {
            ram[0x400 + rng.nextInt(0x400)] = 0x20 + rng.nextInt(16);
        }

        // PPU registers
        ppuRegs[0] = 0x90 | (f % 120 < 60 ? 0 : 1);
        ppuRegs[1] = 0x1E;
        ppuRegs[2] = (int) ((f & 1) << 7);
        ppuRegs[5] = (int) (f * 2 & 0xFF);
        ppuRegs[6] = 0x20 + rng.nextInt(4);
        ppuRegs[7] = rng.nextInt(256);

        // occasional save-RAM write
        if (f % 240 == 0) {
            prgRam[0x10 + rng.nextInt(0x1F0)] = rng.nextInt(256);
        }

        pc = 0xC000 + rng.nextInt(0x400);
        a = ram[0x30];
        x = rng.nextInt(64);
        y = rng.nextInt(16);
        p = 0x20 | rng.nextInt(256) & 0xC3 | (a == 0 ? 0x02 : 0);
    }

    private void writeSprite(int index, int y, int tile, int attr, int x) {
        int base = 0x200 + index * 4;
        ram[base] = y & 0xFF;
        ram[base + 1] = tile & 0xFF;
        ram[base + 2] = attr;
        ram[base + 3] = x & 0xFF;
    }

    private void bcdIncrement(int address, int bytes) {
        for (int i = bytes - 1; i >= 0; i--) {
            int v = ram[address + i] + 1;
            if ((v & 0x0F) > 9) v += 6;
            if (v > 0x99) {
                ram[address + i] = 0;
            } else {
                ram[address + i] = v;
                return;
            }
        }
    }

    // ---- MemorySource -----------------------------------------------------------------------

    @Override
    public int size() {
        return SIZE;
    }

    @Override
    public int peek(int address) {
        if (address < 0x2000) return ram[address & 0x7FF];
        if (address < 0x4000) return ppuRegs[address & 7];
        if (address < 0x4020) return apu[address - 0x4000];
        if (address < 0x6000) return 0;
        if (address < 0x8000) return prgRam[address - 0x6000];
        return prgRom[address - 0x8000];
    }

    @Override
    public boolean isEditable(int address) {
        return address < 0x2000 || (address >= 0x6000 && address < 0x8000);
    }

    @Override
    public void poke(int address, int value) {
        if (address < 0x2000) ram[address & 0x7FF] = value & 0xFF;
        else if (address >= 0x6000 && address < 0x8000) prgRam[address - 0x6000] = value & 0xFF;
    }

    private static final int[] OPCODES = {
            0xA9, 0xA5, 0xAD, 0x85, 0x8D, 0xA2, 0xA0, 0xE8, 0xC8, 0xCA, 0x88, 0x4C, 0x20, 0x60,
            0xD0, 0xF0, 0x10, 0x30, 0x69, 0xE9, 0x29, 0x09, 0x0A, 0x4A, 0xC9, 0xE0, 0x18, 0x38, 0xEA,
    };
}
