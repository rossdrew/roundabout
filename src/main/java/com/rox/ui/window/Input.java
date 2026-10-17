package com.rox.ui.window;

import java.util.HashSet;
import java.util.Set;

/**
 * Input state for one frame. Positions are in logical (DPI-independent) pixels.
 * "Pressed"/"released" flags are edges that last exactly one frame.
 */
public final class Input {
    private static final double DOUBLE_CLICK_SECONDS = 0.35;

    public float mouseX = -1, mouseY = -1;
    public boolean mouseDown;
    public boolean mousePressed;
    public boolean mouseReleased;
    public boolean doubleClicked;
    public float scrollY;

    private final Set<Integer> keysPressed = new HashSet<>();
    private double lastClickTime = -1;

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
}
