![Roundabout](resources/roundabout-logo-200.png)
# Roundabout

What you lose on the Swings, you gain on the Roundabout: a GPU-rendered, immediate-mode UI toolkit for real-time debugging tools (Skija + LWJGL/GLFW), dark mode first.

Swing is great for forms, for dynamic data like memory activity in an emulator it's just too heavyweight.  A library for creating intuitive views over dynamic data.

It comes with a ready-made debugger window for emulators and CPU simulators: a live hex view with write heat, a memory map, watches with history, CPU registers and flags, frame timing and run controls. Nothing in it is tied to one machine; the target describes its own memory regions, registers, flags and step controls.

```
./gradlew run                                  # the debugger on a simulated NES (the demo isn't part of the library)
./gradlew run -Droundabout.screenshot=/tmp/ui.png   # render ~2s, save a PNG, exit
```

The memory map and the watches (list and history) are docked to the hex grid; click the link between two panels (or a folded strip) to fold or unfold one.

Keys: `Space` run/pause · each of the target's steps on its own key (the demo: `F` step frame, `I` step instruction) · arrows / PgUp / PgDn / Home / End navigate · `W`, `Enter` or double-click adds a watch · `Del` removes it · double-clicking a watch jumps the hex view to it.

## Embedding the debugger

Implement `com.rox.roundabout.debugger.DebugTarget` over your emulator and call `new MemoryDebugger(target, title).run()` (blocks; `close()` from any thread). A sketch for an 8-bit CPU:

```java
class MyTarget implements DebugTarget, MemorySource {
    private final List<Step> steps = List.of(
            new Step("Step frame", GLFW_KEY_F, machine::runFrame),
            new Step("Step instr", GLFW_KEY_I, machine::step));

    public MemorySource memory() { return this; }
    public int size() { return 0x10000; }
    public int peek(int address) { return machine.bus().peek(address); } // no side effects

    public List<MemoryRegion> regions() {
        return List.of(new MemoryRegion("RAM", 0x0000, 0x7FFF, 0xFF89DDFF),
                       new MemoryRegion("ROM", 0x8000, 0xFFFF, 0xFF7AA2F7));
    }

    public CpuState cpu() {
        Cpu c = machine.cpu();
        return new CpuState(List.of(new Register("PC", c.pc(), 16), new Register("A", c.a(), 8)),
                            Flag.fromBits("NV-BDIZC", c.status()));
    }

    public List<Step> steps() { return steps; }
    public boolean isPaused() { return machine.isPaused(); }
    public void pause() { machine.pause(); }
    public void resume() { machine.resume(); }
    public String status() { return machine.cycles() + " cycles"; }
}
```

Optional: `labels()` for watches to start with, `frameTimings()` for frame-timing streams (each `FrameTimes` with its own budget), and `numberStyle()` for `$`/`%` instead of `0x`/`0b`. The demo's `FakeNes` is a complete example, and [EmuRox](https://github.com/rossdrew/EmuRoxLab) uses it for its NES.

The parts are reusable on their own: `MemoryPanel` (map + hex + watches over a `MemorySource`), `CpuPanel`, `FrameTimingGraph`, `Graphs`, and `Dock` for any foldable, linked panel.

## Using it as a library

Releases are published to GitHub Packages as `com.rox:roundabout` (the demo isn't included). GitHub Packages needs a token to read, even for public packages: create one with `read:packages` and put it in `~/.gradle/gradle.properties`:

```
gpr.user=<github username>
gpr.key=<token>
```

then in the consuming build:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/rossdrew/roundabout")
        credentials {
            username = providers.gradleProperty("gpr.user").get()
            password = providers.gradleProperty("gpr.key").get()
        }
    }
}

dependencies {
    implementation("com.rox:roundabout:0.2.0")
}
```

Skija, LWJGL and the Linux x64 natives come with it (Linux only for now). Run the app with `--enable-native-access=ALL-UNNAMED`.

To release: bump `version` in `build.gradle.kts`, merge to `main`, and publish a GitHub release tagged `v<version>`; the `Publish` workflow tests and publishes it. `./gradlew publishToMavenLocal` installs a build locally for trying changes out.

![Roundabout debugging an NES in EmuRox](/resources/state_221026.png)

## License

[MIT](LICENSE)
