package com.starboundmc.world.starmap;

import com.starboundmc.world.Planet;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry of all star systems shown on the ship console's star map.
 *
 * <p>Two systems exist so far:
 * <ul>
 *   <li>{@code sys1} "第一恒星系" (warm yellow sun-like star): barren planet on the
 *       inner orbit, the lush overworld, its molten moon, and a gas giant with
 *       a dead rocky moon on the outer orbit (the gas giant and its moon are
 *       placeholders — visible on the map but locked/unreachable).</li>
 *   <li>{@code sys2} "第二恒星系" (dim, strongly-radiating RED DWARF): only the
 *       frozen world for now; more bodies can be added later.</li>
 * </ul>
 *
 * <p>Adding a planet later: append one {@link PlanetEntry} to the system's
 * list (destination = the {@link Planet} if it has a dimension, null if it is
 * a locked placeholder) and add its translation keys.</p>
 */
public class StarSystems
{
    private static final int GALAXY_MAP_WIDTH = 250;
    private static final int GALAXY_MAP_HEIGHT = 220;

    public static final String SYS_MAIN = "sys1";
    public static final String SYS_COLD = "sys2";

    public static final int STAR_COLOR_MAIN = 0xFFF2D98B; // warm yellow-white
    public static final int STAR_COLOR_RED_DWARF = 0xFFCC5A38; // dim red dwarf

    /**
     * The second system is translated as one cluster. Its centre is about
     * 40,000 units from sys1: from the starter orbit the red dwarf is roughly
     * twice as distant as the local star, leaving a legible deep-space gap.
     */
    private static final Vec3 COLD_SYSTEM_CENTER = new Vec3(32400.0, 102.0, 20550.0);
    private static final Vec3 COLD_STAR_OFFSET = new Vec3(6000.0, 6898.0, 11000.0);

    private static final StellarVisualProfile MAIN_STAR = new StellarVisualProfile(
            new Vec3(18000.0, 9000.0, -14000.0),
            0xFFFFF9DD, STAR_COLOR_MAIN, 0xFFFFB84A,
            new StellarDistanceResponse(24478.0, 11.5F, 0.65F, 2.20F,
                    1.80F, 1.45F, 0.18F, 0.45F),
            2.75F, 0.42F, 0.0F, 0.018F, 34, 0);
    private static final StellarVisualProfile RED_DWARF = new StellarVisualProfile(
            COLD_SYSTEM_CENTER.add(COLD_STAR_OFFSET),
            0xFFFFC090, STAR_COLOR_RED_DWARF, 0xFFFF3028,
            new StellarDistanceResponse(14303.2, 7.5F, 0.60F, 2.40F,
                    1.60F, 1.45F, 0.15F, 0.38F),
            3.25F, 0.22F, 0.90F, 0.037F, 18, 44);

    private static final StarmapBodyVisual BARREN_BODY = StarmapBodyVisual.builder(
                    StarmapBodyType.ROCKY, 0xFFC8A060, 18, 0x5A17B4E1L)
            .secondaryColor(0xFF765936)
            .surfaceDetail(0.78F)
            .texture("starboundmc:textures/gui/starmap/bodies/barren.png")
            .focusTexture("starboundmc:textures/gui/starmap/bodies/barren_focus.png")
            .build();
    private static final StarmapBodyVisual LUSH_BODY = StarmapBodyVisual.builder(
                    StarmapBodyType.TERRESTRIAL, 0xFF58C458, 18, 0x34C92D71L)
            .secondaryColor(0xFF286B45)
            .atmosphere(0xFF8CCBFF, 0.48F)
            .surfaceDetail(0.66F)
            .texture("starboundmc:textures/gui/starmap/bodies/lush.png")
            .focusTexture("starboundmc:textures/gui/starmap/bodies/lush_focus.png")
            .build();
    private static final StarmapBodyVisual MOLTEN_BODY = StarmapBodyVisual.builder(
                    StarmapBodyType.VOLCANIC, 0xFFE06040, 12, 0x718F2BC3L)
            .secondaryColor(0xFFFFB038)
            .atmosphere(0xFFFF6A32, 0.16F)
            .surfaceDetail(0.84F)
            .texture("starboundmc:textures/gui/starmap/bodies/molten.png")
            .build();
    private static final StarmapBodyVisual GAS_GIANT_BODY = StarmapBodyVisual.builder(
                    StarmapBodyType.GAS_GIANT, 0xFFE8A860, 22, 0x26D7A91CL)
            .secondaryColor(0xFFB86648)
            .atmosphere(0xFFFFD6A0, 0.62F)
            .bands(0.88F)
            .rings(0xFFD8C8A0, 0.46F)
            .build();
    private static final StarmapBodyVisual ROCKY_MOON_BODY = StarmapBodyVisual.builder(
                    StarmapBodyType.ROCKY, 0xFF909090, 10, 0x63E14AB5L)
            .secondaryColor(0xFF5E6268)
            .surfaceDetail(0.90F)
            .build();
    private static final StarmapBodyVisual FROZEN_BODY = StarmapBodyVisual.builder(
                    StarmapBodyType.ICY, 0xFF60A8E8, 18, 0x49B3C762L)
            .secondaryColor(0xFFB7E8FF)
            .atmosphere(0xFF9AD8FF, 0.24F)
            .surfaceDetail(0.58F)
            .texture("starboundmc:textures/gui/starmap/bodies/frozen.png")
            .focusTexture("starboundmc:textures/gui/starmap/bodies/frozen_focus.png")
            .build();

