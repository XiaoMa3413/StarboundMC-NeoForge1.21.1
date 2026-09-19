package com.starboundmc.world.universe;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.world.GasGiantGeometry;
import com.starboundmc.world.starmap.GalaxyMapPosition;
import com.starboundmc.world.starmap.StarmapBodyType;
import com.starboundmc.world.starmap.StarmapBodyVisual;
import com.starboundmc.world.starmap.StellarDistanceResponse;
import com.starboundmc.world.starmap.StellarVisualProfile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * The universe the mod ships with, as plain definitions.
 *
 * <p>Every value here is a transcription of the shipped {@code StarSystems} /
 * {@code ShipSpace} / {@code PlanetRenderer} constants. Nothing is rebalanced,
 * renamed or rounded: the point of the data layer is to give the existing
 * universe a data-shaped home whose contents are provably identical.
 * {@code LegacyUniverseSnapshotTest} pins the legacy side and the equivalence
 * tests compare the two, so a typo here fails the build instead of moving a
 * planet.</p>
 *
 * <p>This is runtime data, not a build artifact. Datagen turns it into the
 * shipped JSON, and the client falls back to it when no server registry has
 * been synced yet — which is why it lives outside {@link UniverseDatagen}.</p>
 *
 * <p>Entry ids keep the legacy {@code system:body} form. The registry key for a
 * system is {@code starboundmc:<systemId>}, but the {@code entry_id} inside a
 * body stays {@code sys1:lush} because that string is already in saves,
 * packets, the star map and tests.</p>
 */
public final class BuiltInUniverse
{
    /** The starter system. */
    public static final String MAIN_SYSTEM_ID = "sys1";
    /** The remote red-dwarf system. */
    public static final String COLD_SYSTEM_ID = "sys2";
    /**
     * The body the ship starts docked at, and the fallback whenever a location
     * is missing or unreadable. Named here so client state does not need the
     * legacy enum just to express "the starting planet".
     */
    public static final String STARTER_BODY_ID = "sys1:lush";

    /** Galaxy-map authoring canvas the current pixel positions were composed on. */
    private static final int GALAXY_MAP_WIDTH = 250;
    private static final int GALAXY_MAP_HEIGHT = 220;

    private BuiltInUniverse()
    {
    }

    /**
     * The built-in universe as plain definitions.
     *
     * <p>The single source of truth for datagen, the client baseline and tests.
     * Tests cannot stand up a datapack registry, so they assert against this same
     * list rather than a re-typed copy of the data.</p>
     */
    public static List<StarSystemDefinition> systems()
    {
        return List.of(mainSystem(), coldSystem());
    }

    // ------------------------------------------------------------------ sys1

