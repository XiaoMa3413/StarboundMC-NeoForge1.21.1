// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.story;

import net.minecraft.nbt.CompoundTag;
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
    @Test void oldTaskLedgerKeepsClaimsDuringMigration() {
        var tag = new CompoundTag(); tag.putInt("schema", 1); tag.putInt("completed", 15); tag.putInt("claimed", 6);
        var old = NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        var migrated = old.observeEpp(true, false, false);
        assertTrue(migrated.claimed(NovaTask.SURFACE)); assertTrue(migrated.claimed(NovaTask.REPAIR));
        assertFalse(migrated.claimable(NovaTask.REPAIR)); assertTrue(migrated.claimable(NovaTask.LIFE_SUPPORT));
        assertEquals(NovaTaskProgress.SCHEMA, migrated.schemaVersion());
    }
}