    private static final List<StarSystem> SYSTEMS = buildSystems();
    private static final GalaxySpatialIndex SPATIAL_INDEX = GalaxySpatialIndex.build(SYSTEMS);
    private static final Map<String, StarSystem> BY_ID = new HashMap<>();
    private static final Map<String, PlanetEntry> ENTRIES = new HashMap<>();
    private static final Map<Planet, StarSystem> BY_PLANET = new EnumMap<>(Planet.class);

    static
    {
        for (StarSystem system : SYSTEMS)
        {
            BY_ID.put(system.getSystemId(), system);
            for (PlanetEntry entry : system.getEntries())
            {
                ENTRIES.put(entry.getEntryId(), entry);
                if (entry.getDestination() != null)
                    BY_PLANET.put(entry.getDestination(), system);
            }
        }
    }

    private static List<StarSystem> buildSystems()
    {
        List<StarSystem> systems = new ArrayList<>();

        // ---- 第一恒星系: 荒芜(内) · 主世界 · 熔岩月亮(主世界卫星) · 气态巨行星(外) + 岩石卫星 ----
        systems.add(new StarSystem(SYS_MAIN, "starmap.system.sys1", "starmap.system.sys1.desc",
                "starmap.type.yellow_dwarf", MAIN_STAR,
                GalaxyMapPosition.fromPixelCenter(62, 84, GALAXY_MAP_WIDTH, GALAXY_MAP_HEIGHT),
                new Vec3(-1500.0, 102.0, -700.0), 5500.0, List.of(
                new PlanetEntry("sys1:barren", "starmap.entry.sys1.barren.name",
                        "planet.starboundmc.barren", "starmap.entry.sys1.barren.desc",
                        Planet.BARREN, 2, 52, 135.0F, null, BARREN_BODY),
                new PlanetEntry("sys1:lush", "starmap.entry.sys1.lush.name",
                        "planet.starboundmc.lush", "starmap.entry.sys1.lush.desc",
                        Planet.LUSH, 1, 84, 315.0F, null, LUSH_BODY),
                new PlanetEntry("sys1:molten", "starmap.entry.sys1.molten.name",
                        "planet.starboundmc.molten", "starmap.entry.sys1.molten.desc",
                        Planet.MOLTEN, 6, 22, 90.0F, "sys1:lush", MOLTEN_BODY),
                new PlanetEntry("sys1:gasgiant", "starmap.entry.sys1.gasgiant.name",
                        "starmap.type.gas_giant", "starmap.entry.sys1.gasgiant.desc",
                        null, 0, 116, 200.0F, null, GAS_GIANT_BODY),
                new PlanetEntry("sys1:rockymoon", "starmap.entry.sys1.rockymoon.name",
                        "starmap.type.rocky_moon", "starmap.entry.sys1.rockymoon.desc",
                        null, 0, 20, 30.0F, "sys1:gasgiant", ROCKY_MOON_BODY)
        )));

        // ---- 第二恒星系: 强辐射的暗淡红矮星, 目前只有寒冷世界 ----
        systems.add(new StarSystem(SYS_COLD, "starmap.system.sys2", "starmap.system.sys2.desc",
                "starmap.type.red_dwarf", RED_DWARF,
                GalaxyMapPosition.fromPixelCenter(208, 152, GALAXY_MAP_WIDTH, GALAXY_MAP_HEIGHT),
                COLD_SYSTEM_CENTER, 4500.0, List.of(
                new PlanetEntry("sys2:frozen", "starmap.entry.sys2.frozen.name",
                        "planet.starboundmc.frozen", "starmap.entry.sys2.frozen.desc",
                        Planet.FROZEN, 4, 84, 270.0F, null, FROZEN_BODY)
        )));

        return systems;
    }

    public static List<StarSystem> all()
    {
        return SYSTEMS;
    }

    public static GalaxySpatialIndex spatialIndex()
    {
        return SPATIAL_INDEX;
    }

    public static StarSystem byId(String systemId)
    {
        return BY_ID.get(systemId);
    }

    /** Entry id of the system a given entry belongs to, or null. */
    public static String systemIdOfEntry(String entryId)
    {
        if (entryId == null || entryId.isEmpty())
            return null;
        int idx = entryId.indexOf(':');
        return idx > 0 ? entryId.substring(0, idx) : null;
    }

    public static PlanetEntry entryById(String entryId)
    {
        return entryId == null ? null : ENTRIES.get(entryId);
    }

    /** Star system containing the supplied destination, or null for an unknown body. */
    public static StarSystem systemOfPlanet(Planet planet)
    {
        return planet == null ? null : BY_PLANET.get(planet);
    }
}
