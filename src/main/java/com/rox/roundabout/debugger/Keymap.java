package com.rox.roundabout.debugger;

import com.rox.roundabout.memory.HexGrid;
import com.rox.roundabout.window.Input;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The {@link MemoryDebugger}'s own shortcut keys, as {@code GLFW_KEY_*} codes; {@code GLFW_KEY_UNKNOWN}
 * turns one off. Change them when they clash with the host app. The hex view's navigation keys
 * ({@link HexGrid#NAVIGATION_KEYS}) are fixed.
 *
 * @param runPause toggles run/pause
 * @param watch    watches the selected byte
 * @param unwatch  removes the selected watch
 */
public record Keymap(int runPause, int watch, int unwatch) {
    /** Space run/pause, W watch, Del unwatch. */
    public static final Keymap DEFAULT = new Keymap(GLFW_KEY_SPACE, GLFW_KEY_W, GLFW_KEY_DELETE);

    public Keymap {
        List<Integer> seen = new ArrayList<>();
        for (int key : new int[]{runPause, watch, unwatch}) {
            if (key == GLFW_KEY_UNKNOWN) {
                continue;
            }
            if (seen.contains(key)) {
                throw new IllegalArgumentException(Input.keyName(key) + " is bound twice");
            }
            if (HexGrid.NAVIGATION_KEYS.contains(key)) {
                throw new IllegalArgumentException(Input.keyName(key) + " is a hex navigation key");
            }
            seen.add(key);
        }
    }

    /** Throws if a step's key is already one of these keys or the hex view's navigation keys. */
    void checkNoClash(List<DebugTarget.Step> steps) {
        for (DebugTarget.Step step : steps) {
            int key = step.key();
            if (key == GLFW_KEY_UNKNOWN) {
                continue;
            }
            String bound = key == runPause ? "run/pause" : key == watch ? "watch" : key == unwatch ? "unwatch"
                    : HexGrid.NAVIGATION_KEYS.contains(key) ? "hex navigation" : null;
            if (bound != null) {
                throw new IllegalArgumentException(
                        "\"" + step.label() + "\" uses " + Input.keyName(key) + ", already bound to " + bound);
            }
        }
    }
}
