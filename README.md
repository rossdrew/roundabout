# Roundabout

What you lose on the Swings, you gain on the Roundabout: a GPU-rendered, immediate-mode UI toolkit for real-time debugging tools (Skija + LWJGL/GLFW), dark mode first.

Swing is great for forms, for dynamic data like memory activity in an emulator it's just too heavyweight.  A library for creating intuitive views over dynamic data.

```
./gradlew run                                  # standalone memory debugger preview (simulated NES data)
./gradlew run -Droundabout.screenshot=/tmp/ui.png   # render ~2s, save a PNG, exit
```

The memory map and the watches (list and history) are docked to the hex grid; click the link between two panels (or a folded strip) to fold or unfold one.

Keys: `Space` run/pause · `F` step frame · `I` step instruction · arrows / PgUp / PgDn / Home / End navigate · `W`, `Enter` or double-click adds a watch · `Del` removes it · double-clicking a watch jumps the hex view to it.

Embedding: implement `com.rox.roundabout.debugger.DebugTarget` and call `new MemoryDebugger(target, title).run()` (blocks; `close()` from any thread). The parts are reusable on their own: `MemoryPanel` (map + hex + watches over a `MemorySource`), `CpuPanel`, and `Dock` for any foldable, linked panel. EmuRoxLab does this in `NesRoundaboutTarget`.
