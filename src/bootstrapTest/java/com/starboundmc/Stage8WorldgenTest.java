package com.starboundmc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage8WorldgenTest {
    private static final Path DATA = Path.of("src/main/resources/data/starboundmc");

    @Test
    void everyAuthoredDimensionHasAValidTypeAndExpectedGenerator() throws IOException {
        Map<String, String> generators = Map.of(
                "ship", "starboundmc:ship",
                "frozen", "minecraft:noise",
                "barren", "starboundmc:barren",
                "molten", "starboundmc:molten");
        for (var entry : generators.entrySet()) {
            JsonObject dimension = json("dimension/" + entry.getKey() + ".json");
            assertEquals("starboundmc:" + entry.getKey(), dimension.get("type").getAsString());
            assertEquals(entry.getValue(), dimension.getAsJsonObject("generator").get("type").getAsString());

            JsonObject type = json("dimension_type/" + entry.getKey() + ".json");
            assertTrue(type.get("height").getAsInt() > 0);
            assertEquals(0, type.get("height").getAsInt() % 16);
            assertEquals(0, type.get("min_y").getAsInt() % 16);
        }
    }

    @Test
    void shipDimensionIsVoidAndUsesTheDedicatedBiome() throws IOException {
        JsonObject generator = json("dimension/ship.json").getAsJsonObject("generator");
        JsonObject biomeSource = generator.getAsJsonObject("biome_source");
        assertEquals("minecraft:fixed", biomeSource.get("type").getAsString());
        assertEquals("starboundmc:ship", biomeSource.get("biome").getAsString());
        assertTrue(Files.isRegularFile(DATA.resolve("worldgen/biome/ship.json")));
    }

    @Test
    void planetProfilesRetainTheirGameplayConstraints() throws IOException {
        JsonObject frozen = json("dimension/frozen.json").getAsJsonObject("generator")
                .getAsJsonObject("biome_source");
        JsonObject barren = json("dimension/barren.json").getAsJsonObject("generator")
                .getAsJsonObject("biome_source");
        JsonObject moltenType = json("dimension_type/molten.json");
        assertEquals("starboundmc:filtered", frozen.get("type").getAsString());
        assertEquals("starboundmc:filtered", barren.get("type").getAsString());
        assertTrue(frozen.getAsJsonArray("allowed").size() >= 10);
        assertTrue(barren.getAsJsonArray("allowed").size() >= 7);
        assertFalse(moltenType.get("has_ceiling").getAsBoolean());
    }

    @Test
    void worldgenCodecsAndLifecycleUseNeoForge121Signatures() throws IOException {
        String registry = source("world/ModWorldgen.java");
        String ship = source("world/ShipChunkGenerator.java");
        String barren = source("world/BarrenChunkGenerator.java");
        String molten = source("world/MoltenChunkGenerator.java");
        String placement = source("event/ShipPlacementHandler.java");
        assertTrue(registry.contains("ShipChunkGenerator.CODEC"));
        assertTrue(registry.contains("FilteredBiomeSource.CODEC"));
        assertTrue(ship.contains("MapCodec<ShipChunkGenerator>"));
        assertTrue(barren.contains("super.fillFromNoise(blender, randomState, structureManager, chunk)"));
        assertTrue(molten.contains("fillFromNoise(Blender blender"));
        assertFalse(ship.contains("Executor executor"));
        assertFalse(placement.contains("net.minecraftforge"));
    }

    private static JsonObject json(String relativePath) throws IOException {
        return JsonParser.parseString(Files.readString(DATA.resolve(relativePath))).getAsJsonObject();
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
