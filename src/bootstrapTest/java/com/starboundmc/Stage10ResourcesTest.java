package com.starboundmc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
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
            "titanium_ore", "durasteel_ore", "star_core_ore", "titanium_alloy_furnace",
            "ship_ai_terminal", "voxel_refinery", "voxel_printing_station", "ship_engine_unit",
            "fuel_crystal_ore", "hull_plating", "reinforced_hull", "hull_window",
            "industrial_light", "hull_hazard", "hull_grate", "beacon_emitter", "life_support_station", "epp_service_station");

    private static final List<String> ITEMS = List.of(
            "basic_circuit_board", "relay_data_core", "jump_thruster", "epp_mk1", "epp_mk2", "epp_mk2_upgrade_kit", "epp_mk3", "epp_mk3_upgrade_kit", "heating_module_1", "cooling_module_1", "oxygen_canister", "empty_oxygen_canister",
            "matter_manipulator", "matter_manipulator_module", "matter_manipulator_workbench",
            "teleporter", "ship_console", "captain_chair", "fuel_controller", "ship_crate",
            "ship_door", "ship_engine", "tungsten_ore", "titanium_ore", "durasteel_ore",
            "star_core_ore", "titanium_alloy_furnace", "raw_tungsten", "raw_titanium",
            "raw_durasteel", "raw_star_core", "tungsten_ingot", "titanium_ingot",
            "durasteel_ingot", "star_core_fragment", "ship_ai_terminal", "voxel",
            "voxel_refinery", "voxel_printing_station", "sublight_ignition_core", "ship_engine_unit",
            "fuel_crystal_ore", "fuel_crystal", "hull_plating", "reinforced_hull", "hull_window",
            "industrial_light", "hull_hazard", "hull_grate", "beacon_emitter", "life_support_station", "epp_service_station");

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
            assertEquals(38, files.size());
            int printingRecipes = 0;
            int decompositionRecipes = 0;
            for (Path path : files) {
                JsonObject root = json(path);
                String type = root.get("type").getAsString();
                switch (type) {
                    case "starboundmc:voxel_printing" -> printingRecipes++;
                    case "starboundmc:voxel_decomposition" -> decompositionRecipes++;
                    case "minecraft:crafting_shaped", "minecraft:smelting" -> {
                        // vanilla machine recipes kept for existing machines
                    }
                    default -> throw new AssertionError("unexpected recipe type " + type + " in " + path);
                }
                if (type.equals("starboundmc:voxel_printing")) {
                    JsonObject result = root.getAsJsonObject("result");
                    assertNotNull(result, path.toString());
                    assertTrue(result.has("id"), path.toString());
                    assertTrue(result.has("count"), path.toString());
                }
            }
            assertEquals(14, printingRecipes, "fourteen printing recipes");
            assertEquals(14, decompositionRecipes, "fourteen decomposition recipes");
        }
    }

    /**
     * Loot tables are parsed by the server at startup, not by the build, so a
     * malformed number provider is invisible until a world loads and then only shows
     * up as a log line: the block silently drops nothing.
     *
     * <p>{@code set_count}'s {@code count} is a loot NumberProvider, whose uniform
     * range is {@code min}/{@code max}. Worldgen's IntProvider spells the same idea
     * {@code min_inclusive}/{@code max_inclusive}, and copying that form across is
     * the mistake this catches.</p>
     */
    @Test
    void everyLootTableNumberProviderUsesTheLootFieldNames() throws IOException {
        Path lootDirectory = DATA.resolve("loot_table/blocks");
        try (Stream<Path> files = Files.list(lootDirectory)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonObject root = json(path);
                for (var pool : root.getAsJsonArray("pools")) {
                    for (var entry : pool.getAsJsonObject().getAsJsonArray("entries")) {
                        var functions = entry.getAsJsonObject().getAsJsonArray("functions");
                        if (functions == null)
                            continue;
                        for (var function : functions) {
                            var count = function.getAsJsonObject().get("count");
                            if (count == null || !count.isJsonObject())
                                continue;
                            JsonObject provider = count.getAsJsonObject();
                            if (!"minecraft:uniform".equals(provider.get("type").getAsString()))
                                continue;
                            assertTrue(provider.has("min") && provider.has("max"),
                                    path.getFileName() + ": a uniform loot NumberProvider needs "
                                            + "min/max, not the worldgen min_inclusive/max_inclusive");
                        }
                    }
                }
            }
        }
    }

    @Test
    void engineUnitHasIndependentModelAndAnimatedPixelTextures() throws IOException {
        var model = json(ASSETS.resolve("models/block/ship_engine_unit.json"));
        assertTrue(model.getAsJsonArray("elements").size() > 10, "Engine needs an actual machine model");
        for (var part : model.getAsJsonArray("elements")) {
            for (String edge : List.of("from", "to"))
                for (var coordinate : part.getAsJsonObject().getAsJsonArray(edge))
                    assertTrue(coordinate.getAsDouble() >= 0 && coordinate.getAsDouble() <= 16);
        }
        for (String layer : List.of("casing", "metal", "vent", "panel", "coil", "core")) {
            assertEquals("starboundmc:block/ship_engine_unit_" + layer,
                    model.getAsJsonObject("textures").get(layer).getAsString());
            var image = ImageIO.read(ASSETS.resolve("textures/block/ship_engine_unit_" + layer + ".png").toFile());
            assertEquals(32, image.getWidth());
            assertEquals(layer.equals("core") ? 128 : 32, image.getHeight());
        }
        assertTrue(json(ASSETS.resolve("textures/block/ship_engine_unit_core.png.mcmeta"))
                .getAsJsonObject("animation").get("interpolate").getAsBoolean());
        assertFalse(Files.exists(DATA.resolve("recipe/ship_engine_unit.json")));
    }

    @Test
    void ignitionCoreRecipeAndSocketTagAgree() throws IOException {
        var recipe = json(DATA.resolve("recipe/print_sublight_ignition_core.json"));
        assertEquals("starboundmc:voxel_printing", recipe.get("type").getAsString());
        var materials = recipe.getAsJsonArray("materials");
        assertEquals(2, materials.size());
        assertEquals("minecraft:diamond", materials.get(0).getAsJsonObject()
                .getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(3, materials.get(0).getAsJsonObject().get("count").getAsInt());
        assertEquals("starboundmc:voxel", materials.get(1).getAsJsonObject()
                .getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals(50, materials.get(1).getAsJsonObject().get("count").getAsInt());
        assertEquals(5, recipe.get("print_seconds").getAsInt());
        assertEquals("starboundmc:sublight_ignition_core", recipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, recipe.getAsJsonObject("result").get("count").getAsInt());
        var modules = json(DATA.resolve("tags/item/engine_repair_modules.json")).getAsJsonArray("values");
        assertEquals(1, modules.size(), "No unimplemented hyperdrive module");
        assertEquals("starboundmc:sublight_ignition_core", modules.get(0).getAsString());
        assertFalse(Files.exists(DATA.resolve("recipe/print_ship_engine.json")));
    }

    @Test
    void sublightRepairWorkbenchUsesConfirmedPrintingRecipe() throws IOException {
        JsonObject root = json(DATA.resolve("recipe/print_matter_manipulator_workbench.json"));
        assertEquals("starboundmc:voxel_printing", root.get("type").getAsString());
        assertFalse(root.has("voxel_cost"));
        assertEquals(5, root.get("print_seconds").getAsInt());

        int iron = 0;
        int redstone = 0;
        int craftingTable = 0;
        int voxels = 0;
        for (JsonElement element : root.getAsJsonArray("materials")) {
            JsonObject material = element.getAsJsonObject();
            String item = material.getAsJsonObject("ingredient").get("item").getAsString();
            switch (item) {
                case "minecraft:iron_ingot" -> iron = material.get("count").getAsInt();
                case "minecraft:redstone" -> redstone = material.get("count").getAsInt();
                case "minecraft:crafting_table" -> craftingTable = material.get("count").getAsInt();
                case "starboundmc:voxel" -> voxels = material.get("count").getAsInt();
                default -> throw new AssertionError("unexpected workbench ingredient " + item);
            }
        }
        assertEquals(4, iron);
        assertEquals(1, redstone);
        assertEquals(1, craftingTable);
        assertEquals(50, voxels);
        assertEquals("starboundmc:matter_manipulator_workbench",
                root.getAsJsonObject("result").get("id").getAsString());
        assertEquals(1, root.getAsJsonObject("result").get("count").getAsInt());
    }

    @Test
    void onlyOriginalNovaAudioIsPresentAndBothLocalesAreComplete() throws IOException {
        JsonObject sounds = json(ASSETS.resolve("sounds.json"));
        assertEquals(Set.of("nova_text"), sounds.keySet());
        assertEquals("starboundmc:ui/nova_text", sounds.getAsJsonObject("nova_text")
                .getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString());
        assertTrue(Files.isRegularFile(ASSETS.resolve("sounds/ui/nova_text.ogg")));
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
    void transparentBlockTexturesDeclareARenderType() throws IOException {
        // A block's transparency needs two separate things, and getting only one
        // is silently wrong: partially transparent texels in the PNG, and a
        // `render_type` in the block model telling the game to bake the block
        // into a non-solid chunk layer. Without the second, transparent texels
        // render as opaque black — which is how the hull window shipped looking
        // like a solid metal slab, and the grating like a dark plate with no
        // holes. Neither is caught by any other test, so assert the pairing.
        //
        // In NeoForge 1.21.1 the key is `render_type` (the vanilla BlockModel
        // does not read it; NeoForge's ExtendedBlockModelDeserializer does) and
        // the value is a render-type name resolved through NamedRenderTypeManager.
        Set<String> known = Set.of("minecraft:solid", "minecraft:cutout",
                "minecraft:cutout_mipped", "minecraft:cutout_mipped_all",
                "minecraft:translucent", "minecraft:tripwire");
        int checked = 0;
        try (Stream<Path> textures = Files.list(ASSETS.resolve("textures/block"))) {
            for (Path texture : textures.filter(p -> p.toString().endsWith(".png")).toList()) {
                String name = texture.getFileName().toString().replace(".png", "");
                BufferedImage image = ImageIO.read(texture.toFile());
                assertNotNull(image, name);
                boolean hasTransparency = false;
                for (int x = 0; x < image.getWidth() && !hasTransparency; x++) {
                    for (int y = 0; y < image.getHeight(); y++) {
                        if ((image.getRGB(x, y) >>> 24) < 255) {
                            hasTransparency = true;
                            break;
                        }
                    }
                }
                if (!hasTransparency) {
                    continue;
                }
                Path model = ASSETS.resolve("models/block/" + name + ".json");
                assertTrue(Files.isRegularFile(model),
                        name + " has transparent texels but no block model");
                JsonObject root = json(model);
                assertTrue(root.has("render_type"),
                        name + " has transparent texels but its model declares no render_type,"
                                + " so the gaps would render opaque");
                assertTrue(known.contains(root.get("render_type").getAsString()),
                        name + " declares an unknown render_type: "
                                + root.get("render_type").getAsString());
                checked++;
            }
        }
        // Guards against the scan silently finding nothing to check.
        assertTrue(checked > 0, "no transparent block textures were found to verify");
    }

    @Test
    void shipAiPortraitTextureIsPackaged() {        for (String texture : List.of("nova_bust", "nova_body", "nova_eyes")) {
            assertTrue(Files.isRegularFile(
                    ASSETS.resolve("textures/gui/ship_ai/" + texture + ".png")), texture);
        }
        assertTrue(Files.isRegularFile(ASSETS.resolve("lss/nova_broadcast_hud.lss")));
    }

    @Test
    void shipAiTerminalUsesDedicatedAnimatedTextures() throws IOException {
        JsonObject textures = json(ASSETS.resolve("models/block/ship_ai_terminal.json"))
                .getAsJsonObject("textures");
        assertEquals("starboundmc:block/command_deck_atlas", textures.get("atlas").getAsString());
        assertEquals("starboundmc:block/ship_ai_terminal_screen", textures.get("screen").getAsString());
        assertFalse(textures.toString().contains("ship_console"));

        BufferedImage atlas = ImageIO.read(ASSETS.resolve("textures/block/command_deck_atlas.png").toFile());
        assertNotNull(atlas);
        assertEquals(256, atlas.getWidth());
        assertEquals(256, atlas.getHeight());
        BufferedImage edges = ImageIO.read(ASSETS.resolve("textures/block/command_deck_edges.png").toFile());
        assertNotNull(edges);
        assertEquals(1024, edges.getWidth());
        assertEquals(1024, edges.getHeight());
        JsonObject navigation = json(ASSETS.resolve("models/block/starmap_terminal.json"));
        assertEquals("minecraft:block/block", navigation.get("parent").getAsString());
        assertFalse(navigation.getAsJsonArray("elements").isEmpty());

        Path screen = ASSETS.resolve("textures/block/ship_ai_terminal_screen.png");
        BufferedImage image = ImageIO.read(screen.toFile());
        assertNotNull(image);
        assertEquals(48, image.getWidth());
        assertEquals(96, image.getHeight());
        assertTrue(Files.isRegularFile(Path.of(screen + ".mcmeta")));
        JsonObject animation = json(Path.of(screen + ".mcmeta")).getAsJsonObject("animation");
        assertEquals(48, animation.get("width").getAsInt());
        assertEquals(32, animation.get("height").getAsInt());
    }

    @Test
    void commandDeckFacesKeepConsistentTexelDensity() throws IOException {
        for (String name : List.of("ship_ai_terminal", "starmap_terminal")) {
            for (JsonElement entry : json(ASSETS.resolve("models/block/" + name + ".json"))
                    .getAsJsonArray("elements")) {
                JsonObject element = entry.getAsJsonObject();
                double[] size = new double[3];
                for (int axis = 0; axis < 3; axis++) {
                    size[axis] = element.getAsJsonArray("to").get(axis).getAsDouble()
                            - element.getAsJsonArray("from").get(axis).getAsDouble();
                }
                for (var faceEntry : element.getAsJsonObject("faces").entrySet()) {
                    JsonObject face = faceEntry.getValue().getAsJsonObject();
                    String texture = face.get("texture").getAsString();
                    if (texture.equals("#screen")) continue;
                    String side = faceEntry.getKey();
                    double width = side.equals("east") || side.equals("west") ? size[2] : size[0];
                    double height = side.equals("up") || side.equals("down") ? size[2] : size[1];
                    var uv = face.getAsJsonArray("uv");
                    if (!texture.equals("#chart") && (Math.min(width, height) <= 4
                            || side.equals("east") || side.equals("west"))) {
                        assertEquals("#edges", texture, name + ":" + side + " needs complete tailored artwork");
                    }
                    assertEquals(width / height,
                            (uv.get(2).getAsDouble() - uv.get(0).getAsDouble())
                                    / (uv.get(3).getAsDouble() - uv.get(1).getAsDouble()),
                            0.00001, name + ":" + side + " must not stretch");
                    if (texture.equals("#chart")) continue;
                    double texelsPerUv = texture.equals("#edges") ? 64 : 16;
                    assertEquals(width * 8, texelsPerUv * (uv.get(2).getAsDouble() - uv.get(0).getAsDouble()),
                            0.00001, name + ":" + side + " horizontal density");
                    assertEquals(height * 8, texelsPerUv * (uv.get(3).getAsDouble() - uv.get(1).getAsDouble()),
                            0.00001, name + ":" + side + " vertical density");
                }
            }
        }
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
