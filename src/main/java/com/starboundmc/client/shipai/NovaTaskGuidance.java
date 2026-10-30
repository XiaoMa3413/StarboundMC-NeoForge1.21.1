package com.starboundmc.client.shipai;

import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.NovaTask;
import com.starboundmc.story.NovaTaskProgress;

/** Selects actionable copy without mutating task progress or inferring inventory from past evidence. */
final class NovaTaskGuidance {
    private NovaTaskGuidance() { }

    static String stage(NovaTask task, NovaTaskProgress progress, EngineState engine,
                        MineralScanState scan, boolean upgraded, boolean core) {
        if (progress.completed(task)) return progress.claimable(task) ? "claim" : "complete";
        if (!task.available(progress.completedMask())) return "locked." + task.key();
        if (task == NovaTask.REPAIR) {
            if (engine == EngineState.IGNITING) return "repair.igniting";
            if (engine == EngineState.ONLINE) return "complete";
            if (scan == MineralScanState.LOCKED) return "repair.wood";
            if (scan != MineralScanState.COMPLETE) return "repair.scanning";
            if (!upgraded) return "repair.upgrade";
            return core ? "repair.install" : "repair.print";
        }
        if (task == NovaTask.LUNAR_SORTIE && (progress.evidenceMask() & NovaTaskProgress.MOON_VISITED) != 0)
            return "lunar_sortie.return";
        return task.key();
    }

    static String translation(String stage, String kind) {
        return "gui.starboundmc.guidance." + stage + "." + kind;
    }
}
