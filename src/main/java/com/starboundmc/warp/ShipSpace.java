package com.starboundmc.warp;

import net.minecraft.world.phys.Vec3;

/**
 * Shared virtual-space math. The ship structure remains fixed at
 * {@link #WORLD_ANCHOR}; V/yaw/pitch are the virtual pose used to render the
 * outside scene. Coordinates use the approved 0.1 scale of the design document.
 *
 * <p>This class deliberately holds no universe data. The per-body dock
 * coordinates, radii and dock headings that used to live here as
 * {@code EnumMap<Planet, ...>} tables are now read from the universe catalog
 * through {@link UniverseNavigation}, so a body's flight geometry belongs to its
 * definition rather than to this helper.</p>
 */
public final class ShipSpace
{
    public static final Vec3 WORLD_ANCHOR = new Vec3(0.0, 102.0, 0.0);
    public static final Vec3 REF_VIEW_OFFSET = new Vec3(0.5, 10.0, 80.0);

    private ShipSpace()
    {
    }

    /** Offset which produces the same 38.4 degree apparent radius at every dock. */
    public static Vec3 dockOffset(double bodyRadius)
    {
        return REF_VIEW_OFFSET.scale(bodyRadius / 50.0);
    }

    /** Minecraft yaw convention: positive yaw rotates virtual space clockwise viewed from above. */
    public static Vec3 rotateYaw(Vec3 vector, double degrees)
    {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x * cos + vector.z * sin, vector.y, -vector.x * sin + vector.z * cos);
    }

    public static Vec3 rotatePitch(Vec3 vector, double degrees)
    {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(vector.x, vector.y * cos - vector.z * sin, vector.y * sin + vector.z * cos);
    }
}
