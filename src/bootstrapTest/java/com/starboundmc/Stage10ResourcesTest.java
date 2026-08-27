package com.starboundmc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage10ResourcesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/starboundmc");
    private static final Path DATA = Path.of("src/main/resources/data/starboundmc");
    private static final Path GENERATED = Path.of("src/generated/resources/data/starboundmc");

    private static final List<String> BLOCKS = List.of(
            "matter_manipulator_workbench", "teleporter", "ship_console", "ship_engine",
            "captain_chair", "fuel_controller", "ship_crate", "ship_door", "tungsten_ore",
            "titanium_ore", "durasteel_ore", "star_core_ore", "titanium_alloy_furnace");

    private static final List<String> ITEMS = List.of(
            "matter_manipulator", "matter_manipulator_module", "matter_manipulator_workbench",
            "teleporter", "ship_console", "captain_chair", "fuel_controller", "ship_crate",
            "ship_door", "ship_engine", "tungsten_ore", "titanium_ore", "durasteel_ore",
            "star_core_ore", "titanium_alloy_furnace", "raw_tungsten", "raw_titanium",
            "raw_durasteel", "raw_star_core", "tungsten_ingot", "titanium_ingot",
            "durasteel_ingot", "star_core_fragment");

    @Test
    void everyRegisteredBlockAndItemHasAClientDefinition() {
        for (String block : BLOCKS) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("blockstates/" + block + ".json")), block);
            assertTrue(Files.isRegularFile(ASSETS.resolve("models/block/" + block + ".json")), block);
        }
        for (String item : ITEMS) {
            assertTrue(Files.isRegularFile(ASSETS.resolve("models/item/" + item + ".json")), item);
        }
    }

    @Test
    void customModelAndBlockstateReferencesResolve() throws IOException {
        try (Stream<Path> files = Files.walk(ASSETS)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json"))
                    .filter(file -> file.toString().contains("models") || file.toString().contains("blockstates"))
                    .toList()) {
                visitReferences(JsonParser.parseString(Files.readString(path)), path);
            }
        }
    }

    @Test
    void recipesAndLootTablesUseMinecraft121DirectoriesAndItemStacks() throws IOException {
        assertFalse(Files.exists(DATA.resolve("loot_tables")));
        try (Stream<Path> loot = Files.list(DATA.resolve("loot_table/blocks"))) {
            assertEquals(BLOCKS.size(), loot.filter(path -> path.toString().endsWith(".json")).count());
        }
        try (Stream<Path> recipes = Files.list(DATA.resolve("recipe"))) {
            List<Path> files = recipes.filter(path -> path.toString().endsWith(".json")).toList();
            assertEquals(4, files.size());
            for (Path path : files) {
                JsonObject result = json(path).getAsJsonObject("result");
                assertNotNull(result, path.toString());
                assertTrue(result.has("id"), path.toString());
                assertFalse(result.has("item"), path.toString());
            }
        }
    }

    @Test
    void customAudioIsAbsentAndBothLocalesAreComplete() throws IOException {
        assertFalse(Files.exists(ASSETS.resolve("sounds.json")));
        for (String sound : List.of("warp_start", "warp_loop", "warp_end", "teleporter_use")) {
            assertFalse(Files.exists(ASSETS.resolve("sounds/" + sound + ".ogg")), sound);
        }

        Set<String> english = json(ASSETS.resolve("lang/en_us.json")).keySet();
        Set<String> chinese = json(ASSETS.resolve("lang/zh_cn.json")).keySet();
        assertEquals(new HashSet<>(english), new HashSet<>(chinese));
        assertTrue(english.contains("message.starboundmc.warp.no_fuel"));
        assertTrue(english.contains("gui.starboundmc.starmap.detail.navigation"));
    }

    @Test
    void shipRegistryResourcesComeFromDatagen() {
        assertTrue(Files.isRegularFile(GENERATED.resolve("dimension/ship.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve("dimension_type/ship.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve("worldgen/biome/ship.json")));
        assertFalse(Files.exists(DATA.resolve("dimension/ship.json")));
    }

    private static void visitReferences(JsonElement element, Path source) {
        if (element.isJsonObject()) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                if ((entry.getKey().equals("parent") || entry.getKey().equals("model"))
                        && entry.getValue().isJsonPrimitive()) {
                    assertModel(entry.getValue().getAsString(), source);
                } else if (entry.getKey().equals("textures") && entry.getValue().isJsonObject()) {
                    for (JsonElement texture : entry.getValue().getAsJsonObject().asMap().values()) {
                        if (texture.isJsonPrimitive()) assertTexture(texture.getAsString(), source);
                    }
                }
                visitReferences(entry.getValue(), source);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) visitReferences(child, source);
        }
    }

    private static void assertModel(String id, Path source) {
        if (!id.startsWith("starboundmc:")) return;
        Path model = ASSETS.resolve("models/" + id.substring("starboundmc:".length()) + ".json");
        assertTrue(Files.isRegularFile(model), () -> source + " -> " + id);
    }

    private static void assertTexture(String id, Path source) {
        if (id.startsWith("#") || !id.startsWith("starboundmc:")) return;
        Path texture = ASSETS.resolve("textures/" + id.substring("starboundmc:".length()) + ".png");
        assertTrue(Files.isRegularFile(texture), () -> source + " -> " + id);
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
