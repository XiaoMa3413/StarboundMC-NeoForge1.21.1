package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpaceRenderClockTest {
    @Test void keepsFractionalMotionAtLargeWorldTimes() {
        long ticks = 1L << 42;
        float phase = SpaceRenderClock.periodicPhase(ticks, 0, 2400);
        float later = SpaceRenderClock.periodicPhase(ticks, .5F, 2400);
        assertEquals(Math.PI / 2400, later - phase, 1e-6);
    }
    @Test void wrapsWithoutAJumpInPeriodicEffects() {
        float before = SpaceRenderClock.periodicPhase(2399, .999F, 2400);
        float after = SpaceRenderClock.periodicPhase(2400, .001F, 2400);
        for (int frequency = 2; frequency <= 5; frequency++)
            assertEquals(Math.sin(before * frequency), Math.sin(after * frequency), 3e-5);
    }
    @Test void rejectsAnInvalidPeriodAndTime() {
        assertThrows(IllegalArgumentException.class, () -> SpaceRenderClock.periodicPhase(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SpaceRenderClock.periodicPhase(0, Float.NaN, 10));
    }
}
