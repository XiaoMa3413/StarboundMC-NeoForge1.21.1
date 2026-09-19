package com.starboundmc.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NovaTaskProgressTest {
    @Test void oldSaveStartsWithUnclaimedTasksAndMigratesFromExistingEvidence() {
        var old = NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).getOrThrow();
        assertEquals(NovaTaskProgress.DEFAULT, old);
        var migrated = old.observe(true, true, false, false, true);
        assertTrue(migrated.claimable(NovaTask.SURFACE));
        assertTrue(migrated.claimable(NovaTask.REPAIR));
        assertFalse(migrated.completed(NovaTask.EXPLORATION));
    }
    @Test void repairIsSharedButArrivalAndRewardsRemainPersonal() {
        var alice = NovaTaskProgress.DEFAULT.observe(true, true, true, true, true);
        var bob = NovaTaskProgress.DEFAULT.observe(true, false, false, false, true);
        assertFalse(bob.completed(NovaTask.REPAIR));
        alice = alice.claim(NovaTask.REPAIR);
        bob = bob.observe(true, true, false, false, true);
        assertTrue(bob.claimable(NovaTask.REPAIR));
        assertTrue(alice.claimed(NovaTask.REPAIR));
        assertSame(alice, alice.claim(NovaTask.REPAIR));
    }
    @Test void explorationRequiresAnotherSurfaceAfterRepair() {
        var p = NovaTaskProgress.DEFAULT.arrive("minecraft:overworld", false)
                .arrive("starboundmc:frozen", false).observe(true, true, false, false, true);
        assertFalse(p.completed(NovaTask.EXPLORATION));
        p = p.arrive("minecraft:overworld", true).observe(true, true, false, false, true);
        assertFalse(p.completed(NovaTask.EXPLORATION));
        p = p.arrive("starboundmc:frozen", true).observe(true, true, false, false, true);
        assertTrue(p.claimable(NovaTask.EXPLORATION));
        assertEquals(NovaTask.LIFE_SUPPORT.id(), p.trackedTask());
    }
    @Test void achievementsAndEvidenceSurviveInventoryLossAndCodecRoundTrip() {
        var p = NovaTaskProgress.DEFAULT.observe(true, true, true, true, true).claim(NovaTask.SURFACE);
        assertSame(p, p.observe(false, false, false, false, false));
        var saved = NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, p).getOrThrow();
        var restored = NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        assertEquals(p, restored);
        assertFalse(restored.claimable(NovaTask.SURFACE));
        assertTrue(restored.claimable(NovaTask.REPAIR));
    }
    @Test void lockedTasksCannotBeTrackedAndUntrackingPersists() {
        assertSame(NovaTaskProgress.DEFAULT, NovaTaskProgress.DEFAULT.track(3));
        var p = NovaTaskProgress.DEFAULT.track(-1).observe(true, true, false, false, false);
        assertEquals(-1, p.trackedTask());
        assertThrows(IllegalArgumentException.class, () -> p.track(9));
        assertSame(p, p.claim(NovaTask.REPAIR));
    }
    @Test void futureSchemaIsPreservedAndReadOnly() {
        var future = new NovaTaskProgress(NovaTaskProgress.SCHEMA + 1, 42, 63, 63, 255, 5, "future:surface");
        var restored = NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE,
                NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, future).getOrThrow()).getOrThrow();
        assertEquals(future, restored);
        assertSame(restored, restored.observe(true, true, true, true, true));
        assertSame(restored, restored.arrive("other", true));
        assertSame(restored, restored.track(0));
        assertSame(restored, restored.claim(NovaTask.SURFACE));
    }
}
