package com.starboundmc.story;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NovaTaskProgressTest {
    @Test void defaultStartsWithNoAchievementsAndTracksContact() {
        var fresh = NovaTaskProgress.DEFAULT;
        assertEquals(NovaTaskProgress.SCHEMA, fresh.schemaVersion());
        assertEquals(0, fresh.revision());
        assertEquals(0, fresh.completedMask());
        assertEquals(0, fresh.claimedMask());
        assertEquals(0, fresh.evidenceMask());
        assertEquals(NovaTask.CONTACT.id(), fresh.trackedTask());
        assertEquals("", fresh.firstSurface());
    }
    @Test void everyPersistedFieldIsRequired() {
        assertTrue(NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, new CompoundTag()).error().isPresent());
        var saved = (CompoundTag) NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, NovaTaskProgress.DEFAULT).getOrThrow();
        for (String field : new String[]{"schema", "revision", "completed", "claimed", "evidence", "tracked", "first_surface"}) {
            var incomplete = saved.copy();
            incomplete.remove(field);
            assertTrue(NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, incomplete).error().isPresent(), field);
        }
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
    @Test void oldAndFutureSchemasAreRejected() {
        for (int schema : new int[]{NovaTaskProgress.SCHEMA - 1, NovaTaskProgress.SCHEMA + 1}) {
            var saved = (CompoundTag) NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, NovaTaskProgress.DEFAULT).getOrThrow();
            saved.putInt("schema", schema);
            assertThrows(IllegalArgumentException.class, () -> NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, saved));
        }
    }
    @Test void invalidPersistedValuesAreRejectedWithoutNormalization() {
        var saved = (CompoundTag) NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, NovaTaskProgress.DEFAULT).getOrThrow();
        saved.putLong("revision", -1);
        assertThrows(IllegalArgumentException.class, () -> NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, saved));
        assertEquals(-1, saved.getLong("revision"));
        for (int[] invalid : new int[][]{
                {-1, 0, 0, 0}, {64, 0, 0, 0}, {0, -1, 0, 0}, {0, 64, 0, 0},
                {0, NovaTask.SURFACE.mask(), 0, 0}, {0, 0, -1, 0}, {0, 0, 32, 0},
                {0, 0, 0, -2}, {0, 0, 0, NovaTask.values().length}}) {
            var malformed = (CompoundTag) NovaTaskProgress.CODEC.encodeStart(NbtOps.INSTANCE, NovaTaskProgress.DEFAULT).getOrThrow();
            malformed.putInt("completed", invalid[0]);
            malformed.putInt("claimed", invalid[1]);
            malformed.putInt("evidence", invalid[2]);
            malformed.putInt("tracked", invalid[3]);
            var original = malformed.copy();
            assertThrows(IllegalArgumentException.class, () -> NovaTaskProgress.CODEC.parse(NbtOps.INSTANCE, malformed));
            assertEquals(original, malformed);
        }
    }
    @Test void constructorRejectsMalformedState() {
        assertThrows(IllegalArgumentException.class, () -> new NovaTaskProgress(NovaTaskProgress.SCHEMA, -1, 0, 0, 0, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> new NovaTaskProgress(NovaTaskProgress.SCHEMA, 0, 0, 1, 0, 0, ""));
        assertThrows(NullPointerException.class, () -> new NovaTaskProgress(NovaTaskProgress.SCHEMA, 0, 0, 0, 0, 0, null));
    }
}
