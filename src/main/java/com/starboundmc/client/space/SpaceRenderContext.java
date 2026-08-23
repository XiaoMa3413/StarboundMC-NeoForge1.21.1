package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;

/** Immutable per-frame snapshot consumed by the astronomical renderers. */
public record SpaceRenderContext(Vec3 shipPosition, UniversePosition universePosition,
                                 Vec3 shipVelocity,
                                 double yaw, double pitch, double roll,
                                 FlightPhase flightPhase, boolean warping,
                                 float warpProgress, int warpDurationTicks,
                                 Planet currentBody, Planet targetBody,
                                 String currentSystemHint, String targetSystemHint,
                                 float animationTicks)
{
    static SpaceRenderContext capture(ShipPoseProvider provider, float animationTicks)
    {
        return new SpaceRenderContext(provider.position(), provider.universePosition(), provider.velocity(),
                provider.yaw(), provider.pitch(), provider.roll(),
                provider.flightPhase(), provider.isWarping(), provider.warpProgress(),
                provider.warpDurationTicks(), provider.currentBody(), provider.targetBody(),
                provider.currentSystemHint(), provider.targetSystemHint(), animationTicks);
    }
}
