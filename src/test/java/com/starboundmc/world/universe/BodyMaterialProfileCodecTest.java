package com.starboundmc.world.universe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Data compatibility for the material block added to {@code BodySpaceVisualProfile}.
 *
 * <p>The block and every field inside it are optional, so universe data written
 * before materials existed must decode to the diffuse-only default rather than
 * fail; a datapack that authors one gets exactly the values it wrote.</p>
 */
final class BodyMaterialProfileCodecTest {
    private static final JsonOps JSON = JsonOps.INSTANCE;

    @Test
    void anEmptyMaterialBlockDecodesToTheDiffuseOnlyDefault() {
        BodyMaterialProfile material = BodyMaterialProfile.CODEC
                .parse(JSON, new JsonObject()).getOrThrow();

        assertEquals(BodyMaterialProfile.DEFAULT.roughness(), material.roughness());
        assertEquals(0.0F, material.specularStrength());
        assertEquals(0.0F, material.fresnelStrength());
        assertEquals(0.0F, material.oceanSpecular());
        assertEquals(0.0F, material.emissiveStrength());
        assertEquals(0, material.emissiveColor());
        assertTrue(material.emissiveMask().isEmpty());
    }

    @Test
    void aFullMaterialBlockRoundTrips() {
        JsonObject json = new JsonObject();
        json.addProperty("roughness", 0.45);
        json.addProperty("specular_strength", 0.32);
        json.addProperty("fresnel_strength", 0.15);
        json.addProperty("ocean_roughness", 0.15);
        json.addProperty("ocean_specular", 0.85);
        json.addProperty("emissive_strength", 1.0);
        json.addProperty("emissive_color", 0xFF8A3C);
        json.addProperty("emissive_mask", "starboundmc:textures/planet/molten_emissive.png");

        BodyMaterialProfile material = BodyMaterialProfile.CODEC.parse(JSON, json).getOrThrow();

        assertEquals(0.45F, material.roughness());
        assertEquals(0.32F, material.specularStrength());
        assertEquals(0.15F, material.fresnelStrength());
        assertEquals(0.15F, material.oceanRoughness());
        assertEquals(0.85F, material.oceanSpecular());
        assertEquals(1.0F, material.emissiveStrength());
        assertEquals(0xFF8A3C, material.emissiveColor());
        assertEquals(Optional.of("starboundmc:textures/planet/molten_emissive.png"),
                material.emissiveMask());
    }

    @Test
    void outOfRangeMaterialValuesAreRejectedRatherThanClamped() {
        JsonObject json = new JsonObject();
        json.addProperty("roughness", 1.5);
        assertThrows(RuntimeException.class, () -> BodyMaterialProfile.CODEC.parse(JSON, json).getOrThrow());
    }

    @Test
    void aSpaceVisualProfileWithoutAMaterialBlockDecodesToEmpty() {
        JsonObject json = new JsonObject();
        json.addProperty("point_color", 0xFF68D68A);

        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JSON, json).getOrThrow();

        assertTrue(profile.material().isEmpty());
    }

    @Test
    void aSpaceVisualProfileWithAMaterialBlockKeepsIt() {
        JsonObject material = new JsonObject();
        material.addProperty("roughness", 0.85);
        material.addProperty("specular_strength", 0.18);
        JsonObject json = new JsonObject();
        json.addProperty("point_color", 0xFF68D68A);
        json.add("material", material);

        BodySpaceVisualProfile profile = BodySpaceVisualProfile.CODEC.parse(JSON, json).getOrThrow();

        assertTrue(profile.material().isPresent());
        assertEquals(0.85F, profile.material().orElseThrow().roughness());
        assertEquals(0.18F, profile.material().orElseThrow().specularStrength());
    }
}
