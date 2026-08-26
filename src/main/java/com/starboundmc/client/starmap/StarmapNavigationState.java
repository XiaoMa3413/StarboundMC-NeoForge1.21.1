package com.starboundmc.client.starmap;

import com.starboundmc.world.starmap.PlanetEntry;
import com.starboundmc.world.starmap.StarSystem;

/** Pure page-context transitions, kept independent of the client UI runtime. */
record StarmapNavigationState(StarmapLevel level, StarSystem selectedSystem,
                              PlanetEntry selectedEntry, PlanetEntry focusedPlanet,
                              boolean centralStarSelected) {
    StarmapNavigationState enterSystem(StarSystem system) {
        if (system == null)
            return this;
        return new StarmapNavigationState(StarmapLevel.SYSTEM, system,
                null, null, false);
    }

    StarmapNavigationState enterPlanet(PlanetEntry planet) {
        if (level != StarmapLevel.SYSTEM || selectedSystem == null
                || planet == null || planet.isMoon())
            return this;
        return new StarmapNavigationState(StarmapLevel.PLANET, selectedSystem,
                null, planet, false);
    }

    StarmapNavigationState goBack() {
        if (level == StarmapLevel.PLANET) {
            return new StarmapNavigationState(StarmapLevel.SYSTEM, selectedSystem,
                    null, null, false);
        }
        if (level == StarmapLevel.SYSTEM) {
            return new StarmapNavigationState(StarmapLevel.GALAXY, null,
                    null, null, false);
        }
        return this;
    }
}
