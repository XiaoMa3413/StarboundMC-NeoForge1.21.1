// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.starmap;

import com.starboundmc.encounter.RelayData;
import java.util.Locale;
import java.util.Objects;

/** Map-only placement and action rules; the relay remains a space encounter, not a planet. */
final class RelayMapPresentation {
    private RelayMapPresentation() { }

    static boolean visibleAt(StarmapLevel level, String homeSystem, String selectedSystem,
                             String homeBody, String parentBody, String focusedPlanet) {
        if (level == StarmapLevel.GALAXY) return false;
        if (!Objects.equals(homeSystem, selectedSystem)) return false;
        return level == StarmapLevel.SYSTEM || Objects.equals(parentBody == null ? homeBody : parentBody, focusedPlanet);
    }

    static float[] offset(float hostX, float hostY, float separation) {
        return new float[]{hostX + separation * .8f, hostY - separation * .6f};
    }

    static boolean canAct(RelayData.Phase phase, int outsideCrew, boolean warping, boolean sublight,
                          boolean hyperdrive, boolean sameSystem, int fuel, int cost) {
        if (outsideCrew > 0 || warping) return false;
        if (phase == RelayData.Phase.ACTIVE) return true;
        return phase == RelayData.Phase.AVAILABLE && sublight
                && (sameSystem || hyperdrive) && fuel >= cost;
    }

    static String phaseKey(RelayData.Phase phase) {
        return phase.name().toLowerCase(Locale.ROOT);
    }
}
