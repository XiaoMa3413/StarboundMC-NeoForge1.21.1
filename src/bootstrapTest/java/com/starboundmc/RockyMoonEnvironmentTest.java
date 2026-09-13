package com.starboundmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.starboundmc.client.RockyMoonDimensionEffects;
import com.starboundmc.client.RockyMoonSkyRenderer;
import com.starboundmc.world.RockyMoonPlanet;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rocky moon polish pass: the airless dimension renders with no fog at all,
 * giant ore veins (dozens to 100+ ores per vein) generate through a custom
 * feature, and the gas giant wears the Saturn texture with its ring aligned
 * to the body's equator.
 *
 * <p>Worldgen JSON is validated structurally: the full vanilla bootstrap is
 * not available in plain JUnit (FeatureFlags need the FML loading runtime),
 * so codec round-trips are exercised in-game instead.</p>
 */
final class RockyMoonEnvironmentTest {
    private static final Path DATA = Path.of("src/main/resources/data/starboundmc");
    private static final Path ASSETS = Path.of("src/main/resources/assets/starboundmc");

    // ---- fog-free airless sky ----

    @Test
    void rockyMoonHasNoFogOfAnyKind() {
        RockyMoonDimensionEffects effects = new RockyMoonDimensionEffects();
        assertFalse(effects.isFoggyAt(0, 0), "nether-style thick fog must stay off");
        Vec3 tinted = effects.getBrightnessDependentFogColor(new Vec3(0.9D, 0.8D, 0.7D), 1.0F);
        assertEquals(0.0D, tinted.x, 1e-9);
        assertEquals(0.0D, tinted.y, 1e-9);
        assertEquals(0.0D, tinted.z, 1e-9);
    }

    @Test
    void terrainFogEventOnlyTriggersOnTheRockyMoon() {
        assertTrue(RockyMoonSkyRenderer.shouldDisableFog(RockyMoonPlanet.ROCKY_MOON_LEVEL, FogType.NONE));
        assertFalse(RockyMoonSkyRenderer.shouldDisableFog(RockyMoonPlanet.ROCKY_MOON_LEVEL, FogType.WATER),
                "fluid fog (lava/powder snow) must survive");
        assertFalse(RockyMoonSkyRenderer.shouldDisableFog(Level.OVERWORLD, FogType.NONE),
                "the rocky moon must be the only fog-free dimension");
    }

    // ---- giant ore veins ----

    @Test
    void giantVeinConfiguredFeaturesUseTheCustomWormFeature() {
        assertGiantVein("moon_giant_iron", 120,
                "minecraft:iron_ore", "minecraft:deepslate_iron_ore");
        assertGiantVein("moon_giant_gold", 80,
                "minecraft:gold_ore", "minecraft:deepslate_gold_ore");
        assertGiantVein("moon_giant_fuel_crystal", 96,
                "starboundmc:fuel_crystal_ore");
    }

    private static void assertGiantVein(String name, int expectedSize, String... oreStates) {
        JsonObject root = json(DATA.resolve("worldgen/configured_feature/" + name + ".json"));
        assertEquals("starboundmc:giant_ore_vein", root.get("type").getAsString());
        JsonObject config = root.getAsJsonObject("config");
        assertEquals(expectedSize, config.get("size").getAsInt(), name + " ore budget");
        Set<String> states = new HashSet<>();
        for (JsonElement element : config.getAsJsonArray("targets")) {
            JsonObject target = element.getAsJsonObject();
            assertEquals("minecraft:tag_match",
                    target.getAsJsonObject("target").get("predicate_type").getAsString(),
                    name + " predicate");
            states.add(target.getAsJsonObject("state").get("Name").getAsString());
        }
        assertEquals(Set.of(oreStates), states, name + " ore states");
    }

    @Test
    void giantVeinPlacementIsRareAndBounded() {
        assertPlaced("moon_giant_iron", 6);
        assertPlaced("moon_giant_gold", 10);
        assertPlaced("moon_giant_fuel_crystal", 8);
    }

