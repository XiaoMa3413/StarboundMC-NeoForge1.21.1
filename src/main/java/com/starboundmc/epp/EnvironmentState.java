// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.world.universe.PlanetEnvironmentProfile;

/** Resolved at the player's location, independently of the ship's destination. */
public record EnvironmentState(Atmosphere atmosphere, int coldTier, int heatTier,
                               int radiationTier, float gravityScale, boolean pressurized) {
    public enum Atmosphere { BREATHABLE, UNBREATHABLE, VACUUM }
    public static final EnvironmentState SAFE = from(PlanetEnvironmentProfile.TEMPERATE);
    public static final EnvironmentState SHIP_INTERIOR = new EnvironmentState(Atmosphere.BREATHABLE, 0, 0, 0, 1, true);
    public static final EnvironmentState SPACE = new EnvironmentState(Atmosphere.VACUUM, 0, 0, 0, 1, false);
    public boolean breathable() { return atmosphere == Atmosphere.BREATHABLE; }
    public static EnvironmentState from(PlanetEnvironmentProfile profile) {
        return new EnvironmentState(profile.breathable() ? Atmosphere.BREATHABLE : Atmosphere.UNBREATHABLE,
                profile.coldTier(), profile.heatTier(), profile.radiationTier(), profile.gravityScale(), false);
    }
}
