package com.starboundmc.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightVisualClockTest
{
    @Test
    void smoothsForwardSnapshotJumps()
    {
        FlightVisualClock clock = new FlightVisualClock();
        assertTrue(clock.sample(0.0, 560.0, 0L) == 0.0);

        double next = clock.sample(6.0, 560.0, 10_000_000L);
        assertTrue(next > 0.0 && next < 6.0,
                "a late snapshot must not move the renderer six ticks at once");
    }

    @Test
    void neverMovesBackWhenAnOlderSnapshotArrives()
    {
        FlightVisualClock clock = new FlightVisualClock();
        clock.sample(4.0, 560.0, 0L);
        double before = clock.sample(8.0, 560.0, 250_000_000L);
        double after = clock.sample(3.0, 560.0, 500_000_000L);

        assertTrue(after >= before,
                "a late older snapshot must not pull the planet backwards");
    }

    @Test
    void clampsAtTheFlightEnd()
    {
        FlightVisualClock clock = new FlightVisualClock();
        clock.sample(0.0, 10.0, 0L);
        assertTrue(clock.sample(20.0, 10.0, 1_000_000_000L) <= 10.0);
    }

    @Test
    void acceptsTheSignedValuesReturnedBySystemNanoTime()
    {
        FlightVisualClock clock = new FlightVisualClock();
        assertTrue(clock.sample(0.0, 10.0, -2_000_000_000L) == 0.0);
        assertTrue(clock.sample(1.0, 10.0, -1_900_000_000L) > 0.0);
    }

    @Test
    void neverRunsAheadOfTheAuthoritativeTwentyTicksPerSecondClock()
    {
        FlightVisualClock clock = new FlightVisualClock();
        clock.sample(0.0, 100.0, 0L);
        assertTrue(clock.sample(100.0, 100.0, 1_000_000_000L) <= 20.0);
    }
}