    private static StarSystemDefinition mainSystem()
    {
        StellarVisualProfile star = new StellarVisualProfile(
                new Vec3(18000.0, 9000.0, -14000.0),
                0xFFFFF9DD, 0xFFF2D98B, 0xFFFFB84A,
                new StellarDistanceResponse(24478.0, 11.5F, 0.65F, 2.20F, 1.80F, 1.45F, 0.18F, 0.45F),
                2.75F, 0.42F, 0.0F, 0.018F, 34, 0);

        return new StarSystemDefinition("sys1",
                "starmap.system.sys1", "starmap.system.sys1.desc", "starmap.type.yellow_dwarf",
                star,
                GalaxyMapPosition.fromPixelCenter(62, 84, GALAXY_MAP_WIDTH, GALAXY_MAP_HEIGHT),
                UniversePosition.fromLegacy(new Vec3(-1500.0, 102.0, -700.0)),
                5500.0,
                // The star's art-directed fade ends long before the outer berths:
                // the gas giant parks at ~16000 and its moon at ~16500 from this
                // centre. The body-rendering field has to cover both, or the outer
                // bodies vanish exactly when the ship arrives at them.
                24000.0,
                List.of(
                        body("sys1:barren",
                                "starmap.entry.sys1.barren.name", "planet.starboundmc.barren",
                                "starmap.entry.sys1.barren.desc", 2,
                                BodyOrbitDefinition.aroundStar(52, 135.0F),
                                StarmapBodyVisual.builder(StarmapBodyType.ROCKY, 0xFFC8A060, 18, 0x5A17B4E1L)
                                        .secondaryColor(0xFF765936)
                                        .surfaceDetail(0.78F)
                                        .texture("starboundmc:textures/gui/starmap/bodies/barren.png")
                                        .focusTexture("starboundmc:textures/gui/starmap/bodies/barren_focus.png")
                                        .build(),
                                navigation(-5000.0, 102.0, -2000.0,
                                        -5002.668504563659, 102.8, -1994.1827254324958,
                                        4.0, 335.0),
                                spaceVisual("starboundmc:textures/planet/barren.png",
                                        0.75F, 0.65F, 0.50F, 0.15F,
                                        12.0F, 285.0F, 0.0F, 0xFFD0B07A,
                                        0.18F, 0.00275F, 0.06F),
                                surface("starboundmc:barren", BodySurfaceDefinition.LandingPolicy.SURFACE_SCAN,
                                        new PlanetEnvironmentProfile(true, 0, 0, 0, 1.0F))),

                        body("sys1:lush",
                                "starmap.entry.sys1.lush.name", "planet.starboundmc.lush",
                                "starmap.entry.sys1.lush.desc", 1,
                                BodyOrbitDefinition.aroundStar(84, 315.0F),
                                StarmapBodyVisual.builder(StarmapBodyType.TERRESTRIAL, 0xFF58C458, 18, 0x34C92D71L)
                                        .secondaryColor(0xFF286B45)
                                        .atmosphere(0xFF8CCBFF, 0.48F)
                                        .surfaceDetail(0.66F)
                                        .texture("starboundmc:textures/gui/starmap/bodies/lush.png")
                                        .focusTexture("starboundmc:textures/gui/starmap/bodies/lush_focus.png")
                                        .build(),
                                navigation(0.0, 102.0, 0.0, 0.06, 103.2, 9.6, 6.0, 0.0),
                                spaceVisual("starboundmc:textures/planet/lush.png",
                                        0.30F, 0.60F, 1.0F, 0.20F,
                                        23.0F, 15.0F, 0.0F, 0xFF68D68A,
                                        0.24F, 0.00375F, 0.10F),
                                // The lush world is the vanilla overworld and returns
                                // the player to their respawn anchor.
                                surface("minecraft:overworld", BodySurfaceDefinition.LandingPolicy.OVERWORLD_RESPAWN,
                                        new PlanetEnvironmentProfile(true, 0, 0, 0, 1.0F))),

                        body("sys1:molten",
                                "starmap.entry.sys1.molten.name", "planet.starboundmc.molten",
                                "starmap.entry.sys1.molten.desc", 6,
                                BodyOrbitDefinition.aroundBody("sys1:lush", 22, 90.0F),
                                StarmapBodyVisual.builder(StarmapBodyType.VOLCANIC, 0xFFE06040, 12, 0x718F2BC3L)
                                        .secondaryColor(0xFFFFB038)
                                        .atmosphere(0xFFFF6A32, 0.16F)
                                        .surfaceDetail(0.84F)
                                        .texture("starboundmc:textures/gui/starmap/bodies/molten.png")
                                        .build(),
                                navigation(-82.64625654611092, 98.87302452316077, -4.673526941779457,
                                        -83.94, 99.2, -2.4000000000000004,
                                        6.0 / 3.67, 330.0),
                                spaceVisual("starboundmc:textures/planet/molten.png",
                                        1.0F, 0.45F, 0.20F, 0.26F,
                                        6.0F, 210.0F, 0.0F, 0xFFFF8A4C,
                                        0.16F, 0.0075F, 0.16F),
                                surface("starboundmc:molten", BodySurfaceDefinition.LandingPolicy.SURFACE_SCAN,
                                        new PlanetEnvironmentProfile(true, 0, 1, 0, 1.0F))),

                        body("sys1:gasgiant",
                                "starmap.entry.sys1.gasgiant.name", "starmap.type.gas_giant",
                                "starmap.entry.sys1.gasgiant.desc", 8,
                                BodyOrbitDefinition.aroundStar(116, 200.0F),
                                StarmapBodyVisual.builder(StarmapBodyType.GAS_GIANT, 0xFFE8A860, 22, 0x26D7A91CL)
                                        .secondaryColor(0xFFB86648)
                                        .atmosphere(0xFFFFD6A0, 0.62F)
                                        .bands(0.88F)
                                        .rings(0xFFD8C8A0, 0.46F)
                                        .texture("starboundmc:textures/gui/starmap/bodies/gasgiant.png")
                                        .focusTexture("starboundmc:textures/gui/starmap/bodies/gasgiant_focus.png")
                                        .build(),
                                navigation(-16000.0, 102.0, -7500.0,
                                        -16062.900463338801, 110.0, -7511.819124830719,
                                        40.0, 259.0),
                                // Saturn-like: 26.7-degree tilt, no hard day/night line,
                                // a fast spin so the bands never rest, and a warm night
                                // side. The ring texture is what makes it ringed.
                                spaceVisual("starboundmc:textures/planet/gasgiant.png",
                                        0.99F, 0.91F, 0.70F, 0.24F,
                                        GasGiantGeometry.AXIAL_TILT_DEGREES, GasGiantGeometry.BODY_YAW_DEGREES, 0.0F,
                                        0xFFE4C893, 0.42F, 0.009F, 0.22F,
                                        "starboundmc:textures/planet/gasgiant_ring.png"),
                                // Orbit-only: no surface definition, which is what the
                                // landing button reads to refuse with "no solid surface".
                                Optional.empty()),

                        body("sys1:rockymoon",
                                "starmap.entry.sys1.rockymoon.name", "starmap.type.rocky_moon",
                                "starmap.entry.sys1.rockymoon.desc", 5,
                                BodyOrbitDefinition.aroundBody("sys1:gasgiant", 26, 30.0F),
                                StarmapBodyVisual.builder(StarmapBodyType.ROCKY, 0xFF909090, 10, 0x63E14AB5L)
                                        .secondaryColor(0xFF5E6268)
                                        .surfaceDetail(0.90F)
                                        .texture("starboundmc:textures/gui/starmap/bodies/rockymoon.png")
                                        .focusTexture("starboundmc:textures/gui/starmap/bodies/rockymoon_focus.png")
                                        .build(),
                                navigation(-15781.273564177154, 89.0, -7464.854196469775,
                                        -15787.716310869539, 90.0, -7460.111463601716,
                                        5.0, 306.0),
                                // Airless rock: thin halo, a hard terminator and a black
                                // far side, tumbled so the crater field never looks flat.
                                spaceVisual("starboundmc:textures/planet/rockymoon.png",
                                        0.55F, 0.55F, 0.60F, 0.03F,
                                        8.0F, 160.0F, 0.0F,
                                        0xFFB4B8BE, 0.12F, 0.002F, 0.03F),
                                surface("starboundmc:rockymoon", BodySurfaceDefinition.LandingPolicy.SURFACE_SCAN,
                                        new PlanetEnvironmentProfile(false, 0, 0, 0, 1.0F)))
                ));
    }

