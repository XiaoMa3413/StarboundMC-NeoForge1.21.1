package com.starboundmc.client.space;

import com.starboundmc.warp.FlightPhase;
import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;

/** Immutable per-frame projection. Vec3 values are derived local frame data, never pose authority. */
public record SpaceRenderContext(Vec3 shipPosition, UniversePosition universePosition,
                                 Vec3 shipVelocity,
                                 double yaw, double pitch, double roll,
                                 FlightPhase flightPhase, boolean warping,
                                 float warpProgress, int warpDurationTicks,
                                 String currentBodyId, String targetBodyId,
                                 String currentSystemHint, String targetSystemHint,
                                 float animationTicks)
{
    static SpaceRenderContext capture(ShipPoseProvider provider, float animationTicks)
    {
        if (provider instanceof ClientShipPoseProvider client)
            return client.capture(animationTicks);
        UniversePosition position = provider.universePosition();
        return new SpaceRenderContext(position.toLocalVec3(), position, provider.universeVelocity().toVec3(),
                provider.yaw(), provider.pitch(), provider.roll(),
                provider.flightPhase(), provider.isWarping(), provider.warpProgress(),
                provider.warpDurationTicks(), provider.currentBodyId(), provider.targetBodyId(),
                provider.currentSystemHint(), provider.targetSystemHint(), animationTicks);
    }
}
