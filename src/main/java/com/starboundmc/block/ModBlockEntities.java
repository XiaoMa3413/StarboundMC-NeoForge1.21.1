package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.entity.AlloyFurnaceBlockEntity;
import com.starboundmc.block.entity.FuelControllerBlockEntity;
import com.starboundmc.block.entity.ShipCrateBlockEntity;
import com.starboundmc.block.entity.ShipDoorBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StarboundMC.MODID);

    public static final RegistryObject<BlockEntityType<ShipCrateBlockEntity>> SHIP_CRATE =
            BLOCK_ENTITIES.register("ship_crate",
                    () -> BlockEntityType.Builder.of(ShipCrateBlockEntity::new, ModBlocks.SHIP_CRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ShipDoorBlockEntity>> SHIP_DOOR =
            BLOCK_ENTITIES.register("ship_door",
                    () -> BlockEntityType.Builder.of(ShipDoorBlockEntity::new, ModBlocks.SHIP_DOOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<AlloyFurnaceBlockEntity>> ALLOY_FURNACE =
            BLOCK_ENTITIES.register("titanium_alloy_furnace",
                    () -> BlockEntityType.Builder.of(AlloyFurnaceBlockEntity::new, ModBlocks.TITANIUM_ALLOY_FURNACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<FuelControllerBlockEntity>> FUEL_CONTROLLER =
            BLOCK_ENTITIES.register("fuel_controller",
                    () -> BlockEntityType.Builder.of(FuelControllerBlockEntity::new, ModBlocks.FUEL_CONTROLLER.get()).build(null));

    public static void register(IEventBus modEventBus)
    {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
