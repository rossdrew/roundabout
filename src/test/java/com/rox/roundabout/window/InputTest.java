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
}
