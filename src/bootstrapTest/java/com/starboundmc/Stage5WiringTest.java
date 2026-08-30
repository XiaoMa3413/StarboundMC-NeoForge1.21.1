package com.starboundmc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage5WiringTest {
    @Test
    void registersRealPersistentBlockEntities() throws IOException {
        String registry = source("block/ModBlockEntities.java");
        assertTrue(registry.contains("BlockEntityType<ShipCrateBlockEntity>"));
        assertTrue(registry.contains("BlockEntityType<ShipDoorBlockEntity>"));
        assertTrue(registry.contains("BlockEntityType<AlloyFurnaceBlockEntity>"));
        assertTrue(registry.contains("BlockEntityType<FuelControllerBlockEntity>"));
        assertFalse(registry.contains("Stage2ShipCrateBlockEntity"));
        assertFalse(registry.contains("Stage2ShipDoorBlockEntity"));
    }

    @Test
    void usesRegistryAwareBlockEntitySerialization() throws IOException {
        for (String file : new String[] {
                "block/entity/ShipCrateBlockEntity.java",
                "block/entity/FuelControllerBlockEntity.java",
                "block/entity/AlloyFurnaceBlockEntity.java"
        }) {
            String source = source(file);
            assertTrue(source.contains("HolderLookup.Provider registries"), file);
            assertTrue(source.contains("saveAdditional(CompoundTag tag, HolderLookup.Provider"), file);
            assertTrue(source.contains("loadAdditional(CompoundTag tag, HolderLookup.Provider"), file);
            assertFalse(source.contains("net.minecraftforge"), file);
        }
    }

    @Test
    void usesModernSavedDataFactoriesAndAtomicFlightSnapshot() throws IOException {
        for (String file : new String[] {
                "warp/ShipStateData.java",
                "world/TeleporterManager.java",
                "world/ShipTemplatePlacer.java"
        }) {
            String source = source(file);
            assertTrue(source.contains("SavedData.Factory"), file);
            assertTrue(source.contains("HolderLookup.Provider registries"), file);
        }
        String shipState = source("warp/ShipStateData.java");
        assertTrue(shipState.contains("Atomically persists a sector-aware virtual-flight snapshot"));
        assertTrue(shipState.contains("this.shipVelocity ="));
        assertTrue(shipState.contains("this.shipYaw ="));
    }

    @Test
    void reconnectsContainerDirtyHooksAndServerTickers() throws IOException {
        String blocks = source("block/Stage2Blocks.java");
        assertTrue(blocks.contains("useWithoutItem("));
        assertTrue(blocks.contains("setOpen(level, pos, open)"));
        assertFalse(blocks.contains("ShipDoorBlockEntity::tick"));
        assertTrue(blocks.contains("AlloyFurnaceBlockEntity::tick"));

        String door = source("block/entity/ShipDoorBlockEntity.java");
        assertFalse(door.contains("getEntitiesOfClass"));
        assertFalse(door.contains("CHECK_INTERVAL"));

        String crate = source("block/entity/ShipCrateBlockEntity.java");
        String alloy = source("block/entity/AlloyFurnaceBlockEntity.java");
        String fuel = source("block/entity/FuelControllerBlockEntity.java");
        assertTrue(crate.contains("addListener(ignored -> setChanged())"));
        assertTrue(crate.contains("implements Container"));
        assertTrue(alloy.contains("addListener(ignored -> setChanged())"));
        assertTrue(fuel.contains("this.setChanged()"));
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(Path.of("src/main/java/com/starboundmc").resolve(relativePath));
    }
}
