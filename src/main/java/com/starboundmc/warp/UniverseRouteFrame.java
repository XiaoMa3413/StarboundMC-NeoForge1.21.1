package com.starboundmc.warp;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Converts a universe route into small double coordinates relative to its
 * departure point. Route geometry never needs a huge absolute coordinate and
 * crossing a sector boundary does not change the relative curve.
 */
public final class UniverseRouteFrame
{
    private final UniversePosition origin;

    public UniverseRouteFrame(UniversePosition origin)
    {
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    public Vec3 toRelative(UniversePosition position)
    {
        return origin.deltaTo(Objects.requireNonNull(position, "position")).toVec3();
    }

    public UniversePosition toUniverse(Vec3 relativePosition)
    {
        Objects.requireNonNull(relativePosition, "relativePosition");
        return origin.add(new UniverseDelta(relativePosition.x, relativePosition.y, relativePosition.z));
    }

    public UniversePosition origin()
    {
        return origin;
    }
}
