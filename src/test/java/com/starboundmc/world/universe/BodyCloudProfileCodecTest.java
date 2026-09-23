package com.starboundmc.world.universe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Codec behaviour for the optional procedural cloud block. The block carries no
 * texture: the cloud density is computed in the shader, so the only data is the
 * rotation rate, the opacity and the coverage threshold.
 */
final class BodyCloudProfileCodecTest {
    private static final JsonOps JSON = JsonOps.INSTANCE;

    @Test
    void anEmptyBlockIsRejectedRatherThanInheritingHiddenDefaults() {
        assertThrows(RuntimeException.class,
                () -> BodyCloudProfile.CODEC.parse(JSON, new JsonObject()).getOrThrow());
    }

    @Test
    void explicitValuesRoundTrip() {
        JsonObject json = new JsonObject();
        json.addProperty("spin_rate", 0.004);
        json.addProperty("opacity", 0.6);
        json.addProperty("coverage", 0.5);

        BodyCloudProfile cloud = BodyCloudProfile.CODEC.parse(JSON, json).getOrThrow();

        assertEquals(0.004F, cloud.spinRate());
        assertEquals(0.6F, cloud.opacity());
        assertEquals(0.5F, cloud.coverage());
    }

    @Test
    void outOfRangeValuesAreRejected() {
        JsonObject high = new JsonObject();
        high.addProperty("opacity", 1.5);
        assertThrows(RuntimeException.class,
                () -> BodyCloudProfile.CODEC.parse(JSON, high).getOrThrow());

        JsonObject badCoverage = new JsonObject();
        badCoverage.addProperty("coverage", -0.1);
        assertThrows(RuntimeException.class,
                () -> BodyCloudProfile.CODEC.parse(JSON, badCoverage).getOrThrow());
    }

    @Test
    void aSpaceVisualProfileWithoutACloudBlockDecodesToEmpty() {
        JsonObject json = new JsonObject();
        json.addProperty("point_color", 0xFF68D68A);

        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JSON, json).getOrThrow();

        assertTrue(profile.cloud().isEmpty());
    }

    @Test
    void aSpaceVisualProfileWithACloudBlockKeepsIt() {
        JsonObject cloud = new JsonObject();
        cloud.addProperty("spin_rate", 0.009);
        cloud.addProperty("opacity", 0.85);
        cloud.addProperty("coverage", 0.5);
        JsonObject json = new JsonObject();
        json.addProperty("point_color", 0xFF68D68A);
        json.add("cloud", cloud);

        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JSON, json).getOrThrow();

        assertTrue(profile.cloud().isPresent());
        assertEquals(0.5F, profile.cloud().orElseThrow().coverage());
    }
}
