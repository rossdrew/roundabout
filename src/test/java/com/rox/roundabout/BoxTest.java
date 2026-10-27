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

    @Test
    void containsExcludesTheRightAndBottomEdges() {
        Box box = new Box(10, 10, 5, 5);
        assertTrue(box.contains(14.9f, 14.9f));
        assertFalse(box.contains(12, 15));
        assertFalse(box.contains(9.9f, 12));
        assertFalse(box.contains(12, 9.9f));
    }

    @Test
    void edgesAndCentres() {
        Box box = new Box(10, 20, 30, 40);
        assertEquals(40, box.right());
        assertEquals(60, box.bottom());
        assertEquals(25, box.centerX());
        assertEquals(40, box.centerY());
        assertEquals(io.github.humbleui.types.Rect.makeXYWH(10, 20, 30, 40), box.rect());
    }

    @Test
    void insetShrinksEachSide() {
        Box box = new Box(10, 20, 30, 40);
        assertEquals("Box[12.0,22.0 26.0x36.0]", box.inset(2).toString());
        assertEquals("Box[13.0,24.0 24.0x32.0]", box.inset(3, 4).toString());
        assertEquals("Box[10.0,20.0 30.0x40.0]", box.toString(), "inset leaves the box alone");
    }

    @Test
    void aCopyIsIndependent() {
        Box box = new Box(0, 0, 10, 10);
        Box copy = box.copy();
        copy.cutTop(4);
        assertEquals("Box[0.0,0.0 10.0x10.0]", box.toString());
        assertEquals("Box[0.0,4.0 10.0x6.0]", copy.toString());
    }
}
