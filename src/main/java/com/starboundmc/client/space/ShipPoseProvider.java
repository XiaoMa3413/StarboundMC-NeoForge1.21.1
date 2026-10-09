package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.space.UniversePosition;

/**
 * Canonical virtual pose and route projection consumed by the space renderer.
 */
public interface ShipPoseProvider
{
    UniversePosition universePosition();

    com.starboundmc.space.UniverseDelta universeVelocity();

    double yaw();

    double pitch();

    double roll();

    FlightPhase flightPhase();

    boolean isWarping();

    float warpProgress();

    int warpDurationTicks();

    /**
     * Body the ship is docked at or departing from, as a universe entry id.
     *
     * <p>Identity belongs to the universe catalog; renderers do not infer it from pose.</p>
     */
    String currentBodyId();

    /** Body being flown to as a universe entry id, or null when docked. */
    String targetBodyId();

    String currentSystemHint();

    String targetSystemHint();
}
