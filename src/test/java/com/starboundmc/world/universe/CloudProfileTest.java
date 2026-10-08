package com.starboundmc.world.universe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudProfileTest
{
    private static final String LUSH_ID = "sys1:lush";
    private static final String CLOUD_TEXTURE = "starboundmc:textures/planet/lush_clouds.png";

    @Test
    void legacyProfilesDefaultToNoClouds()
    {
        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"point_color\":-1}"))
                .getOrThrow();

        assertFalse(profile.hasClouds());
        assertEquals(Optional.empty(), profile.cloudTexture());
        assertEquals(BodySpaceVisualProfile.DEFAULT_CLOUD_SHELL_SCALE, profile.cloudShellScale(), 0.0F);
        assertEquals(BodySpaceVisualProfile.DEFAULT_CLOUD_OPACITY, profile.cloudOpacity(), 0.0F);
        assertEquals(BodySpaceVisualProfile.DEFAULT_CLOUD_DRIFT_RATE, profile.cloudDriftRate(), 0.0F);
    }

    @Test
    void lushCloudProfileSurvivesBuiltInCodecAndGeneratedDatapackRoundTrips()
    {
        BodySpaceVisualProfile builtIn = body(BuiltInUniverse.systems().get(0), LUSH_ID)
                .spaceVisual().orElseThrow();
        assertCloudValues(builtIn);
        for (StarSystemDefinition system : BuiltInUniverse.systems())
        {
            for (CelestialBodyDefinition candidate : system.bodies())
            {
                if (!candidate.entryId().equals(LUSH_ID))
                    assertFalse(candidate.spaceVisual().orElseThrow().hasClouds(),
                            candidate.entryId() + " should not have a cloud shell in PR5A");
            }
        }

        var encoded = BodySpaceVisualProfile.CODEC.encodeStart(JsonOps.INSTANCE, builtIn).getOrThrow();
        BodySpaceVisualProfile decoded = BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE, encoded)
                .getOrThrow();
        assertCloudValues(decoded);

        try (InputStream stream = CloudProfileTest.class.getResourceAsStream(
                "/data/starboundmc/starboundmc/star_system/sys1.json"))
        {
            assertNotNull(stream, "missing generated sys1 datapack resource");
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            StarSystemDefinition generated = StarSystemDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                    .getOrThrow();
            BodySpaceVisualProfile generatedLush = body(generated, LUSH_ID).spaceVisual().orElseThrow();
            assertCloudValues(generatedLush);
        }
        catch (java.io.IOException exception)
        {
            throw new AssertionError("could not read generated sys1 datapack", exception);
        }
    }

    @Test
    void cloudProfileRejectsInvalidShellOpacityAndDriftValues()
    {
        assertRejected("\"cloud_shell_scale\":1.0");
        assertRejected("\"cloud_shell_scale\":1.051");
        assertRejected("\"cloud_opacity\":-0.01");
        assertRejected("\"cloud_opacity\":1.01");
        assertRejected("\"cloud_drift_rate\":1.0e39");
    }

    private static CelestialBodyDefinition body(StarSystemDefinition system, String entryId)
    {
        return system.bodies().stream()
                .filter(candidate -> candidate.entryId().equals(entryId))
                .findFirst()
                .orElseThrow();
    }

    private static void assertCloudValues(BodySpaceVisualProfile profile)
    {
        assertTrue(profile.hasClouds());
        assertEquals(Optional.of(CLOUD_TEXTURE), profile.cloudTexture());
        assertEquals(1.008F, profile.cloudShellScale(), 0.0F);
        assertEquals(0.82F, profile.cloudOpacity(), 0.0F);
        assertEquals(0.0025F, profile.cloudDriftRate(), 0.0F);
    }

    private static void assertRejected(String field)
    {
        String json = "{\"point_color\":-1," + field + "}";
        assertThrows(IllegalArgumentException.class,
                () -> BodySpaceVisualProfile.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)),
                "profile should reject " + field);
    }
}
