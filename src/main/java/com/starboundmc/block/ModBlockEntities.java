package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    // These minimal types provide valid placed-block save shells. Stage 5
    // reconnects the preserved gameplay block entity implementations.
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StarboundMC.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage2ShipCrateBlockEntity>> SHIP_CRATE =
            BLOCK_ENTITIES.register("ship_crate", () -> BlockEntityType.Builder.of(
                    Stage2ShipCrateBlockEntity::new, ModBlocks.SHIP_CRATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage2ShipDoorBlockEntity>> SHIP_DOOR =
            BLOCK_ENTITIES.register("ship_door", () -> BlockEntityType.Builder.of(
                    Stage2ShipDoorBlockEntity::new, ModBlocks.SHIP_DOOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage2AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITIES.register("titanium_alloy_furnace", () -> BlockEntityType.Builder.of(
                    Stage2AlloyFurnaceBlockEntity::new, ModBlocks.TITANIUM_ALLOY_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Stage2FuelControllerBlockEntity>> FUEL_CONTROLLER =
            BLOCK_ENTITIES.register("fuel_controller", () -> BlockEntityType.Builder.of(
                    Stage2FuelControllerBlockEntity::new, ModBlocks.FUEL_CONTROLLER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    public static final class Stage2ShipCrateBlockEntity extends BlockEntity implements Container {
        private static final int SLOT_COUNT = 54;
        private final SimpleContainer container = new SimpleContainer(SLOT_COUNT);

        public Stage2ShipCrateBlockEntity(BlockPos position, BlockState state) {
            super(SHIP_CRATE.get(), position, state);
            container.addListener(ignored -> setChanged());
        }

        public SimpleContainer container() {
            return container;
        }

        @Override
        public int getContainerSize() {
            return container.getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return container.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return container.getItem(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return container.removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return container.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            container.setItem(slot, stack);
        }

        @Override
        public boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(this, player);
        }

        @Override
        public void clearContent() {
            container.clearContent();
        }

        @Override
        protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            ContainerHelper.loadAllItems(tag, container.getItems(), registries);
        }

        @Override
        protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            ContainerHelper.saveAllItems(tag, container.getItems(), registries);
        }
    }

    public static final class Stage2ShipDoorBlockEntity extends BlockEntity {
        public Stage2ShipDoorBlockEntity(BlockPos position, BlockState state) {
            super(SHIP_DOOR.get(), position, state);
        }
    }

    public static final class Stage2AlloyFurnaceBlockEntity extends BlockEntity {
        public Stage2AlloyFurnaceBlockEntity(BlockPos position, BlockState state) {
            super(ALLOY_FURNACE.get(), position, state);
        }
    }

    public static final class Stage2FuelControllerBlockEntity extends BlockEntity {
        public Stage2FuelControllerBlockEntity(BlockPos position, BlockState state) {
            super(FUEL_CONTROLLER.get(), position, state);
        }
    }
}
