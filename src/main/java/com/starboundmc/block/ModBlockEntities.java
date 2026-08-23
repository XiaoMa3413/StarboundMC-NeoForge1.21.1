package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    // These minimal types reserve the published registry entries. Stage 5
    // reconnects the preserved block entity implementations and saved state.
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StarboundMC.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage1ShipCrateBlockEntity>> SHIP_CRATE =
            BLOCK_ENTITIES.register("ship_crate", () -> BlockEntityType.Builder.of(
                    Stage1ShipCrateBlockEntity::new, ModBlocks.SHIP_CRATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage1ShipDoorBlockEntity>> SHIP_DOOR =
            BLOCK_ENTITIES.register("ship_door", () -> BlockEntityType.Builder.of(
                    Stage1ShipDoorBlockEntity::new, ModBlocks.SHIP_DOOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage1AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITIES.register("titanium_alloy_furnace", () -> BlockEntityType.Builder.of(
                    Stage1AlloyFurnaceBlockEntity::new, ModBlocks.TITANIUM_ALLOY_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage1FuelControllerBlockEntity>> FUEL_CONTROLLER =
            BLOCK_ENTITIES.register("fuel_controller", () -> BlockEntityType.Builder.of(
                    Stage1FuelControllerBlockEntity::new, ModBlocks.FUEL_CONTROLLER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    public static final class Stage1ShipCrateBlockEntity extends BlockEntity {
        public Stage1ShipCrateBlockEntity(BlockPos position, BlockState state) {
            super(SHIP_CRATE.get(), position, state);
        }
    }

    public static final class Stage1ShipDoorBlockEntity extends BlockEntity {
        public Stage1ShipDoorBlockEntity(BlockPos position, BlockState state) {
            super(SHIP_DOOR.get(), position, state);
        }
    }

    public static final class Stage1AlloyFurnaceBlockEntity extends BlockEntity {
        public Stage1AlloyFurnaceBlockEntity(BlockPos position, BlockState state) {
            super(ALLOY_FURNACE.get(), position, state);
        }
    }

    public static final class Stage1FuelControllerBlockEntity extends BlockEntity {
        public Stage1FuelControllerBlockEntity(BlockPos position, BlockState state) {
            super(FUEL_CONTROLLER.get(), position, state);
        }
    }
}
