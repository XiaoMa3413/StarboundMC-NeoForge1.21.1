package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.space.UniversePosition;
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

    /**
     * Body the ship is docked at or departing from, as a universe entry id.
     *
     * <p>An id rather than the legacy {@code Planet} enum, so a renderer never
     * needs the enum and a datapack body renders without a new enum constant.</p>
     */
    String currentBodyId();

    /** Body being flown to as a universe entry id, or null when docked. */
    String targetBodyId();

    String currentSystemHint();

    String targetSystemHint();
}
