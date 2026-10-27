package com.rox.roundabout;

import com.rox.roundabout.window.GlfwWindow;
import com.rox.roundabout.window.InputDriver;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;

import java.util.function.Consumer;

/**
 * Draws frames off-screen, with no window or GPU, driven by an {@link InputDriver}. The scene is any
 * {@link GlfwWindow.Renderer} (e.g. a whole debugger frame), or widgets drawn with {@link #ui}.
 */
public final class UiHarness implements AutoCloseable {
    public final Ui ui = new Ui(Theme.dark());
    public final InputDriver input = new InputDriver();
    private final int width, height;
    private final Surface surface;
    private GlfwWindow.Renderer scene = (canvas, in, w, h) -> {
    };

    public UiHarness(int width, int height) {
        this.width = width;
        this.height = height;
        surface = Surface.makeRaster(ImageInfo.makeN32Premul(width, height));
    }

    public UiHarness scene(GlfwWindow.Renderer scene) {
        this.scene = scene;
        return this;
    }

    /** A scene of widgets: {@code draw} runs between {@link Ui#begin} and {@link Ui#end} on a cleared canvas. */
    public UiHarness ui(Consumer<Ui> draw) {
        return scene((canvas, in, w, h) -> {
            canvas.clear(ui.theme.background);
            ui.begin(canvas, in, w, h);
            draw.accept(ui);
            ui.end();
        });
    }

    /** Draws one frame with the input given so far, then clears its edges. */
    public UiHarness frame() {
        scene.frame(surface.getCanvas(), input.input(), width, height);
        input.endFrame();
        return this;
    }

    public UiHarness hover(float x, float y) {
        input.move(x, y);
        return frame();
    }

    /** Presses at the point for one frame and releases on the next. */
    public UiHarness click(float x, float y) {
        input.move(x, y).press();
        frame();
        input.release();
        return frame();
    }

    public UiHarness doubleClick(float x, float y) {
        return click(x, y).click(x, y);
    }

    public UiHarness key(int glfwKey) {
        input.key(glfwKey);
        return frame();
    }

    public UiHarness scroll(float x, float y, double dy) {
        input.move(x, y).scroll(dy);
        return frame();
    }

    /** The colour of a pixel in the last frame, as ARGB. */
    public int pixel(int x, int y) {
        try (Bitmap bitmap = new Bitmap()) {
            bitmap.allocN32Pixels(1, 1);
            surface.readPixels(bitmap, x, y);
            return bitmap.getColor(0, 0);
        }
    }

    @Override
    public void close() {
        surface.close();
    }
}
