package com.rox.ui;

import io.github.humbleui.types.Rect;

/**
 * A mutable layout rectangle using the "rect cut" approach: each cut slices a strip
 * off one edge, shrinks this box and returns the strip.
 */
public final class Box {
    public float x, y, w, h;

    public Box(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0, w);
        this.h = Math.max(0, h);
    }

    public Box copy() {
        return new Box(x, y, w, h);
    }

    public float right() {
        return x + w;
    }

    public float bottom() {
        return y + h;
    }

    public float centerY() {
        return y + h / 2;
    }

    public Box cutTop(float amount) {
        float a = Math.min(amount, h);
        Box cut = new Box(x, y, w, a);
        y += a;
        h -= a;
        return cut;
    }

    public Box cutBottom(float amount) {
        float a = Math.min(amount, h);
        h -= a;
        return new Box(x, y + h, w, a);
    }

    public Box cutLeft(float amount) {
        float a = Math.min(amount, w);
        Box cut = new Box(x, y, a, h);
        x += a;
        w -= a;
        return cut;
    }

    public Box cutRight(float amount) {
        float a = Math.min(amount, w);
        w -= a;
        return new Box(x + w, y, a, h);
    }

    public Box inset(float amount) {
        return inset(amount, amount);
    }

    public Box inset(float dx, float dy) {
        return new Box(x + dx, y + dy, w - 2 * dx, h - 2 * dy);
    }

    public boolean contains(float px, float py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public Rect rect() {
        return Rect.makeXYWH(x, y, w, h);
    }

    @Override
    public String toString() {
        return "Box[" + x + "," + y + " " + w + "x" + h + "]";
    }
}
