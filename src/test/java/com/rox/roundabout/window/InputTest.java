package com.rox.roundabout.window;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.glfw.GLFW.*;

class InputTest {
    @Test
    void keyNamesAreShortLabels() {
        assertEquals("F", Input.keyName(GLFW_KEY_F));
        assertEquals("7", Input.keyName(GLFW_KEY_7));
        assertEquals("F10", Input.keyName(GLFW_KEY_F10));
        assertEquals("Space", Input.keyName(GLFW_KEY_SPACE));
        assertEquals("PgDn", Input.keyName(GLFW_KEY_PAGE_DOWN));
        assertEquals("key 348", Input.keyName(GLFW_KEY_MENU));
    }

    @Test
    void keyNameRangesIncludeTheirEnds() {
        assertEquals("A", Input.keyName(GLFW_KEY_A));
        assertEquals("Z", Input.keyName(GLFW_KEY_Z));
        assertEquals("0", Input.keyName(GLFW_KEY_0));
        assertEquals("9", Input.keyName(GLFW_KEY_9));
        assertEquals("F1", Input.keyName(GLFW_KEY_F1));
        assertEquals("F25", Input.keyName(GLFW_KEY_F25));
    }

    @Test
    void pressesAndReleasesAreOneFrameEdges() {
        Input input = new Input();
        input.onMouseButton(true, 1);
        assertTrue(input.mousePressed);
        assertTrue(input.mouseDown);
        assertFalse(input.doubleClicked);

        input.endFrame();
        assertFalse(input.mousePressed);
        assertTrue(input.mouseDown, "held until released");

        input.onMouseButton(false, 1.1);
        assertTrue(input.mouseReleased);
        assertFalse(input.mouseDown);
        input.endFrame();
        assertFalse(input.mouseReleased);
    }

    @Test
    void aSecondQuickPressIsADoubleClick() {
        Input input = new Input();
        input.onMouseButton(true, 1);
        input.onMouseButton(false, 1.05);
        input.onMouseButton(true, 1.2);
        assertTrue(input.doubleClicked);

        input.onMouseButton(false, 1.25);
        input.onMouseButton(true, 1.3);
        assertFalse(input.doubleClicked, "a third press starts again");
    }

    @Test
    void aSlowSecondPressIsNotADoubleClick() {
        Input input = new Input();
        input.onMouseButton(true, 1);
        input.onMouseButton(false, 1.1);
        input.onMouseButton(true, 1.5);
        assertFalse(input.doubleClicked);
    }

    @Test
    void keysAndScrollLastOneFrame() {
        Input input = new Input();
        input.onKey(GLFW_KEY_W);
        input.onScroll(1);
        input.onScroll(2);
        assertTrue(input.keyPressed(GLFW_KEY_W));
        assertFalse(input.keyPressed(GLFW_KEY_Q));
        assertEquals(3, input.scrollY);

        input.endFrame();
        assertFalse(input.keyPressed(GLFW_KEY_W));
        assertEquals(0, input.scrollY);
    }

    @Test
    void exactlyTheDoubleClickTimeIsTooSlow() {
        Input input = new Input();
        input.onMouseButton(true, 0.25);
        input.onMouseButton(false, 0.3);
        input.onMouseButton(true, 0.6); // 0.6 - 0.25 is exactly 0.35 as a double
        assertFalse(input.doubleClicked);
    }
}
