package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Persistence guards for slot-backed voxel machines and M6 reserved work. */
final class VoxelMachinePersistenceTest {
    @Test
    void printingStationPersistsExactMaterialAndOutputSlots() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/starboundmc/block/entity/VoxelPrintingStationBlockEntity.java"));

        assertTrue(source.contains("stack.save(registries, new CompoundTag())"),
                "item stacks must encode into a mutable compound before adding the slot id");
        assertTrue(source.contains("stackTag.putByte(\"slot\", (byte) slot)"),
                "each saved stack must retain its material/output slot index");
        assertTrue(source.contains("stackTag.contains(\"slot\", Tag.TAG_BYTE)"),
                "loading must recognize indexed stack entries");
        assertTrue(source.contains("Byte.toUnsignedInt(stackTag.getByte(\"slot\")) : i"),
                "loading must preserve the sequential fallback for older development saves");
        assertTrue(source.contains("items[slot] = ItemStack.parse(registries, stackTag)"),
                "indexed entries must restore to their original slots");
    }

    @Test
    void printingStationPersistsActiveAndQueuedReservations() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/starboundmc/block/entity/VoxelPrintingStationBlockEntity.java"));

        assertTrue(source.contains("tag.put(\"pending_result\", pendingResult.save(registries))"));
        assertTrue(source.contains("tag.putInt(\"active_voxel_cost\", activeVoxelCost)"));
        assertTrue(source.contains("tag.put(\"active_materials\", saveStacks(activeMaterials, registries))"));
        assertTrue(source.contains("entryTag.put(\"crafts\", craftsTag)"));
        assertTrue(source.contains("tag.put(\"print_queue\", queueTag)"));
        assertTrue(source.contains("loadStacks(tag.getList(\"active_materials\", Tag.TAG_COMPOUND), registries)"));
        assertTrue(source.contains("entryTag.getList(\"crafts\", Tag.TAG_COMPOUND)"));
    }

    @Test
    void refineryMigratesOldInFlightOutputAndStopsWhenInputChanges() throws IOException {
        String source = Files.readString(Path.of(
                "src/main/java/com/starboundmc/block/entity/VoxelRefineryBlockEntity.java"));

        assertTrue(source.contains("stopAfterInputChange(wasRunning)"));
        assertTrue(source.contains("cancelActiveJob()"));
        assertTrue(source.contains("refinery.startNextJob(serverLevel);"));
        assertTrue(source.contains("boolean stopRefinement(ServerLevel level)"));
        assertTrue(source.contains("if (refineProgress > 0)"));
        assertFalse(source.contains("if (!wasRunning) {\n                startNextJob(serverLevel);"));
        assertFalse(source.contains("continuous_mode"));
        assertTrue(source.contains("!tag.contains(\"job_voxels\", net.minecraft.nbt.Tag.TAG_INT)"));
        assertTrue(source.contains("jobVoxels = savedVoxels;"));
        assertTrue(source.contains("pendingVoxels = 0;"));
    }
}
