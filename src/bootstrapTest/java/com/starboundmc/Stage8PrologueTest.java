package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static wiring checks for the first playable prologue cue chain. */
final class Stage8PrologueTest
{

    @Test
    void novaBroadcastsHaveBothLanguageEntries()
            throws IOException
    {
        String english = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lang/en_us.json"));
        String chinese = Files.readString(Path.of(
                "src/main/resources/assets/starboundmc/lang/zh_cn.json"));
        for (String key : new String[]{
                "message.starboundmc.nova.prologue.emergency",
                "message.starboundmc.nova.prologue.locate_terminal",
                "message.starboundmc.nova.prologue.core_online",
                "message.starboundmc.nova.prologue.first_landing_complete",
                "message.starboundmc.nova.tutorial.matter_manipulator",
                "message.starboundmc.nova.tutorial.wood_acquired",
                "message.starboundmc.nova.prologue.mineral_scan_started",
                "message.starboundmc.nova.prologue.mineral_scan_result",
                "message.starboundmc.nova.prologue.mineral_scan_sublight_hint"})
        {
            assertTrue(english.contains("\"" + key + "\""), key + " en_us");
            assertTrue(chinese.contains("\"" + key + "\""), key + " zh_cn");
        }
        assertFalse(english.contains("message.starboundmc.nova.tutorial.matter_manipulator\": \"[N.O.V.A.]"));
        assertFalse(chinese.contains("message.starboundmc.nova.tutorial.matter_manipulator\": \"[N.O.V.A.]"));
        assertFalse(english.contains("left-click does not perform normal mining"));
        assertFalse(chinese.contains("左键不会执行普通挖掘"));
        assertFalse(english.contains("Left-click mining is disabled"));
        assertFalse(chinese.contains("左键无法挖掘"));
        assertTrue(chinese.contains("通信链路现已恢复"));
        assertTrue(chinese.contains("生存概率提高了 17 个百分点"));
        assertTrue(chinese.contains("地层矿物扫描中................"));
        assertTrue(english.contains("Mineral survey in progress................"));
        assertTrue(chinese.contains("\"gui.starboundmc.ship_ai.prologue.boot.restarting\": \""
                + "核心系统重启中…\""));
        assertTrue(english.contains("\"gui.starboundmc.ship_ai.prologue.boot.restarting\": \""
                + "Restarting core systems…\""));
        assertTrue(chinese.contains("少量钻石"));
        assertTrue(chinese.contains("幸运的是，我们目前位于一颗宜居星球的轨道上"));
        assertTrue(chinese.contains("检测到你已经苏醒"));
        assertTrue(chinese.contains("我将协助你恢复飞船系统"));
        assertTrue(english.contains("I have detected that you are awake"));
        assertTrue(english.contains("I will assist you in restoring the ship's systems"));
        assertFalse(chinese.contains("确认你仍然存活"));
        assertFalse(chinese.contains("恭喜你获得了木材"));
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