    // ------------------------------------------------------------------ sys2

    private static StarSystemDefinition coldSystem()
    {
        StellarVisualProfile star = new StellarVisualProfile(
                new Vec3(38400.0, 7000.0, 31550.0),
                0xFFFFC090, 0xFFCC5A38, 0xFFFF3028,
                new StellarDistanceResponse(14303.2, 7.5F, 0.60F, 2.40F, 1.60F, 1.45F, 0.15F, 0.38F),
                3.25F, 0.22F, 0.90F, 0.037F, 18, 44);

        return new StarSystemDefinition("sys2",
                "starmap.system.sys2", "starmap.system.sys2.desc", "starmap.type.red_dwarf",
                star,
                GalaxyMapPosition.fromPixelCenter(208, 152, GALAXY_MAP_WIDTH, GALAXY_MAP_HEIGHT),
                UniversePosition.fromLegacy(new Vec3(32400.0, 102.0, 20550.0)),
                4500.0,
                List.of(
                        body("sys2:frozen",
                                "starmap.entry.sys2.frozen.name", "planet.starboundmc.frozen",
                                "starmap.entry.sys2.frozen.desc", 4,
                                BodyOrbitDefinition.aroundStar(84, 270.0F),
                                StarmapBodyVisual.builder(StarmapBodyType.ICY, 0xFF60A8E8, 18, 0x49B3C762L)
                                        .secondaryColor(0xFFB7E8FF)
                                        .atmosphere(0xFF9AD8FF, 0.24F)
                                        .surfaceDetail(0.58F)
                                        .texture("starboundmc:textures/gui/starmap/bodies/frozen.png")
                                        .focusTexture("starboundmc:textures/gui/starmap/bodies/frozen_focus.png")
                                        .build(),
                                navigation(32400.0, 102.0, 20550.0,
                                        32408.8, 103.1, 20549.945,
                                        5.5, 90.0),
                                spaceVisual("starboundmc:textures/planet/frozen.png",
                                        0.55F, 0.78F, 1.0F, 0.23F,
                                        32.0F, 125.0F, 0.0F, 0xFF8FD7FF,
                                        0.30F, 0.00225F, 0.14F),
                                surface("starboundmc:frozen", BodySurfaceDefinition.LandingPolicy.SURFACE_SCAN,
                                        new PlanetEnvironmentProfile(true, 1, 0, 0, 1.0F)))
                ));
    }

