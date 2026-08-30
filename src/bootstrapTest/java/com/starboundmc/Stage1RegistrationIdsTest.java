package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class Stage1RegistrationIdsTest {
    @Test
    void keepsPublishedBlockIds() throws IOException {
        assertSourceIds("block/ModBlocks.java", "(?:registerCopy|registerSimpleBlock|registerBlock)\\(\\s*\"([^\"]+)\"", Set.of(
                "matter_manipulator_workbench", "teleporter", "ship_console", "ship_engine",
                "captain_chair", "fuel_controller", "ship_crate", "ship_door", "tungsten_ore",
                "titanium_ore", "durasteel_ore", "star_core_ore", "titanium_alloy_furnace",
                "starmap_terminal"
        ));
    }

    @Test
    void keepsPublishedBlockEntityIds() throws IOException {
        assertSourceIds("block/ModBlockEntities.java", "BLOCK_ENTITIES\\.register\\(\\s*\"([^\"]+)\"", Set.of(
                "ship_crate", "ship_door", "titanium_alloy_furnace", "fuel_controller"
        ));
    }

    @Test
    void keepsPublishedItemIds() throws IOException {
        assertSourceIds("item/ModItems.java", "ITEMS\\.(?:registerItem|registerSimpleItem|registerSimpleBlockItem)\\(\\s*\"([^\"]+)\"", Set.of(
                "matter_manipulator", "matter_manipulator_module", "matter_manipulator_workbench",
                "teleporter", "ship_console", "captain_chair", "fuel_controller", "ship_crate",
                "ship_door", "ship_engine", "tungsten_ore", "titanium_ore", "durasteel_ore",
                "star_core_ore", "titanium_alloy_furnace", "starmap_terminal", "raw_tungsten", "raw_titanium",
                "raw_durasteel", "raw_star_core", "tungsten_ingot", "titanium_ingot",
                "durasteel_ingot", "star_core_fragment", "emergency_food_can", "survival_knife"
        ));
        assertSourceIds("item/ModItems.java", "CREATIVE_MODE_TABS\\.register\\(\\s*\"([^\"]+)\"",
                Set.of("starboundmc"));
    }

    @Test
    void keepsPublishedEntityAndMenuIds() throws IOException {
        assertSourceIds("entity/ModEntities.java", "ENTITIES\\.register\\(\\s*\"([^\"]+)\"", Set.of("seat"));
        assertSourceIds("menu/ModMenus.java", "MENUS\\.register\\(\\s*\"([^\"]+)\"", Set.of(
                "upgrade_menu", "ship_console_menu", "ship_crate_menu", "teleporter_menu",
                "alloy_furnace_menu", "fuel_controller_menu", "starmap_terminal_menu"
        ));
    }

    @Test
    void keepsPublishedWorldgenIds() throws IOException {
        assertSourceIds("world/ModWorldgen.java", "public static final ResourceLocation [A-Z_]+ = id\\(\"([^\"]+)\"\\)",
                Set.of("ship", "molten", "barren", "filtered"));
    }

    private static void assertSourceIds(String relativePath, String expression, Set<String> expected)
            throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
        Set<String> actual = Pattern.compile(expression).matcher(source).results()
                .map(match -> match.group(1))
                .collect(Collectors.toSet());
        assertEquals(expected, actual);
    }
}
