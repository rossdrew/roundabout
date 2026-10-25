package com.rox.roundabout.data;

import com.rox.roundabout.data.AddressFormat.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressFormatTest {
    @Test
    void digitsCoverTheAddressSpaceRoundedToWholeBytes() {
        assertEquals(2, AddressFormat.forSize(0x100, Style.C).digits());
        assertEquals(4, AddressFormat.forSize(0x800, Style.C).digits(), "0x7FF needs 3, rounded up to 4");
        assertEquals(4, AddressFormat.forSize(0x10000, Style.C).digits());
        assertEquals(6, AddressFormat.forSize(0x1000000, Style.C).digits());
        assertEquals(2, AddressFormat.forSize(1, Style.C).digits());
    }

    @Test
    void stylesSetThePrefixes() {
        AddressFormat c = AddressFormat.forSize(0x10000, Style.C);
        AddressFormat motorola = AddressFormat.forSize(0x10000, Style.MOTOROLA);

        assertEquals("0x00FF", c.address(0xFF));
        assertEquals("0x0A", c.value(10));
        assertEquals("0b00001010", c.binary(10));
        assertEquals("$00FF", motorola.address(0xFF));
        assertEquals("$0A", motorola.value(10));
        assertEquals("%00001010", motorola.binary(10));
    }
}