    // --------------------------------------------------------------- builders

    private static CelestialBodyDefinition body(String entryId, String nameKey, String typeKey,
                                                String descriptionKey, int threatLevel,
                                                BodyOrbitDefinition orbit, StarmapBodyVisual starmapVisual,
                                                Optional<BodyNavigationProfile> navigation,
                                                Optional<BodySpaceVisualProfile> spaceVisual,
                                                Optional<BodySurfaceDefinition> surface)
    {
        return new CelestialBodyDefinition(entryId, nameKey, typeKey, descriptionKey, threatLevel,
                orbit, starmapVisual, navigation, spaceVisual, surface);
    }

    private static Optional<BodyNavigationProfile> navigation(double dockX, double dockY, double dockZ,
                                                              double bodyX, double bodyY, double bodyZ,
                                                              double radius, double yawDock)
    {
        return Optional.of(new BodyNavigationProfile(
                UniversePosition.fromLegacy(new Vec3(dockX, dockY, dockZ)),
                UniversePosition.fromLegacy(new Vec3(bodyX, bodyY, bodyZ)),
                radius, yawDock));
    }

    /**
     * Builds a body's cockpit-window visual.
     *
     * <p>The atmosphere, orientation, point colour and shading numbers are
     * transcriptions of the per-planet tables the renderer used to own.</p>
     */
    private static Optional<BodySpaceVisualProfile> spaceVisual(String texture,
                                                                float atmoRed, float atmoGreen,
                                                                float atmoBlue, float atmoPeak,
                                                                float tilt, float yaw, float roll,
                                                                int pointColor,
                                                                float terminatorWidth,
                                                                float spinRate,
                                                                float nightFloor)
    {
        return spaceVisual(texture, atmoRed, atmoGreen, atmoBlue, atmoPeak, tilt, yaw, roll,
                pointColor, terminatorWidth, spinRate, nightFloor, null);
    }

    /** Variant for a ringed body, whose ring texture is the only extra datum. */
    private static Optional<BodySpaceVisualProfile> spaceVisual(String texture,
                                                                float atmoRed, float atmoGreen,
                                                                float atmoBlue, float atmoPeak,
                                                                float tilt, float yaw, float roll,
                                                                int pointColor,
                                                                float terminatorWidth,
                                                                float spinRate,
                                                                float nightFloor,
                                                                String ringTexture)
    {
        return Optional.of(new BodySpaceVisualProfile(
                Optional.ofNullable(texture), atmoRed, atmoGreen, atmoBlue, atmoPeak,
                tilt, yaw, roll, pointColor, terminatorWidth, spinRate, nightFloor,
                Optional.ofNullable(ringTexture)));
    }

    private static Optional<BodySurfaceDefinition> surface(String dimension,
                                                           BodySurfaceDefinition.LandingPolicy policy,
                                                           PlanetEnvironmentProfile environment)
    {
        return Optional.of(new BodySurfaceDefinition(
                net.minecraft.resources.ResourceLocation.parse(dimension), policy, environment));
    }

    /** Registry keys for the built-in systems. */
    public static ResourceKey<StarSystemDefinition> mainSystemKey()
    {
        return ModUniverseRegistries.systemKey("sys1");
    }

    public static ResourceKey<StarSystemDefinition> coldSystemKey()
    {
        return ModUniverseRegistries.systemKey("sys2");
    }
}
