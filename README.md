![Roundabout](resources/roundabout-logo-200.png)
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
    implementation("com.rox:roundabout:0.1.0")
}
```

Skija, LWJGL and the Linux x64 natives come with it. Run the app with `--enable-native-access=ALL-UNNAMED`.

To release: bump `version` in `build.gradle.kts`, merge to `main`, and publish a GitHub release tagged `v<version>`; the `Publish` workflow tests and publishes it. `./gradlew publishToMavenLocal` installs a build locally for trying changes out.

![Screenshot running inside EmuRox](/resources/state_221026.png)