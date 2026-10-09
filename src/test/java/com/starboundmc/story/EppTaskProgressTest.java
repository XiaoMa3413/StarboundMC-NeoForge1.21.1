// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.story;

import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EppTaskProgressTest {
    @Test void lushEquipmentTaskDoesNotRequireBarrenOrEngineRepair() {
        var progress = NovaTaskProgress.DEFAULT.observe(true, true, false, false, false).observeEpp(true, false, false);
        assertTrue(progress.claimable(NovaTask.LIFE_SUPPORT));
        assertFalse(progress.completed(NovaTask.REPAIR));
        assertFalse(progress.completed(NovaTask.LUNAR_SORTIE));
    }
    @Test void lunarTaskNeedsEquippedVisitAndSafeReturn() {
        var progress = NovaTaskProgress.DEFAULT.observe(true, true, true, true, true).observeEpp(true, false, true);
        assertFalse(progress.completed(NovaTask.LUNAR_SORTIE));
        progress = progress.observeEpp(false, true, false).observeEpp(true, false, true);
        assertFalse(progress.completed(NovaTask.LUNAR_SORTIE));
        progress = progress.observeEpp(true, true, false);
        assertFalse(progress.completed(NovaTask.LUNAR_SORTIE));
        progress = progress.observeEpp(true, false, true);
        assertTrue(progress.claimable(NovaTask.LUNAR_SORTIE));
        assertFalse(progress.claim(NovaTask.LUNAR_SORTIE).claimable(NovaTask.LUNAR_SORTIE));
    }
    @Test void currentLedgerKeepsClaimsWhenLifeSupportCompletes() {
        var progress = NovaTaskProgress.DEFAULT.observe(true, true, false, false, true)
                .claim(NovaTask.SURFACE).claim(NovaTask.REPAIR);
        var restored = NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE,
                NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, progress).getOrThrow()).getOrThrow();
        var equipped = restored.observeEpp(true, false, false);
        assertTrue(equipped.claimed(NovaTask.SURFACE)); assertTrue(equipped.claimed(NovaTask.REPAIR));
        assertFalse(equipped.claimable(NovaTask.REPAIR)); assertTrue(equipped.claimable(NovaTask.LIFE_SUPPORT));
    }
}
