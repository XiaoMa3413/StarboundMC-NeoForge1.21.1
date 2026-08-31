package com.starboundmc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M0 voxel drop resources: the two global loot modifiers must reference the
 * registered serializer, carry the agreed amounts (passive 5, hostile 20) and
 * point at entity-type tags that exist, parse and stay disjoint.
 */
final class VoxelDropResourcesTest {
    private static final Path DATA = Path.of("src/main/resources/data/starboundmc");

    private static final Map<String, Integer> EXPECTED_DROPS = Map.of(
            "passive_voxel_drops", 5,
            "hostile_voxel_drops", 20);

    @Test
    void lootModifiersDeclareAmountsAndTagConditions() throws IOException {
        for (var entry : EXPECTED_DROPS.entrySet()) {
            Path file = DATA.resolve("loot_modifiers/" + entry.getKey() + ".json");
            assertTrue(Files.isRegularFile(file), file.toString());
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            assertEquals("starboundmc:add_voxel_drop", root.get("type").getAsString(), file.toString());
            assertEquals(entry.getValue(), root.get("amount").getAsInt(), file.toString());

            JsonObject condition = root.getAsJsonArray("conditions").get(0).getAsJsonObject();
            assertEquals("minecraft:entity_properties", condition.get("condition").getAsString());
            assertEquals("this", condition.get("entity").getAsString());
            String type = condition.getAsJsonObject("predicate").get("type").getAsString();
            String expectedTag = entry.getKey().startsWith("passive")
                    ? "#starboundmc:passive_voxel_drop"
                    : "#starboundmc:hostile_voxel_drop";
            assertEquals(expectedTag, type, file.toString());
        }
    }

    @Test
    void entityTagsAreValidIdsAndDisjoint() throws IOException {
        List<String> passive = tagValues("passive_voxel_drop");
        List<String> hostile = tagValues("hostile_voxel_drop");
        assertFalse(passive.isEmpty());
        assertFalse(hostile.isEmpty());
        for (String id : passive) {
            assertTrue(id.startsWith("minecraft:"), id);
            assertFalse(hostile.contains(id), "entity in both drop tiers: " + id);
        }
        for (String id : hostile) {
            assertTrue(id.startsWith("minecraft:"), id);
        }
    }

    @Test
    void globalLootModifierManifestListsBothEntries() throws IOException {
        Path manifest = Path.of("src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json");
        assertTrue(Files.isRegularFile(manifest), manifest.toString());
        JsonObject root = JsonParser.parseString(Files.readString(manifest)).getAsJsonObject();
        assertEquals(false, root.get("replace").getAsBoolean(), manifest.toString());
        List<String> entries = root.getAsJsonArray("entries").asList().stream()
                .map(JsonElement::getAsString)
                .toList();
        assertTrue(entries.contains("starboundmc:passive_voxel_drops"), entries.toString());
        assertTrue(entries.contains("starboundmc:hostile_voxel_drops"), entries.toString());
    }

    private static List<String> tagValues(String tag) throws IOException {
        Path file = DATA.resolve("tags/entity_type/" + tag + ".json");
        assertTrue(Files.isRegularFile(file), file.toString());
        JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals(false, root.get("replace").getAsBoolean(), file.toString());
        return root.getAsJsonArray("values").asList().stream()
                .map(JsonElement::getAsString)
                .toList();
    }
}
