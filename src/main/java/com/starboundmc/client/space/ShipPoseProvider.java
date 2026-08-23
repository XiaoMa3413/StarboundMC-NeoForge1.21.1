package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;

/**
 * Source of a ship's virtual pose. A future manual flight controller can
 * implement this interface without coupling the sky renderer to its controls.
 */
public interface ShipPoseProvider
{
    Vec3 position();

    /** Compatibility default until a controller owns native sector coordinates. */
    default UniversePosition universePosition()
    {
        return UniversePosition.fromLegacy(position());
    }

    Vec3 velocity();

    double yaw();

    double pitch();

    double roll();

    FlightPhase flightPhase();

    boolean isWarping();

    float warpProgress();

    int warpDurationTicks();

    Planet currentBody();

    Planet targetBody();

    String currentSystemHint();

    String targetSystemHint();
}
