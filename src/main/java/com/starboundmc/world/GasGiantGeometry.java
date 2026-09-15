package com.starboundmc.world;

import net.minecraft.world.phys.Vec3;

/**
 * Canonical gas-giant ring-frame geometry, shared by the sky renderers and the
 * flight-route planner so the ring plane can never drift between the two.
 *
 * <p>The ring and the surface share one tilted body frame. Baked ring geometry
 * maps the body-local equatorial plane through {@code Rx(tilt) * Ry(yaw)} (see
 * the gas-giant ring bake), so — because a full ring is rotationally symmetric
 * about the pole — the ring plane's normal only depends on the axial tilt:
 * {@code n = Rx(tilt) * (0, 1, 0)}. That normal is what both the renderer (for
 * presentation) and the route planner (for clearance) need.</p>
 */
public final class GasGiantGeometry
{
    /** Saturn's real 26.7-degree axial tilt. */
    public static final float AXIAL_TILT_DEGREES = 26.7F;
    /** Fixed body yaw; it spins the ring and the band texture within the plane. */
    public static final float BODY_YAW_DEGREES = 40.0F;
    /** Real Saturn proportions: the main rings span ~1.24-2.27 planetary radii. */
    public static final float RING_INNER_RADII = 1.24F;
    public static final float RING_OUTER_RADII = 2.27F;

    private static final Vec3 NORMAL = new Vec3(0.0,
            Math.cos(Math.toRadians(AXIAL_TILT_DEGREES)),
            Math.sin(Math.toRadians(AXIAL_TILT_DEGREES)));

    private GasGiantGeometry()
    {
    }

    /** Unit normal of the ring (equatorial) plane in the shared virtual frame. */
    public static Vec3 ringPlaneNormal()
    {
        return NORMAL;
    }

    public static double ringInnerDistance(double bodyRadius)
    {
        return bodyRadius * RING_INNER_RADII;
    }

    public static double ringOuterDistance(double bodyRadius)
    {
        return bodyRadius * RING_OUTER_RADII;
    }

    /**
     * Signed distance of {@code point} from the ring plane through
     * {@code bodyCenter}; positive on the +normal side.
     */
    public static double planeOffset(Vec3 point, Vec3 bodyCenter)
    {
        return point.subtract(bodyCenter).dot(NORMAL);
    }

    /** Planar (XZ) radius from the body centre, which is how routes are planned. */
    public static double planarRadius(Vec3 point, Vec3 bodyCenter)
    {
        double dx = point.x - bodyCenter.x;
        double dz = point.z - bodyCenter.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /**
     * Whether a point's planar radius falls within the ring disc's XZ footprint.
     * A tilted disc's XZ projection is an ellipse no wider than the true radius,
     * so testing against the true outer radius is conservative (never misses a
     * genuinely intersecting point).
     */
    public static boolean insideRingDisc(double planarRadius, double bodyRadius)
    {
        return planarRadius >= ringInnerDistance(bodyRadius)
                && planarRadius <= ringOuterDistance(bodyRadius);
    }
}
