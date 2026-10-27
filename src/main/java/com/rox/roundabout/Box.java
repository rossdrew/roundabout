package com.rox.roundabout;

import io.github.humbleui.types.Rect;

/**
 * A mutable layout rectangle using the "rect cut" approach: each cut slices a strip
 * off one edge, shrinks this box and returns the strip.
 */
public final class Box {
    /** Left, top, width and height, in logical pixels. */
    public float x, y, w, h;

    /** A box; negative sizes are clamped to 0. */
    public Box(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0, w);
        this.h = Math.max(0, h);
    }

    /** An independent copy, to cut from without changing this box. */
    public Box copy() {
        return new Box(x, y, w, h);
    }

    /** The right edge, {@code x + w}. */
    public float right() {
        return x + w;
    }

    /** The bottom edge, {@code y + h}. */
    public float bottom() {
        return y + h;
    }

    /** The horizontal centre. */
    public float centerX() {
        return x + w / 2;
    }

    /** The vertical centre. */
    public float centerY() {
        return y + h / 2;
    }

    /** Cuts a strip {@code amount} high (at most this box's height) off the top and returns it. */
    public Box cutTop(float amount) {
        float a = Math.min(amount, h);
        Box cut = new Box(x, y, w, a);
        y += a;
        h -= a;
        return cut;
    }

    /** Cuts a strip {@code amount} high (at most this box's height) off the bottom and returns it. */
    public Box cutBottom(float amount) {
        float a = Math.min(amount, h);
        h -= a;
        return new Box(x, y + h, w, a);
    }

    /** Cuts a strip {@code amount} wide (at most this box's width) off the left and returns it. */
    public Box cutLeft(float amount) {
        float a = Math.min(amount, w);
        Box cut = new Box(x, y, a, h);
        x += a;
        w -= a;
        return cut;
    }

    /** Cuts a strip {@code amount} wide (at most this box's width) off the right and returns it. */
    public Box cutRight(float amount) {
        float a = Math.min(amount, w);
        w -= a;
        return new Box(x + w, y, a, h);
    }

    /** A new box shrunk by {@code amount} on every side; negative grows it. */
    public Box inset(float amount) {
        return inset(amount, amount);
    }

    /** A new box shrunk by {@code dx} on the left and right and {@code dy} on the top and bottom. */
    public Box inset(float dx, float dy) {
        return new Box(x + dx, y + dy, w - 2 * dx, h - 2 * dy);
    }

    /** True when the point is inside; the right and bottom edges are outside. */
    public boolean contains(float px, float py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /** This box as a Skija rectangle. */
    public Rect rect() {
        return Rect.makeXYWH(x, y, w, h);
    }

    @Override
    public String toString() {
        return "Box[" + x + "," + y + " " + w + "x" + h + "]";
    }
}
