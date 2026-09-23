package com.starboundmc.world.universe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Codec behaviour for the optional cloud layer block. */
final class BodyCloudProfileCodecTest {
    private static final JsonOps JSON = JsonOps.INSTANCE;

    @Test
    void spinRateAndOpacityDefaultWhenAbsent() {
        JsonObject json = new JsonObject();
        json.addProperty("texture", "starboundmc:textures/planet/lush_clouds.png");

        BodyCloudProfile cloud = BodyCloudProfile.CODEC.parse(JSON, json).getOrThrow();

        assertEquals(0.0055F, cloud.spinRate());
        assertEquals(0.85F, cloud.opacity());
        assertEquals("starboundmc:textures/planet/lush_clouds.png", cloud.texture());
    }

    @Test
    void explicitValuesRoundTrip() {
        JsonObject json = new JsonObject();
        json.addProperty("texture", "starboundmc:textures/planet/lush_clouds.png");
        json.addProperty("spin_rate", 0.004);
        json.addProperty("opacity", 0.6);

        BodyCloudProfile cloud = BodyCloudProfile.CODEC.parse(JSON, json).getOrThrow();

        assertEquals(0.004F, cloud.spinRate());
        assertEquals(0.6F, cloud.opacity());
    }

    @Test
    void aMissingTextureIsRejected() {
        assertThrows(RuntimeException.class,
                () -> BodyCloudProfile.CODEC.parse(JSON, new JsonObject()).getOrThrow());
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
        cloud.addProperty("texture", "starboundmc:textures/planet/lush_clouds.png");
        JsonObject json = new JsonObject();
        json.addProperty("point_color", 0xFF68D68A);
        json.add("cloud", cloud);

        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JSON, json).getOrThrow();

        assertTrue(profile.cloud().isPresent());
        assertEquals("starboundmc:textures/planet/lush_clouds.png",
                profile.cloud().orElseThrow().texture());
    }
}
