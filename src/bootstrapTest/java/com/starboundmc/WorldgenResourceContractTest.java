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

final class WorldgenResourceContractTest {
    private static final Path MAIN_DATA = Path.of("src/main/resources/data/starboundmc");
    private static final Path GENERATED_DATA = Path.of("src/generated/resources/data/starboundmc");

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

    private static JsonObject json(String relativePath) throws IOException {
        return JsonParser.parseString(Files.readString(resource(relativePath))).getAsJsonObject();
    }

    private static Path resource(String relativePath) {
        Path authored = MAIN_DATA.resolve(relativePath);
        return Files.isRegularFile(authored) ? authored : GENERATED_DATA.resolve(relativePath);
    }

}
