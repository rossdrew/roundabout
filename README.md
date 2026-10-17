# roxUI

GPU-rendered, immediate-mode UI toolkit for real-time debugging tools (Skija + LWJGL/GLFW), dark mode first.

```
./gradlew run                                  # standalone memory debugger preview (simulated NES data)
./gradlew run -Droxui.screenshot=/tmp/ui.png   # render ~2s, save a PNG, exit
```

Keys: `Space` run/pause · `F` step frame · arrows / PgUp / PgDn / Home / End navigate · `W`, `Enter` or double-click adds a watch.
