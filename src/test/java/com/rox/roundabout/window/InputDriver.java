package com.rox.roundabout.window;

/**
 * Feeds an {@link Input} the events {@link GlfwWindow} would, for tests. Time advances 1/60 s per
 * {@link #endFrame}, so presses a frame or two apart count as a double-click.
 */
public final class InputDriver {
    private final Input input = new Input();
    private double time = 10;

    public Input input() {
        return input;
    }

    public InputDriver move(float x, float y) {
        input.mouseX = x;
        input.mouseY = y;
        return this;
    }

    public InputDriver press() {
        input.onMouseButton(true, time);
        return this;
    }

    public InputDriver release() {
        input.onMouseButton(false, time);
        return this;
    }

    public InputDriver key(int glfwKey) {
        input.onKey(glfwKey);
        return this;
    }

    public InputDriver scroll(double dy) {
        input.onScroll(dy);
        return this;
    }

    /** Lets {@code seconds} pass, e.g. to space presses further apart than a double-click. */
    public InputDriver wait(double seconds) {
        time += seconds;
        return this;
    }

    /** Clears this frame's edges, as the window does after drawing. */
    public void endFrame() {
        input.endFrame();
        time += 1 / 60.0;
    }
}
