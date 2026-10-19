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

    @Test
    void percentileOfSortedSamples() {
        RingSeries series = new RingSeries(200);
        for (int i = 200; i >= 1; i--) {
            series.add(i);
        }
        assertEquals(199, series.percentile(0.99f));
        assertEquals(101, series.percentile(0.5f));
        assertEquals(200, series.percentile(1f));
        assertEquals(0, new RingSeries(3).percentile(0.99f));
    }

    @Test
    void countAboveIsStrict() {
        RingSeries series = new RingSeries(4);
        series.add(10);
        series.add(25);
        series.add(26);
        assertEquals(1, series.countAbove(25));
    }
}
