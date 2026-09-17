package com.starboundmc.world.universe;

import com.starboundmc.space.UniversePosition;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import com.starboundmc.warp.ShipSpace;
/**
 * The authored flight geometry of the four inner-system bodies, as literals.
 *
 * <p>These numbers were captured from the shipped model before the data layer
 * existed: the first pass read them out of {@code ShipSpace}, which is where the
 * legacy code kept them. Migration step A5 then moved that data into the body
 * definitions and reduced {@code ShipSpace} to pure math, so a test can no longer
 * ask {@code ShipSpace} what a dock position is.</p>
 *
 * <p>These four are the bodies whose geometry the migration had to reproduce
 * exactly, which is why they are the ones pinned as literals. The gas giant and
 * the rocky moon are covered by their own berth tests.</p>
 *
 * <p>Keeping them here as literals is the stronger arrangement: the expectation
 * no longer comes from the implementation under test, so the two cannot drift
 * together. Every value below is a copy of what the game used to ship, and
 * changing one is a deliberate gameplay change, not a refactor.</p>
 */
final class NavigableBodyExpectations
{
    /** One body's authored flight geometry. */
    record Dock(double radius, double yawDock,
                double dockX, double dockY, double dockZ,
                double bodyX, double bodyY, double bodyZ,
                double offsetX, double offsetY, double offsetZ)
    {
        UniversePosition dockPosition()
        {
            return UniversePosition.fromLegacy(new Vec3(dockX, dockY, dockZ));
        }

        UniversePosition bodyPosition()
        {
            return UniversePosition.fromLegacy(new Vec3(bodyX, bodyY, bodyZ));
        }

        Vec3 dockOffset()
        {
            return new Vec3(offsetX, offsetY, offsetZ);
        }
    }

    private static final Map<String, Dock> BY_ENTRY_ID = Map.of(
            "sys1:lush", new Dock(6.0, 0.0,
                    0.0, 102.0, 0.0,
                    0.06, 103.2, 9.6,
                    0.06, 1.2, 9.6),
            // The molten moon's radius is the primary divided by the Earth/Moon
            // ratio. Authored as the expression, kept here as its exact value.
            "sys1:molten", new Dock(6.0 / 3.67, 330.0,
                    -82.64625654611092, 98.87302452316077, -4.673526941779457,
                    -83.94, 99.2, -2.4000000000000004,
                    0.01634877384196185, 0.326975476839237, 2.615803814713896),
            "sys1:barren", new Dock(4.0, 335.0,
                    -5000.0, 102.0, -2000.0,
                    -5002.668504563659, 102.8, -1994.1827254324958,
                    0.04, 0.8, 6.4),
            "sys2:frozen", new Dock(5.5, 90.0,
                    32400.0, 102.0, 20550.0,
                    32408.8, 103.1, 20549.945,
                    0.055, 1.1, 8.8));

    /** Virtual-space star position per system. */
    private static final Map<String, Vec3> STAR_POSITION = Map.of(
            "sys1", new Vec3(18000.0, 9000.0, -14000.0),
            "sys2", new Vec3(38400.0, 7000.0, 31550.0));

    /** Unit lighting direction per system, derived from those star positions. */
    private static final Map<String, Vec3> LIGHTING_DIRECTION = Map.of(
            "sys1", new Vec3(0.7730342465236314, 0.35274147310601395, -0.5272489989109896),
            "sys2", new Vec3(0.41948565266193477, 0.48226867201033763, 0.7690570298802137));

    /** Straight-line distance between two docks, as the shipped game computed it. */
    private static final Map<String, Double> FLIGHT_DISTANCE = Map.of(
            "sys1:lush->sys1:molten", 82.8373318667005,
            "sys1:lush->sys1:barren", 5385.164807134504,
            "sys1:lush->sys2:frozen", 38367.46668728599);

    private NavigableBodyExpectations()
    {
    }

    static Dock dock(String entryId)
    {
        Dock dock = BY_ENTRY_ID.get(entryId);
        if (dock == null)
            throw new IllegalArgumentException("No pinned geometry for " + entryId);
        return dock;
    }

    static Vec3 starPosition(String systemId)
    {
        return STAR_POSITION.get(systemId);
    }

    static Vec3 lightingDirection(String systemId)
    {
        return LIGHTING_DIRECTION.get(systemId);
    }

    static double flightDistance(String fromEntryId, String toEntryId)
    {
        return FLIGHT_DISTANCE.get(fromEntryId + "->" + toEntryId);
    }

    /** The four captured bodies, in the order the legacy enum declared them. */
    static List<String> entryIds()
    {
        return List.of("sys1:lush", "sys1:molten", "sys1:barren", "sys2:frozen");
    }
}
