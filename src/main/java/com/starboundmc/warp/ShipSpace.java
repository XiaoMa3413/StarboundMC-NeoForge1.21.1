package com.starboundmc.warp;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.world.phys.Vec3;

import java.util.EnumMap;
import java.util.Map;

/**
 * Shared virtual-space coordinate model. The ship structure remains fixed at
 * {@link #WORLD_ANCHOR}; V/yaw/pitch are the virtual pose used to render the
 * outside scene. Coordinates use the approved 0.1 scale of the design document.
 */
public final class ShipSpace
{
    public static final Vec3 WORLD_ANCHOR = new Vec3(0.0, 102.0, 0.0);
    public static final Vec3 REF_VIEW_OFFSET = new Vec3(0.5, 10.0, 80.0);

    private static final Map<Planet, Double> RADIUS = new EnumMap<>(Planet.class);
    private static final Map<Planet, Double> YAW_DOCK = new EnumMap<>(Planet.class);
    private static final Map<Planet, Vec3> V_DOCK = new EnumMap<>(Planet.class);
    private static final Map<Planet, Vec3> Q_POS = new EnumMap<>(Planet.class);
    private static final Map<Planet, UniversePosition> UNIVERSE_DOCK = new EnumMap<>(Planet.class);
    private static final Map<Planet, UniversePosition> UNIVERSE_BODY = new EnumMap<>(Planet.class);

    static
    {
        // Scale both radii and interplanetary locations. Their ratio preserves
        // the approved 38.4 degree docked angular size.
        // Earth/Moon-like primary-to-moon radius ratio (3.67:1). Absolute
        // values remain in the approved 0.1-scaled virtual-space model.
        RADIUS.put(Planet.LUSH, 6.0);
        RADIUS.put(Planet.MOLTEN, 6.0 / 3.67);
        RADIUS.put(Planet.FROZEN, 5.5);
        RADIUS.put(Planet.BARREN, 4.0);

        YAW_DOCK.put(Planet.LUSH, 0.0);
        // Keep every dock heading within one compact sector. The ship can then
        // make slow, heavy turns instead of rotating hundreds of degrees on
        // every trip. Molten still keeps Lush over 100 degrees away from its
        // disc at dock, so the primary and moon never overlap visually.
        YAW_DOCK.put(Planet.MOLTEN, 330.0);
        YAW_DOCK.put(Planet.FROZEN, 25.0);
        YAW_DOCK.put(Planet.BARREN, 335.0);

        V_DOCK.put(Planet.LUSH, new Vec3(0.0, 102.0, 0.0));
        // The frozen dock follows the second system's navigation centre so a
        // future layout translation cannot separate the world from its system.
        V_DOCK.put(Planet.FROZEN, StarSystems.byId(StarSystems.SYS_COLD).getNavigationCenter());
        V_DOCK.put(Planet.BARREN, new Vec3(-5000.0, 102.0, -2000.0));

        Q_POS.put(Planet.LUSH, vDock(Planet.LUSH).add(rotateYaw(dockOffset(Planet.LUSH), yawDock(Planet.LUSH))));
        // Compressed Earth/Moon-like separation: 14 primary radii. Derive the
        // moon dock from its body coordinate so both retain the 38.4° dock view.
        Vec3 moonOrbit = new Vec3(-radius(Planet.LUSH) * 14.0, -4.0, -12.0);
        Q_POS.put(Planet.MOLTEN, Q_POS.get(Planet.LUSH).add(moonOrbit));
        V_DOCK.put(Planet.MOLTEN, Q_POS.get(Planet.MOLTEN)
                .subtract(rotateYaw(dockOffset(Planet.MOLTEN), yawDock(Planet.MOLTEN))));
        Q_POS.put(Planet.FROZEN, vDock(Planet.FROZEN).add(rotateYaw(dockOffset(Planet.FROZEN), yawDock(Planet.FROZEN))));
        Q_POS.put(Planet.BARREN, vDock(Planet.BARREN).add(rotateYaw(dockOffset(Planet.BARREN), yawDock(Planet.BARREN))));

        for (Planet planet : Planet.values())
        {
            UNIVERSE_DOCK.put(planet, UniversePosition.fromLegacy(V_DOCK.get(planet)));
            UNIVERSE_BODY.put(planet, UniversePosition.fromLegacy(Q_POS.get(planet)));
        }

    }

    private ShipSpace()
    {
    }

    public static double radius(Planet planet)
    {
        Double v = RADIUS.get(planet);
        if (v == null) throw new IllegalArgumentException("Unknown planet: " + planet);
        return v;
    }

    public static double yawDock(Planet planet)
    {
        return YAW_DOCK.getOrDefault(planet, 0.0);
    }

    /** Offset which produces the same 38.4 degree apparent radius at every dock. */
    public static Vec3 dockOffset(Planet planet)
    {
        return REF_VIEW_OFFSET.scale(radius(planet) / 50.0);
    }

    public static Vec3 vDock(Planet planet)
    {
        return V_DOCK.get(planet);
    }

    public static Vec3 qPos(Planet planet)
    {
        return Q_POS.get(planet);
    }

    public static UniversePosition universeDock(Planet planet)
    {
        UniversePosition position = UNIVERSE_DOCK.get(planet);
        if (position == null) throw new IllegalArgumentException("Unknown planet: " + planet);
        return position;
    }

    public static UniversePosition universeBodyPosition(Planet planet)
    {
        UniversePosition position = UNIVERSE_BODY.get(planet);
        if (position == null) throw new IllegalArgumentException("Unknown planet: " + planet);
        return position;
    }

    /** Fixed virtual-space sun coordinate for the planet's star system. */
    public static Vec3 sunPos(Planet planet)
    {
        StarSystem system = StarSystems.systemOfPlanet(planet);
        if (system == null)
            throw new IllegalArgumentException("Unknown planet: " + planet);
        return system.getStellarVisual().getVirtualPosition();
    }

    /** Unit direction from a planet toward its system's fixed sun. */
    public static Vec3 sunDirection(Planet planet)
    {
        StarSystem system = StarSystems.systemOfPlanet(planet);
        if (system == null)
            throw new IllegalArgumentException("Unknown planet: " + planet);
        return universeBodyPosition(planet).deltaTo(system.getStellarVisual().getUniversePosition()).toVec3().normalize();
    }

    public static double flightDistance(Planet from, Planet to)
    {
        return Math.sqrt(universeDock(from).distanceToSqr(universeDock(to)));
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