    private static void assertPlaced(String name, int rarity) {
        JsonObject root = json(DATA.resolve("worldgen/placed_feature/" + name + ".json"));
        String featureId = root.get("feature").getAsString();
        assertTrue(Files.isRegularFile(DATA.resolve("worldgen/configured_feature/"
                + featureId.substring("starboundmc:".length()) + ".json")), featureId);
        JsonArray placement = root.getAsJsonArray("placement");
        assertEquals(3, placement.size(), name + " placement stack");
        JsonObject rarityFilter = placement.get(0).getAsJsonObject();
        assertEquals("minecraft:rarity_filter", rarityFilter.get("type").getAsString());
        assertEquals(rarity, rarityFilter.get("chance").getAsInt(), name + " rarity");
        assertEquals("minecraft:in_square",
                placement.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:height_range",
                placement.get(2).getAsJsonObject().get("type").getAsString());
    }

    @Test
    void moonBiomeModifierWiresSmallAndGiantVeins() {
        JsonObject modifier = json(DATA.resolve("neoforge/biome_modifier/rocky_moon_ores.json"));
        assertEquals("neoforge:add_features", modifier.get("type").getAsString());
        assertEquals("underground_ores", modifier.get("step").getAsString());
        Set<String> features = new HashSet<>();
        for (JsonElement element : modifier.getAsJsonArray("features"))
            features.add(element.getAsString());
        for (String expected : List.of("starboundmc:moon_iron", "starboundmc:moon_gold",
                "starboundmc:moon_fuel_crystal", "starboundmc:moon_giant_iron",
                "starboundmc:moon_giant_gold", "starboundmc:moon_giant_fuel_crystal")) {
            assertTrue(features.contains(expected), expected + " must be wired");
            assertTrue(Files.isRegularFile(DATA.resolve("worldgen/placed_feature/"
                    + expected.substring("starboundmc:".length()) + ".json")), expected);
        }
    }

    // ---- Saturn retexture ----

    @Test
    void gasGiantUsesSaturnRetextureWithMatchingStarmapSprites() throws IOException {
        BufferedImage planet = ImageIO.read(ASSETS.resolve("textures/planet/gasgiant.png").toFile());
        assertEquals(4096, planet.getWidth());
        assertEquals(2048, planet.getHeight());
        // Saturn's bands are pale warm gold: red slightly leads green, blue trails.
        float red = 0.0F, green = 0.0F, blue = 0.0F;
        int samples = 0;
        for (int y = 700; y < 1350; y += 25) {
            for (int x = 1200; x < 2900; x += 25) {
                int argb = planet.getRGB(x, y);
                red += (argb >> 16) & 0xFF;
                green += (argb >> 8) & 0xFF;
                blue += argb & 0xFF;
                samples++;
            }
        }
        red /= samples;
        green /= samples;
        blue /= samples;
        assertTrue(red > 180.0F && green > red * 0.90F && blue < red * 0.85F,
                "pale gold Saturn bands expected, got " + red + "," + green + "," + blue);

        for (String sprite : new String[] {"gasgiant", "gasgiant_focus"}) {
            Path path = ASSETS.resolve("textures/gui/starmap/bodies/" + sprite + ".png");
            BufferedImage image = ImageIO.read(path.toFile());
            int size = sprite.endsWith("_focus") ? 128 : 64;
            assertEquals(size, image.getWidth(), sprite);
            assertEquals(size, image.getHeight(), sprite);
            assertEquals(0, image.getRGB(0, 0) >>> 24, sprite + " corner stays transparent");
            assertTrue(image.getRGB(size / 2, size / 2) >>> 24 > 200, sprite + " disc is opaque");
        }
    }

    @Test
    void fuelCrystalArtReadsAsCyanEnergyCrystal() throws IOException {
        assertCrystalHue(ASSETS.resolve("textures/item/fuel_crystal.png"));
        assertCrystalHue(ASSETS.resolve("textures/block/fuel_crystal_ore.png"));
    }

    /** Crystal pixels must stay in the cyan accent family (blue well over red). */
    private static void assertCrystalHue(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        assertEquals(16, image.getWidth());
        assertEquals(16, image.getHeight());
        int crystalPixels = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int argb = image.getRGB(x, y);
                if ((argb >>> 24) < 200)
                    continue;
                int red = (argb >> 16) & 0xFF;
                int blue = argb & 0xFF;
                // Strongly cyan: blue clearly beats red (neutral grey stone and
                // dark teal outlines don't qualify).
                if (blue >= red + 40)
                    crystalPixels++;
            }
        }
        assertTrue(crystalPixels >= 25, path + " lost its cyan crystal body, only "
                + crystalPixels + " strongly-cyan pixels remain");
    }

    private static JsonObject json(Path path) {
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException(path.toString(), exception);
        }
    }
}
