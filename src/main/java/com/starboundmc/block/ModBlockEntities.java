package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.entity.AlloyFurnaceBlockEntity;
import com.starboundmc.block.entity.FuelControllerBlockEntity;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import com.starboundmc.block.entity.ShipDoorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, StarboundMC.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipCrateBlockEntity>> SHIP_CRATE =
            BLOCK_ENTITIES.register("ship_crate", () -> BlockEntityType.Builder.of(
                    ShipCrateBlockEntity::new, ModBlocks.SHIP_CRATE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipDoorBlockEntity>> SHIP_DOOR =
            BLOCK_ENTITIES.register("ship_door", () -> BlockEntityType.Builder.of(
                    ShipDoorBlockEntity::new, ModBlocks.SHIP_DOOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITIES.register("titanium_alloy_furnace", () -> BlockEntityType.Builder.of(
                    AlloyFurnaceBlockEntity::new, ModBlocks.TITANIUM_ALLOY_FURNACE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FuelControllerBlockEntity>> FUEL_CONTROLLER =
            BLOCK_ENTITIES.register("fuel_controller", () -> BlockEntityType.Builder.of(
                    FuelControllerBlockEntity::new, ModBlocks.FUEL_CONTROLLER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
