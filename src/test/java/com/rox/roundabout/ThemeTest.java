package com.rox.roundabout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ThemeTest {
    @Test
    void mixBlendsEachChannel() {
        assertEquals(0x80406080, Theme.mix(0x00000000, 0xFF80C0FF, 0.5f));
        assertEquals(0x11223344, Theme.mix(0x11223344, 0xFFFFFFFF, 0));
        assertEquals(0xFFFFFFFF, Theme.mix(0x11223344, 0xFFFFFFFF, 1));
        assertEquals(0xFFFFFFFF, Theme.mix(0x11223344, 0xFFFFFFFF, 2), "clamped");
        assertEquals(0x11223344, Theme.mix(0x11223344, 0xFFFFFFFF, -1), "clamped");
    }

    @Test
    void withAlphaReplacesOnlyTheAlpha() {
        assertEquals(0x7F123456, Theme.withAlpha(0xFF123456, 0.5f));
        assertEquals(0x00123456, Theme.withAlpha(0xFF123456, 0));
        assertEquals(0xFF123456, Theme.withAlpha(0x00123456, 1));
        assertEquals(0xFF123456, Theme.withAlpha(0x00123456, 3), "clamped");
    }

    @Test
    void heatRampsFromClearWarnToHot() {
        Theme t = Theme.dark();
        assertEquals(Theme.withAlpha(t.warn, 0), t.heat(0));
        assertEquals(Theme.mix(Theme.withAlpha(t.warn, 0), Theme.withAlpha(t.warn, 0.32f), 0.5f), t.heat(0.25f));
        assertEquals(Theme.withAlpha(t.warn, 0.32f), t.heat(0.5f));
        assertEquals(Theme.mix(Theme.withAlpha(t.warn, 0.32f), Theme.withAlpha(t.hot, 0.62f), 0.5f), t.heat(0.75f));
        assertEquals(Theme.withAlpha(t.hot, 0.62f), t.heat(1));
    }

    @Test
    void theDarkThemeHasFonts() {
        Theme t = Theme.dark();
        assertNotNull(t.ui);
        assertNotNull(t.monoLarge);
        assertTrue(t.mono.getSize() < t.monoLarge.getSize());
    }
}
