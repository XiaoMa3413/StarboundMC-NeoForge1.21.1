package com.starboundmc.client.space;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;

/**
 * Minimal render-facing contract for a future freely controlled ship.
 *
 * <p>A controller owns continuous universe position, velocity and attitude.
 * Automatic-route fields default to an inactive state, so implementing free
 * flight does not require pretending that a warp route is in progress. This
 * interface only supplies visuals; it does not grant client authority over
 * server flight state.</p>
 */
public interface FreeFlightPoseProvider extends ShipPoseProvider
{
    @Override
    UniversePosition universePosition();

    UniverseDelta universeVelocity();

    /** Local compatibility view; sector changes recenter it automatically. */
    @Override
    default Vec3 position()
    {
        return universePosition().toLocalVec3();
    }

    @Override
    default Vec3 velocity()
    {
        return universeVelocity().toVec3();
    }

    @Override
    default FlightPhase flightPhase()
    {
        return FlightPhase.DOCKED;
    }

    @Override
    default boolean isWarping()
    {
        return false;
    }

    @Override
    default float warpProgress()
    {
        return 0.0F;
    }

    @Override
    default int warpDurationTicks()
    {
        return 1;
    }

    @Override
    default Planet currentBody()
    {
        return null;
    }

    @Override
    default Planet targetBody()
    {
        return null;
    }

    @Override
    default String currentSystemHint()
    {
        return null;
    }

    @Override
    default String targetSystemHint()
    {
        return null;
    }
}
