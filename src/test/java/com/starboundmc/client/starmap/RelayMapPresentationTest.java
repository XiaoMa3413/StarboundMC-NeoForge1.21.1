// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.starmap;

import com.starboundmc.encounter.RelayData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelayMapPresentationTest {
    @Test void stationBelongsToItsHomeSystemAndPlanetFamily() {
        assertFalse(RelayMapPresentation.visibleAt(StarmapLevel.GALAXY, "sys1", null, "moon", "planet", null));
        assertFalse(RelayMapPresentation.visibleAt(StarmapLevel.GALAXY, "sys1", "sys1", "moon", "planet", "planet"));
        assertTrue(RelayMapPresentation.visibleAt(StarmapLevel.SYSTEM, "sys1", "sys1", "moon", "planet", null));
        assertFalse(RelayMapPresentation.visibleAt(StarmapLevel.SYSTEM, "sys1", "sys2", "moon", "planet", null));
        assertTrue(RelayMapPresentation.visibleAt(StarmapLevel.PLANET, "sys1", "sys1", "moon", "planet", "planet"));
        assertFalse(RelayMapPresentation.visibleAt(StarmapLevel.PLANET, "sys1", "sys1", "moon", "planet", "other"));
        assertTrue(RelayMapPresentation.visibleAt(StarmapLevel.PLANET, "sys1", "sys1", "planet", null, "planet"));
    }

    @Test void markerOffsetKeepsRequestedSeparationAtEveryMapScale() {
        for (float distance : new float[]{34, 49, 98}) {
            var point = RelayMapPresentation.offset(120, 80, distance);
            assertEquals(distance, Math.hypot(point[0] - 120, point[1] - 80), .0001);
        }
    }

    @Test void approachRequiresFuelPropulsionAndAllCrewAboard() {
        int available = RelayData.Phase.AVAILABLE.ordinal();
        assertTrue(RelayMapPresentation.canAct(available, 0, false, true, false, true, 10, 10));
        assertFalse(RelayMapPresentation.canAct(available, 0, false, true, false, true, 9, 10));
        assertFalse(RelayMapPresentation.canAct(available, 0, false, true, false, false, 10, 10));
        assertTrue(RelayMapPresentation.canAct(available, 0, false, true, true, false, 10, 10));
        assertFalse(RelayMapPresentation.canAct(available, 0, false, false, true, true, 10, 10));
        assertFalse(RelayMapPresentation.canAct(available, 1, false, true, true, true, 10, 10));
        assertFalse(RelayMapPresentation.canAct(available, 0, true, true, true, true, 10, 10));
    }

    @Test void departureDoesNotRequireFuelOrWorkingEnginesButDoesRequireCrew() {
        int active = RelayData.Phase.ACTIVE.ordinal();
        assertTrue(RelayMapPresentation.canAct(active, 0, false, false, false, false, 0, 100));
        assertFalse(RelayMapPresentation.canAct(active, 1, false, false, false, false, 0, 100));
        for (var phase : RelayData.Phase.values())
            if (phase != RelayData.Phase.ACTIVE && phase != RelayData.Phase.AVAILABLE)
                assertFalse(RelayMapPresentation.canAct(phase.ordinal(), 0, false, true, true, true, 100, 0));
        assertEquals("error", RelayMapPresentation.phaseKey(-1));
        assertEquals("error", RelayMapPresentation.phaseKey(999));
    }
}
