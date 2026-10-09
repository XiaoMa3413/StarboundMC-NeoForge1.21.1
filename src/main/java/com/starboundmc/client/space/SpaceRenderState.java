package com.starboundmc.client.space;

import java.util.Objects;

/** Selects the active ship pose source used by the space renderer. */
public final class SpaceRenderState
{
    private static ShipPoseProvider provider = ClientShipPoseProvider.INSTANCE;

    private SpaceRenderState()
    {
    }

    public static SpaceRenderContext capture(float animationTicks)
    {
        return SpaceRenderContext.capture(provider, animationTicks);
    }

    /** Installs a pose source for deterministic render tests. */
    public static void setPoseProvider(ShipPoseProvider poseProvider)
    {
        provider = Objects.requireNonNull(poseProvider);
        StarSystemResolver.reset();
    }

    public static void resetPoseProvider()
    {
        provider = ClientShipPoseProvider.INSTANCE;
        StarSystemResolver.reset();
    }
}
