package com.rox.ui.window;

import io.github.humbleui.skija.BackendRenderTarget;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorSpace;
import io.github.humbleui.skija.ColorType;
import io.github.humbleui.skija.DirectContext;
import io.github.humbleui.skija.EncodePNGOptions;
import io.github.humbleui.skija.EncoderPNG;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.FramebufferFormat;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.SurfaceOrigin;
import org.lwjgl.opengl.GL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A GLFW window with an OpenGL context and a Skija surface on top of the default framebuffer.
 * Drives a vsync'd loop that hands the renderer a canvas in logical pixels.
 */
public final class GlfwWindow implements AutoCloseable {

    @FunctionalInterface
    public interface Renderer {
        void frame(Canvas canvas, Input input, float width, float height);
    }

    private final long handle;
    private final Input input = new Input();
    private final DirectContext context;
    private BackendRenderTarget renderTarget;
    private Surface surface;
    private int framebufferWidth, framebufferHeight;

    public GlfwWindow(String title, int width, int height) {
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialise GLFW");
        }
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_STENCIL_BITS, 8);
        glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);
        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) {
            throw new IllegalStateException("Unable to create GLFW window");
        }
        glfwMakeContextCurrent(handle);
        glfwSwapInterval(1);
        GL.createCapabilities();
        context = DirectContext.makeGL();
        installCallbacks();
        glfwShowWindow(handle);
    }

    private void installCallbacks() {
        glfwSetCursorPosCallback(handle, (w, x, y) -> {
            input.mouseX = (float) x;
            input.mouseY = (float) y;
        });
        glfwSetCursorEnterCallback(handle, (w, entered) -> {
            if (!entered) {
                input.mouseX = -1;
                input.mouseY = -1;
            }
        });
        glfwSetMouseButtonCallback(handle, (w, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                input.onMouseButton(action == GLFW_PRESS, glfwGetTime());
            }
        });
        glfwSetScrollCallback(handle, (w, dx, dy) -> input.onScroll(dy));
        glfwSetKeyCallback(handle, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                input.onKey(key);
            }
        });
    }

    /**
     * Runs the frame loop until the window closes. If the {@code roxui.screenshot} system property
     * names a file, the window saves a PNG there after ~2 seconds and exits (handy for CI/review).
     */
    public void run(Renderer renderer) {
        String screenshot = System.getProperty("roxui.screenshot");
        long frame = 0;
        int[] fbW = new int[1], fbH = new int[1], winW = new int[1], winH = new int[1];
        while (!glfwWindowShouldClose(handle)) {
            glfwPollEvents();
            glfwGetFramebufferSize(handle, fbW, fbH);
            glfwGetWindowSize(handle, winW, winH);
            if (fbW[0] == 0 || fbH[0] == 0) {
                input.endFrame();
                continue; // minimised
            }
            ensureSurface(fbW[0], fbH[0]);
            float scale = (float) fbW[0] / Math.max(1, winW[0]);

            Canvas canvas = surface.getCanvas();
            canvas.save();
            canvas.scale(scale, scale);
            renderer.frame(canvas, input, winW[0], winH[0]);
            canvas.restore();
            context.flushAndSubmit(surface);
            if (screenshot != null && ++frame == Long.getLong("roxui.screenshot.frame", 120)) {
                saveScreenshot(Path.of(screenshot));
                glfwSetWindowShouldClose(handle, true);
            }
            glfwSwapBuffers(handle);
            input.endFrame();
        }
    }

    private void saveScreenshot(Path file) {
        try (Image image = surface.makeImageSnapshot()) {
            Files.write(file, EncoderPNG.encode(context, image, EncodePNGOptions.DEFAULT).getBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write screenshot " + file, e);
        }
    }

    private void ensureSurface(int width, int height) {
        if (surface != null && width == framebufferWidth && height == framebufferHeight) {
            return;
        }
        if (surface != null) {
            surface.close();
            renderTarget.close();
        }
        framebufferWidth = width;
        framebufferHeight = height;
        renderTarget = BackendRenderTarget.makeGL(width, height, 0, 8, 0, FramebufferFormat.GR_GL_RGBA8);
        surface = Surface.wrapBackendRenderTarget(context, renderTarget,
                SurfaceOrigin.BOTTOM_LEFT, ColorType.RGBA_8888, ColorSpace.getSRGB());
    }

    @Override
    public void close() {
        if (surface != null) {
            surface.close();
            renderTarget.close();
        }
        context.close();
        glfwDestroyWindow(handle);
        glfwTerminate();
    }
}
