package com.rox.roundabout.debugger;

import com.rox.roundabout.memory.AddressFormat;
import com.rox.roundabout.memory.MemoryRegion;
import com.rox.roundabout.memory.MemorySource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DebugTargetTest {
    private final DebugTarget minimal = new DebugTarget() {
        public MemorySource memory() {
            return null;
        }

        public List<MemoryRegion> regions() {
            return List.of();
        }

        public CpuState cpu() {
            return new CpuState(List.of(), List.of());
        }

        public boolean isPaused() {
            return false;
        }

        public void pause() {
        }

        public void resume() {
        }

        public List<Step> steps() {
            return List.of();
        }

        public String status() {
            return "";
        }
    };

    @Test
    void optionalPartsDefaultToCNumbersAndNothingExtra() {
        assertEquals(AddressFormat.Style.C, minimal.numberStyle());
        assertEquals(List.of(), minimal.labels());
        assertEquals(List.of(), minimal.frameTimings());
    }
}
