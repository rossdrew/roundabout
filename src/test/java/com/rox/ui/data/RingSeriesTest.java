package com.rox.ui.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RingSeriesTest {
    @Test
    void keepsNewestSamplesInOrder() {
        RingSeries series = new RingSeries(3);
        for (int i = 1; i <= 5; i++) {
            series.add(i);
        }
        assertEquals(3, series.size());
        assertEquals(3, series.get(0));
        assertEquals(5, series.get(2));
        assertEquals(5, series.latest());
        assertEquals(5, series.max());
        assertEquals(4, series.average());
    }

    @Test
    void emptySeriesIsSafe() {
        RingSeries series = new RingSeries(4);
        assertEquals(0, series.latest());
        assertEquals(0, series.max());
        assertThrows(IndexOutOfBoundsException.class, () -> series.get(0));
    }
}
