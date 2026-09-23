package com.starboundmc.world.universe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shipped datapack data must stay in step with {@link BuiltInUniverse}.
 *
 * <p>The datapack registry replaces the Java baseline at runtime, so data
 * generated before a profile field existed silently wins: the renderer would
 * read defaults no matter what the Java definitions say. Regenerating with
 * {@code runData} is the fix; this test is what makes forgetting it fail
 * loudly instead of surfacing as "the effect does nothing" in the client.</p>
 */
final class GeneratedUniverseDataTest {
    private static final JsonOps JSON = JsonOps.INSTANCE;
    private static final Path SYSTEM_DIR =
            Path.of("src/generated/resources/data/starboundmc/starboundmc/star_system");

    @Test
    void generatedSystemDataCarriesTheSameMaterialsAsTheJavaBaseline() throws Exception {
        for (StarSystemDefinition expected : BuiltInUniverse.systems())
        {
            Path file = SYSTEM_DIR.resolve(expected.systemId() + ".json");
            assertTrue(Files.exists(file), "missing generated data for " + expected.systemId());

            JsonObject json = com.google.gson.JsonParser.parseString(Files.readString(file))
                    .getAsJsonObject();
            StarSystemDefinition decoded = StarSystemDefinition.CODEC.parse(JSON, json).getOrThrow();

            for (CelestialBodyDefinition expectedBody : expected.bodies())
            {
                String id = expectedBody.entryId();
                CelestialBodyDefinition actual = decoded.bodies().stream()
                        .filter(body -> body.entryId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("generated data lost body " + id));

                BodyMaterialProfile expectedMaterial = expectedBody.spaceVisual()
                        .map(BodySpaceVisualProfile::material)
                        .orElse(Optional.empty())
                        .orElse(BodyMaterialProfile.DEFAULT);
                BodyMaterialProfile actualMaterial = actual.spaceVisual()
                        .map(BodySpaceVisualProfile::material)
                        .orElse(Optional.empty())
                        .orElse(BodyMaterialProfile.DEFAULT);

                assertEquals(expectedMaterial.roughness(), actualMaterial.roughness(), id + " roughness");
                assertEquals(expectedMaterial.specularStrength(), actualMaterial.specularStrength(),
                        id + " specular strength");
                assertEquals(expectedMaterial.fresnelStrength(), actualMaterial.fresnelStrength(),
                        id + " fresnel strength");
                assertEquals(expectedMaterial.oceanRoughness(), actualMaterial.oceanRoughness(),
                        id + " ocean roughness");
                assertEquals(expectedMaterial.oceanSpecular(), actualMaterial.oceanSpecular(),
                        id + " ocean specular");
                assertEquals(expectedMaterial.emissiveStrength(), actualMaterial.emissiveStrength(),
                        id + " emissive strength");
                assertEquals(expectedMaterial.emissiveMask(), actualMaterial.emissiveMask(),
                        id + " emissive mask");
            }
        }
    }
}
