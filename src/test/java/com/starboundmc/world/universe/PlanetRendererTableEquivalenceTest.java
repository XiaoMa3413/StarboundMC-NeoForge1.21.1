package com.starboundmc.world.universe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Migration gate A9: the definition layer reproduces the renderer's per-planet
 * tables exactly.
 *
 * <p>The values below were read out of {@code PlanetRenderer} before it was
 * migrated, where they lived as per-body {@code EnumMap}s and three {@code switch}
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

    /** Specular strength, roughness, and fresnel strength for the first material pass. */
    private static final Map<String, float[]> MATERIALS = Map.of(
            "sys1:lush", new float[] {0.14F, 0.56F, 0.05F},
            "sys1:molten", new float[] {0.025F, 0.78F, 0.0F},
            "sys1:gasgiant", new float[] {0.02F, 0.92F, 0.035F},
            "sys1:rockymoon", new float[] {0.0F, 1.0F, 0.0F},
            "sys2:frozen", new float[] {0.34F, 0.30F, 0.12F},
            "sys1:barren", new float[] {0.02F, 0.92F, 0.0F});

    /** Shell scale, atmosphere night fraction and terminator twilight strength. */
    private static final Map<String, float[]> ATMOSPHERE_TUNING = Map.of(
            "sys1:lush", new float[] {1.050F, 0.045F, 0.55F},
            "sys1:molten", new float[] {1.030F, 0.04F, 0.30F},
            "sys1:gasgiant", new float[] {1.070F, 0.10F, 0.20F},
            "sys2:frozen", new float[] {1.040F, 0.07F, 0.25F},
            "sys1:barren", new float[] {1.020F, 0.025F, 0.15F});

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
    void materialParametersMatchTheInitialBodyProfiles()
    {
        for (var entry : MATERIALS.entrySet())
        {
            BodySpaceVisualProfile profile = visual(entry.getKey());
            float[] expected = entry.getValue();
            String id = entry.getKey();
            assertEquals(expected[0], profile.specularStrength(), 0.0F, id + " specular strength");
            assertEquals(expected[1], profile.roughness(), 0.0F, id + " roughness");
            assertEquals(expected[2], profile.fresnelStrength(), 0.0F, id + " fresnel strength");
        }
    }

    @Test
    void atmosphereTuningMatchesEachAuthoredBodyProfile()
    {
        for (var entry : ATMOSPHERE_TUNING.entrySet())
        {
            BodySpaceVisualProfile profile = visual(entry.getKey());
            float[] expected = entry.getValue();
            String id = entry.getKey();
            assertEquals(expected[0], profile.atmosphereShellScale(), 0.0F, id + " shell scale");
            assertEquals(expected[1], profile.atmosphereNightFraction(), 0.0F, id + " night fraction");
            assertEquals(expected[2], profile.atmosphereTwilightStrength(), 0.0F,
                    id + " twilight strength");
        }

        BodySpaceVisualProfile rockyMoon = visual("sys1:rockymoon");
        assertEquals(BodySpaceVisualProfile.DEFAULT_ATMOSPHERE_SHELL_SCALE,
                rockyMoon.atmosphereShellScale(), 0.0F);
        assertEquals(BodySpaceVisualProfile.DEFAULT_ATMOSPHERE_NIGHT_FRACTION,
                rockyMoon.atmosphereNightFraction(), 0.0F);
        assertEquals(0.0F, rockyMoon.atmosphereTwilightStrength(), 0.0F);
    }

    @Test
    void legacyDatapackProfileDefaultsToNoMaterialResponse()
    {
        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"point_color\":-1}"))
                .result().orElseThrow();

        assertEquals(0.0F, profile.specularStrength(), 0.0F);
        assertEquals(1.0F, profile.roughness(), 0.0F);
        assertEquals(0.0F, profile.fresnelStrength(), 0.0F);
        assertEquals(0.0F, profile.emissiveStrength(), 0.0F);
        assertEquals(BodySpaceVisualProfile.DEFAULT_ATMOSPHERE_SHELL_SCALE,
                profile.atmosphereShellScale(), 0.0F);
        assertEquals(BodySpaceVisualProfile.DEFAULT_ATMOSPHERE_NIGHT_FRACTION,
                profile.atmosphereNightFraction(), 0.0F);
        assertEquals(0.0F, profile.atmosphereTwilightStrength(), 0.0F);
    }

    @Test
    void moltenProfileUsesItsPackedEmissiveMaskWithoutChangingBaseMaterial()
    {
        BodySpaceVisualProfile molten = visual("sys1:molten");
        assertEquals(Optional.of("starboundmc:textures/planet/molten_material.png"),
                molten.materialMask());
        assertEquals(0.50F, molten.emissiveStrength(), 0.0F);
        assertEquals(0.025F, molten.specularStrength(), 0.0F);
        assertEquals(0.78F, molten.roughness(), 0.0F);
        assertEquals(0.0F, molten.fresnelStrength(), 0.0F);

        for (String id : new String[] {"sys1:lush", "sys1:barren", "sys1:gasgiant",
                "sys1:rockymoon", "sys2:frozen"})
        {
            assertEquals(0.0F, visual(id).emissiveStrength(), 0.0F,
                    id + " must not gain emissive response");
        }
    }

    @Test
    void moltenEmissiveProfileSurvivesCodecRoundTrip()
    {
        BodySpaceVisualProfile molten = visual("sys1:molten");
        var encoded = BodySpaceVisualProfile.CODEC.encodeStart(JsonOps.INSTANCE, molten).getOrThrow();
        BodySpaceVisualProfile decoded = BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();

        assertEquals(molten.materialMask(), decoded.materialMask());
        assertEquals(molten.emissiveStrength(), decoded.emissiveStrength(), 0.0F);
    }

    @Test
    void atmosphereTuningSurvivesCodecRoundTrip()
    {
        for (String id : ATMOSPHERE_TUNING.keySet())
        {
            BodySpaceVisualProfile expected = visual(id);
            var encoded = BodySpaceVisualProfile.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
            BodySpaceVisualProfile decoded = BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE, encoded)
                    .getOrThrow();

            assertEquals(expected.atmosphereShellScale(), decoded.atmosphereShellScale(), 0.0F, id);
            assertEquals(expected.atmosphereNightFraction(), decoded.atmosphereNightFraction(), 0.0F, id);
            assertEquals(expected.atmosphereTwilightStrength(), decoded.atmosphereTwilightStrength(), 0.0F, id);
        }
    }

    @Test
    void invalidAtmosphereTuningIsRejected()
    {
        assertThrows(IllegalArgumentException.class, () -> profileWithAtmosphereTuning(1.004F, 0.08F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> profileWithAtmosphereTuning(1.101F, 0.08F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> profileWithAtmosphereTuning(1.055F, -0.01F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> profileWithAtmosphereTuning(1.055F, 0.251F, 0.0F));
        assertThrows(IllegalArgumentException.class, () -> profileWithAtmosphereTuning(1.055F, 0.08F, -0.01F));
        assertThrows(IllegalArgumentException.class, () -> profileWithAtmosphereTuning(1.055F, 0.08F, 1.01F));
    }

    private static BodySpaceVisualProfile profileWithAtmosphereTuning(
            float shellScale, float nightFraction, float twilightStrength)
    {
        return new BodySpaceVisualProfile(Optional.empty(), Optional.empty(), 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0xFFFFFFFF, 0.20F, 0.00375F, 0.10F, 0.0F, 1.0F, 0.0F, Optional.empty(), shellScale, nightFraction, twilightStrength, Optional.empty(), BodySpaceVisualProfile.DEFAULT_CLOUD_SHELL_SCALE, BodySpaceVisualProfile.DEFAULT_CLOUD_OPACITY, BodySpaceVisualProfile.DEFAULT_CLOUD_DRIFT_RATE);
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
        // Exactly the six the old Planet.values() draw order covered, including the
        // gas giant and its moon, which the legacy renderer drew from their berth.
        assertEquals(6, CATALOG.spaceRenderedBodies().size(),
                "the renderer iterates this list instead of Planet.values()");
        for (CelestialBodyDefinition body : CATALOG.spaceRenderedBodies())
        {
            assertTrue(body.spaceVisual().isPresent(),
                    body.entryId() + " is drawn but has no space visual");
        }
    }

    /**
     * The drawn set must be exactly the six bodies the legacy renderer drew: it
     * iterated {@code Planet.values()}, which covered the gas giant and its moon
     * too. Widening it would put new planets in the sky, which is a feature, not a
     * migration.
     */
    @Test
    void theDrawOrderCoversExactlyTheBodiesThatWereDrawnBefore()
    {
        var drawn = CATALOG.spaceRenderedBodies().stream()
                .map(CelestialBodyDefinition::entryId).sorted().toList();
        assertEquals(java.util.List.of(
                        "sys1:barren", "sys1:gasgiant", "sys1:lush", "sys1:molten",
                        "sys1:rockymoon", "sys2:frozen"),
                drawn,
                "the rendered body set must match what the legacy enum drew");

        // The two outer bodies are drawn AND on the star map: the star map has its
        // own visual, so being drawable in the sky does not imply anything about it.
        for (String drawnId : new String[] {"sys1:gasgiant", "sys1:rockymoon"})
        {
            var body = CATALOG.body(drawnId).orElseThrow();
            assertTrue(body.isSpaceRendered(), drawnId + " must be drawn");
            assertTrue(body.starmapVisual().getMarkerSize() > 0, drawnId + " stays on the map");
        }
    }
}
