package com.starboundmc;

import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Translation and recipe resources supporting the first playable prologue. */
final class PrologueResourceContractTest
{

    @Test
    void novaMessagesHaveBothLanguageEntries()
            throws IOException
    {
        String[] keys = {
                "message.starboundmc.nova.prologue.emergency",
                "message.starboundmc.nova.prologue.locate_terminal",
                "message.starboundmc.nova.prologue.core_online",
                "message.starboundmc.nova.prologue.first_landing_complete",
                "message.starboundmc.nova.tutorial.matter_manipulator",
                "message.starboundmc.nova.tutorial.wood_acquired",
                "message.starboundmc.nova.prologue.mineral_scan_started",
                "message.starboundmc.nova.prologue.mineral_scan_result",
                "message.starboundmc.nova.prologue.mineral_scan_sublight_hint",
                "gui.starboundmc.ship_ai.prologue.boot.restarting",
                "gui.starboundmc.ship_ai.prologue.first_contact.greeting",
                "gui.starboundmc.ship_ai.prologue.topic.nova_identity.response",
                "gui.starboundmc.ship_ai.prologue.topic.incident.response",
                "gui.starboundmc.ship_ai.prologue.topic.current_location.response",
                "gui.starboundmc.ship_ai.prologue.topic.flight_capability.response"};
        for (String language : new String[]{"en_us", "zh_cn"})
        {
            var entries = JsonParser.parseString(Files.readString(Path.of(
                    "src/main/resources/assets/starboundmc/lang/" + language + ".json"))).getAsJsonObject();
            for (String key : keys)
            {
                String context = key + " " + language;
                assertTrue(entries.has(key), context);
                String text = entries.get(key).getAsString();
                assertFalse(text.isBlank(), context);
                assertFalse(text.startsWith("[N.O.V.A.]"), context);
            }
        }
    }

    @Test
    void earlyPrintedManipulatorModuleSupportsTheDiamondObjective()
            throws IOException
    {
        String recipe = Files.readString(Path.of(
                "src/main/resources/data/starboundmc/recipe/print_matter_manipulator_module.json"));

        assertTrue(recipe.contains("starboundmc:voxel_printing"));
        assertTrue(recipe.contains("minecraft:iron_ingot"));
        assertTrue(recipe.contains("minecraft:lapis_lazuli"));
        assertTrue(recipe.contains("starboundmc:matter_manipulator_module"));
        assertTrue(recipe.contains("\"count\": 2"));
        assertFalse(recipe.contains("starboundmc:titanium_ingot"));
        assertFalse(recipe.contains("starboundmc:durasteel_ingot"));
        assertFalse(recipe.contains("starboundmc:star_core_fragment"));
    }

}
