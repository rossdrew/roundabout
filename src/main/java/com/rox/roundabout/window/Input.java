package com.rox.roundabout.window;

import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Input state for one frame. Positions are in logical (DPI-independent) pixels.
 * "Pressed"/"released" flags are edges that last exactly one frame.
 */
public final class Input {
    private static final double DOUBLE_CLICK_SECONDS = 0.35;

    /** Mouse position, or -1 when it's outside the window. */
    public float mouseX = -1, mouseY = -1;
    /** True while the left mouse button is down. */
    public boolean mouseDown;
    /** True on the frame the left button goes down. */
    public boolean mousePressed;
    /** True on the frame the left button goes up. */
    public boolean mouseReleased;
    /** True on the frame of a second press in quick succession; {@link #mousePressed} is also true. */
    public boolean doubleClicked;
    /** Scroll this frame, in wheel steps; positive is up. */
    public float scrollY;

    private final Set<Integer> keysPressed = new HashSet<>();
    private double lastClickTime = -1;

    /** Nothing pressed, with the mouse outside the window. */
    public Input() {
    }

    /** True on the frame {@code glfwKey} ({@code GLFW_KEY_*}) was pressed or repeated. */
    public boolean keyPressed(int glfwKey) {
        return keysPressed.contains(glfwKey);
    }

    void onMouseButton(boolean down, double time) {
        if (down) {
            mousePressed = true;
            doubleClicked = time - lastClickTime < DOUBLE_CLICK_SECONDS;
            lastClickTime = doubleClicked ? -1 : time;
        } else {
            mouseReleased = true;
        }
        mouseDown = down;
    }

    void onKey(int key) {
        keysPressed.add(key);
    }

    void onScroll(double dy) {
        scrollY += (float) dy;
    }

    void endFrame() {
        mousePressed = false;
        mouseReleased = false;
        doubleClicked = false;
        scrollY = 0;
        keysPressed.clear();
    }

    /** A short display name for a {@code GLFW_KEY_*} code, e.g. "F", "7", "F5", "Enter". */
    public static String keyName(int key) {
        if ((key >= GLFW_KEY_A && key <= GLFW_KEY_Z) || (key >= GLFW_KEY_0 && key <= GLFW_KEY_9)) {
            return String.valueOf((char) key);
        }
        if (key >= GLFW_KEY_F1 && key <= GLFW_KEY_F25) {
            return "F" + (key - GLFW_KEY_F1 + 1);
        }
        return switch (key) {
            case GLFW_KEY_SPACE -> "Space";
            case GLFW_KEY_ENTER -> "Enter";
            case GLFW_KEY_TAB -> "Tab";
            case GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW_KEY_INSERT -> "Ins";
            case GLFW_KEY_DELETE -> "Del";
            case GLFW_KEY_HOME -> "Home";
            case GLFW_KEY_END -> "End";
            case GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW_KEY_LEFT -> "←";
            case GLFW_KEY_RIGHT -> "→";
            case GLFW_KEY_UP -> "↑";
            case GLFW_KEY_DOWN -> "↓";
            case GLFW_KEY_PERIOD -> ".";
            case GLFW_KEY_COMMA -> ",";
            case GLFW_KEY_SLASH -> "/";
            case GLFW_KEY_MINUS -> "-";
            case GLFW_KEY_EQUAL -> "=";
            default -> "key " + key;
        };
    }
}
