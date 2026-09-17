package com.starboundmc.world.universe;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A9: the definition layer reproduces the renderer's per-planet
 * tables exactly.
 *
 * <p>The values below were read out of {@code PlanetRenderer} before it was
 * migrated, where they lived as four {@code EnumMap}s and three {@code switch}
 * statements. Losing one of them would not fail any other test — the planet would
 * just quietly lose its atmosphere glow, spin at the wrong rate, or come out
 * flat-shaded — so they are pinned here individually.</p>
 *
 * <p>The expected numbers are literals rather than reads of the renderer, which
 * is the point: the expectation cannot drift along with the implementation.</p>
 */
class PlanetRendererTableEquivalenceTest
{
    private static final UniverseCatalog CATALOG = UniverseCatalog.of(BuiltInUniverse.systems());

    /** Atmosphere tint (r,g,b) and peak alpha, per body. */
    private static final Map<String, float[]> ATMOSPHERE = Map.of(
            "sys1:lush", new float[] {0.30F, 0.60F, 1.0F, 0.20F},
            "sys1:molten", new float[] {1.0F, 0.45F, 0.20F, 0.26F},
            "sys2:frozen", new float[] {0.55F, 0.78F, 1.0F, 0.23F},
            "sys1:barren", new float[] {0.75F, 0.65F, 0.50F, 0.15F});

    /** Fixed body orientation: x=axis tilt, y=fixed yaw, z=fixed roll. */
    private static final Map<String, float[]> ORIENTATION = Map.of(
            "sys1:lush", new float[] {23.0F, 15.0F, 0.0F},
            "sys1:molten", new float[] {6.0F, 210.0F, 0.0F},
            "sys2:frozen", new float[] {32.0F, 125.0F, 0.0F},
            "sys1:barren", new float[] {12.0F, 285.0F, 0.0F});

    /** Point-marker colour used when the body is too distant to shade. */
    private static final Map<String, Integer> POINT_COLOR = Map.of(
            "sys1:lush", 0xFF68D68A,
            "sys1:molten", 0xFFFF8A4C,
            "sys2:frozen", 0xFF8FD7FF,
            "sys1:barren", 0xFFD0B07A);

    /** Shading switches: terminator width, spin rate, night floor. */
    private static final Map<String, float[]> SHADING = Map.of(
            "sys1:lush", new float[] {0.24F, 0.00375F, 0.10F},
            "sys1:molten", new float[] {0.16F, 0.0075F, 0.16F},
            "sys2:frozen", new float[] {0.30F, 0.00225F, 0.14F},
            "sys1:barren", new float[] {0.18F, 0.00275F, 0.06F});

    /** The planet texture each body's sphere is drawn with. */
    private static final Map<String, String> TEXTURE = Map.of(
            "sys1:lush", "starboundmc:textures/planet/lush.png",
            "sys1:molten", "starboundmc:textures/planet/molten.png",
            "sys2:frozen", "starboundmc:textures/planet/frozen.png",
            "sys1:barren", "starboundmc:textures/planet/barren.png");

    private static BodySpaceVisualProfile visual(String entryId)
    {
        return CATALOG.body(entryId).orElseThrow().spaceVisual().orElseThrow();
    }

    @Test
    void atmosphereTintAndPeakMatchTheRenderersTables()
    {
        for (var entry : ATMOSPHERE.entrySet())
        {
            BodySpaceVisualProfile profile = visual(entry.getKey());
            float[] expected = entry.getValue();
            String id = entry.getKey();
            assertEquals(expected[0], profile.atmosphereRed(), 0.0F, id + " atmosphere red");
            assertEquals(expected[1], profile.atmosphereGreen(), 0.0F, id + " atmosphere green");
            assertEquals(expected[2], profile.atmosphereBlue(), 0.0F, id + " atmosphere blue");
            assertEquals(expected[3], profile.atmospherePeak(), 0.0F, id + " atmosphere peak");
            assertTrue(profile.hasAtmosphere(), id + " must still have an atmosphere glow");
        }
    }

    @Test
    void bodyOrientationMatchesTheRenderersTables()
    {
        for (var entry : ORIENTATION.entrySet())
        {
            BodySpaceVisualProfile profile = visual(entry.getKey());
            float[] expected = entry.getValue();
            String id = entry.getKey();
            assertEquals(expected[0], profile.orientationTilt(), 0.0F, id + " tilt");
            assertEquals(expected[1], profile.orientationYaw(), 0.0F, id + " yaw");
            assertEquals(expected[2], profile.orientationRoll(), 0.0F, id + " roll");
        }
    }

    @Test
    void pointColorsMatchTheRenderersTables()
    {
        for (var entry : POINT_COLOR.entrySet())
        {
            assertEquals(entry.getValue().intValue(), visual(entry.getKey()).pointColor(),
                    entry.getKey() + " point colour");
        }
    }

    @Test
    void shadingSwitchesMatchTheRenderersSwitches()
    {
        for (var entry : SHADING.entrySet())
        {
            BodySpaceVisualProfile profile = visual(entry.getKey());
            float[] expected = entry.getValue();
            String id = entry.getKey();
            assertEquals(expected[0], profile.terminatorWidth(), 0.0F, id + " terminator width");
            assertEquals(expected[1], profile.spinRate(), 0.0F, id + " spin rate");
            assertEquals(expected[2], profile.nightFloor(), 0.0F, id + " night floor");
        }
    }

    @Test
    void bodyTexturesMatchThePlanetTexturesTheRendererUsed()
    {
        for (var entry : TEXTURE.entrySet())
        {
            assertEquals(entry.getValue(), visual(entry.getKey()).texture().orElseThrow(),
                    entry.getKey() + " texture");
        }
    }

    /**
     * Every body the renderer draws must carry a visual, or it would render with
     * the neutral fallback and silently lose its appearance.
     */
    @Test
    void everySpaceRenderedBodyHasAVisualProfile()
    {
        // Exactly the four the old Planet.values() draw order covered. The gas
        // giant and rocky moon are on the star map but were never drawn in the
        // cockpit window, and A9 is a migration, not a feature addition.
        assertEquals(4, CATALOG.spaceRenderedBodies().size(),
                "the renderer iterates this list instead of Planet.values()");
        for (CelestialBodyDefinition body : CATALOG.spaceRenderedBodies())
        {
            assertTrue(body.spaceVisual().isPresent(),
                    body.entryId() + " is drawn but has no space visual");
        }
    }

    /**
     * The drawn set must be exactly the four bodies the renderer drew before the
     * migration. Widening it would put new planets in the sky.
     */
    @Test
    void theDrawOrderCoversExactlyTheBodiesThatWereDrawnBefore()
    {
        var drawn = CATALOG.spaceRenderedBodies().stream()
                .map(CelestialBodyDefinition::entryId).sorted().toList();
        assertEquals(java.util.List.of(
                        "sys1:barren", "sys1:lush", "sys1:molten", "sys2:frozen"),
                drawn,
                "the rendered body set changed during the migration");

        for (String notDrawn : new String[] {"sys1:gasgiant", "sys1:rockymoon"})
        {
            var body = CATALOG.body(notDrawn).orElseThrow();
            assertFalse(body.isSpaceRendered(), notDrawn + " must not be drawn");
            // Still on the star map, which is what the star map consumes.
            assertTrue(body.starmapVisual().getMarkerSize() > 0, notDrawn + " stays on the map");
        }
    }
}
