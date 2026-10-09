package com.starboundmc.client.shipai;

import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.NovaTask;
import com.starboundmc.story.NovaTaskProgress;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NovaTaskGuidanceTest
{
    @Test
    void repairGuidanceTracksTheCurrentAction()
    {
        NovaTaskProgress repairAvailable = progress(NovaTask.SURFACE.mask(), 0, 0);

        assertStage("repair.wood", repairAvailable, EngineState.DAMAGED,
                MineralScanState.LOCKED, false, false);
        assertStage("repair.scanning", repairAvailable, EngineState.DAMAGED,
                MineralScanState.SCANNING, false, false);
        assertStage("repair.upgrade", repairAvailable, EngineState.DAMAGED,
                MineralScanState.COMPLETE, false, false);
        assertStage("repair.print", repairAvailable, EngineState.DAMAGED,
                MineralScanState.COMPLETE, true, false);
        assertStage("repair.install", repairAvailable, EngineState.DAMAGED,
                MineralScanState.COMPLETE, true, true);
        assertStage("repair.igniting", repairAvailable, EngineState.IGNITING,
                MineralScanState.COMPLETE, true, true);
        assertStage("complete", repairAvailable, EngineState.ONLINE,
                MineralScanState.COMPLETE, true, true);
    }

    @Test
    void claimLockedAndReturnStagesAreExplicit()
    {
        assertEquals("locked.repair", NovaTaskGuidance.stage(NovaTask.REPAIR,
                NovaTaskProgress.DEFAULT, EngineState.DAMAGED, MineralScanState.LOCKED, false, false));

        NovaTaskProgress surfaceClaim = progress(NovaTask.SURFACE.mask(), 0, 0);
        assertEquals("claim", NovaTaskGuidance.stage(NovaTask.SURFACE, surfaceClaim,
                EngineState.DAMAGED, MineralScanState.LOCKED, false, false));
        assertEquals("complete", NovaTaskGuidance.stage(NovaTask.SURFACE,
                surfaceClaim.claim(NovaTask.SURFACE), EngineState.DAMAGED,
                MineralScanState.LOCKED, false, false));

        int completed = NovaTask.REPAIR.mask() | NovaTask.LIFE_SUPPORT.mask();
        NovaTaskProgress moonVisited = progress(completed, 0, NovaTaskProgress.MOON_VISITED);
        assertEquals("lunar_sortie.return", NovaTaskGuidance.stage(NovaTask.LUNAR_SORTIE,
                moonVisited, EngineState.ONLINE, MineralScanState.COMPLETE, false, false));
    }

    @Test
    void everyDisplayedGuidanceStageHasEnglishAndChineseCopy() throws IOException
    {
        String english = Files.readString(Path.of("src/main/resources/assets/starboundmc/lang/en_us.json"));
        String chinese = Files.readString(Path.of("src/main/resources/assets/starboundmc/lang/zh_cn.json"));
        List<String> stages = List.of("contact", "surface", "repair.wood", "repair.scanning",
                "repair.upgrade", "repair.print", "repair.install", "repair.igniting", "exploration",
                "life_support", "lunar_sortie", "lunar_sortie.return", "claim", "complete");
        for (String stage : stages)
        {
            assertLocalized(english, chinese, stage, "next");
            assertLocalized(english, chinese, stage, "help");
        }
        for (String task : List.of("surface", "repair", "exploration", "life_support", "lunar_sortie"))
            assertLocalized(english, chinese, "locked." + task, "help");
        assertTrue(english.contains("\"gui.starboundmc.tasks.main\""));
        assertTrue(chinese.contains("\"gui.starboundmc.tasks.main\""));
        assertTrue(english.contains("\"gui.starboundmc.tasks.preparation\""));
        assertTrue(chinese.contains("\"gui.starboundmc.tasks.preparation\""));
    }

    private static NovaTaskProgress progress(int completed, int claimed, int evidence)
    {
        return new NovaTaskProgress(NovaTaskProgress.SCHEMA, 0L, completed, claimed,
                evidence, -1, "");
    }

    private static void assertStage(String expected, NovaTaskProgress progress, EngineState engine,
                                    MineralScanState scan, boolean upgraded, boolean core)
    {
        assertEquals(expected, NovaTaskGuidance.stage(NovaTask.REPAIR, progress,
                engine, scan, upgraded, core));
    }

    private static void assertLocalized(String english, String chinese, String stage, String kind)
    {
        String key = NovaTaskGuidance.translation(stage, kind);
        assertTrue(english.contains("\"" + key + "\""), key + " en_us");
        assertTrue(chinese.contains("\"" + key + "\""), key + " zh_cn");
    }
}
