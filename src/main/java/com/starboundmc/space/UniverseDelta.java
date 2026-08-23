package com.starboundmc.space;

import net.minecraft.world.phys.Vec3;

/** Double-precision relative displacement between two universe positions. */
public record UniverseDelta(double x, double y, double z)
{
    public static final UniverseDelta ZERO = new UniverseDelta(0.0, 0.0, 0.0);

    public UniverseDelta
    {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z))
            throw new IllegalArgumentException("Universe delta must be finite");
    }

    public double lengthSqr()
    {
        return x * x + y * y + z * z;
    }

    public double length()
    {
        return Math.sqrt(lengthSqr());
    }

    public UniverseDelta scale(double factor)
    {
        if (!Double.isFinite(factor))
            throw new IllegalArgumentException("Universe delta scale must be finite");
        return new UniverseDelta(x * factor, y * factor, z * factor);
    }

    /** Convert only after subtracting an origin; never use for absolute galaxy coordinates. */
    public Vec3 toVec3()
    {
        return new Vec3(x, y, z);
    }
}
