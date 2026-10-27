# Changelog

## 0.3.0 (unreleased)

API tidy before going public. Breaking:

- **Packages by feature:** the `data` package is gone. `chart`: `Graphs`, `RingSeries`. `memory`: `MemorySource`, `MemoryRegion`, `MemoryHeat`, `AddressFormat`, `HexGrid`, `MemoryMap`, `MemoryPanel` (from `debugger`). `timing`: `FrameTimes`, `FrameTimingGraph`.
- `DebugTarget.Label` → `memory.NamedAddress`, so `memory` doesn't depend on `debugger`.

## 0.2.0

Generalised for any emulator or CPU simulator. Breaking changes to `DebugTarget` and the widgets:

- **CPU:** `CpuState(pc, a, x, y, sp, p)` → `CpuState(List<Register>, List<Flag>)`. `Register(name, value, bits)`, `Flag(name, set)`, and `Flag.fromBits("NV-BDIZC", p)` for status bytes. `CpuPanel` wraps registers and flags and reports its `height(width, cpu)`.
- **Run controls:** `stepFrame()` / `stepInstruction()` → `List<Step> steps()`, each `Step(label, key, run)`. Buttons, keys and hints are built from them.
- **Addresses:** padded to the memory's size via `AddressFormat`; `0x`/`0b` by default, `$`/`%` with `numberStyle()` → `AddressFormat.Style.MOTOROLA`. `HexGrid` and `MemoryMap` take an `AddressFormat`; the byte-only `Graphs.lineGraph` overload is gone.
- **Timing:** `FrameTimes(name, history, budgetMs)` carries its own budget; `DebugTarget.frameBudgetMs()` is gone and `FrameTimingGraph.draw(ui, area, times)` reads it. Watch history is sampled by real time, not per UI frame.
- **NES:** `NesMemoryMap` is no longer in the library (it's in the demo).
- Panel headings upper-case only the name before the first ` · `.
- MIT licensed.

## 0.1.0

First release.
