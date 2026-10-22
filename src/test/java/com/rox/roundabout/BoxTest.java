package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxTest {
    @Test
    void cutsShrinkTheRemainder() {
        Box box = new Box(0, 0, 100, 50);
        Box top = box.cutTop(10);
        Box left = box.cutLeft(20);
        Box right = box.cutRight(30);
        Box bottom = box.cutBottom(5);

        assertEquals("Box[0.0,0.0 100.0x10.0]", top.toString());
        assertEquals("Box[0.0,10.0 20.0x40.0]", left.toString());
        assertEquals("Box[70.0,10.0 30.0x40.0]", right.toString());
        assertEquals("Box[20.0,45.0 50.0x5.0]", bottom.toString());
        assertEquals("Box[20.0,10.0 50.0x35.0]", box.toString());
    }

    @Test
    void cutsNeverGoNegative() {
        Box box = new Box(0, 0, 10, 10);
        Box cut = box.cutLeft(25);
        assertEquals(10, cut.w);
        assertEquals(0, box.w);
    }

    @Test
    void containsIsHalfOpen() {
        Box box = new Box(10, 10, 5, 5);
        assertTrue(box.contains(10, 10));
        assertFalse(box.contains(15, 12));
    }
}
