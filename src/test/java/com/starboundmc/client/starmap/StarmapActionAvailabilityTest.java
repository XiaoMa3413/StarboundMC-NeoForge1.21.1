package com.starboundmc.client.starmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StarmapActionAvailabilityTest {
    @Test
    void galaxyActionDistinguishesLockedAndUnreachableSystems() {
        assertReason(StarmapActionAvailability.Reason.NO_SELECTION,
                StarmapActionAvailability.galaxy(false, true, true));
        assertReason(StarmapActionAvailability.Reason.SYSTEM_LOCKED,
                StarmapActionAvailability.galaxy(true, false, false));
        assertReason(StarmapActionAvailability.Reason.SYSTEM_UNREACHABLE,
                StarmapActionAvailability.galaxy(true, true, false));
        assertTrue(StarmapActionAvailability.galaxy(true, true, true).available());
    }

    @Test
    void systemActionOnlyAcceptsASelectedPlanet() {
        assertReason(StarmapActionAvailability.Reason.NO_SELECTION,
                StarmapActionAvailability.system(false, false));
        assertReason(StarmapActionAvailability.Reason.UNSUPPORTED_TARGET,
                StarmapActionAvailability.system(true, true));
        assertTrue(StarmapActionAvailability.system(true, false).available());
    }

    @Test
    void planetActionReportsEveryWarpBlockerInGameplayOrder() {
        assertReason(StarmapActionAvailability.Reason.NO_SELECTION,
                StarmapActionAvailability.planet(false, true, false, false, 50, 20));
        assertReason(StarmapActionAvailability.Reason.BODY_LOCKED,
                StarmapActionAvailability.planet(true, false, true, true, 0, 20));
        assertReason(StarmapActionAvailability.Reason.WARP_IN_PROGRESS,
                StarmapActionAvailability.planet(true, true, true, true, 0, 20));
        assertReason(StarmapActionAvailability.Reason.CURRENT_DESTINATION,
                StarmapActionAvailability.planet(true, true, false, true, 0, 20));

        StarmapActionAvailability.Result insufficient =
                StarmapActionAvailability.planet(true, true, false, false, 12, 20);
        assertReason(StarmapActionAvailability.Reason.INSUFFICIENT_FUEL, insufficient);
        assertEquals(20, insufficient.requiredFuel());
        assertEquals(12, insufficient.availableFuel());

        StarmapActionAvailability.Result available =
                StarmapActionAvailability.planet(true, true, false, false, 20, 20);
        assertTrue(available.available());
        assertEquals(StarmapActionAvailability.Reason.NONE, available.reason());
    }

    private static void assertReason(StarmapActionAvailability.Reason expected,
                                     StarmapActionAvailability.Result result) {
        assertFalse(result.available());
        assertEquals(expected, result.reason());
    }
}
