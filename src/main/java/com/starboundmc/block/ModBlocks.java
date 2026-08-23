package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, StarboundMC.MODID);

    public static final RegistryObject<Block> MATTER_MANIPULATOR_WORKBENCH = BLOCKS.register("matter_manipulator_workbench",
            () -> new MatterManipulatorWorkbenchBlock(BlockBehaviour.Properties.copy(Blocks.SMITHING_TABLE)));

    public static final RegistryObject<Block> TELEPORTER = BLOCKS.register("teleporter",
            () -> new TeleporterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> SHIP_CONSOLE = BLOCKS.register("ship_console",
            () -> new ShipConsoleBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).lightLevel(s -> 10)));

    public static final RegistryObject<Block> SHIP_ENGINE = BLOCKS.register("ship_engine",
            () -> new ShipEngineBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).lightLevel(s -> 14)));

    public static final RegistryObject<Block> CAPTAIN_CHAIR = BLOCKS.register("captain_chair",
            () -> new CaptainChairBlock(BlockBehaviour.Properties.copy(Blocks.QUARTZ_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> FUEL_CONTROLLER = BLOCKS.register("fuel_controller",
            () -> new FuelControllerBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> SHIP_CRATE = BLOCKS.register("ship_crate",
            () -> new ShipCrateBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> SHIP_DOOR = BLOCKS.register("ship_door",
            () -> new ShipDoorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));

    public static final RegistryObject<Block> TUNGSTEN_ORE = BLOCKS.register("tungsten_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.5F, 3.5F)));

    public static final RegistryObject<Block> TITANIUM_ORE = BLOCKS.register("titanium_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DIAMOND_ORE).requiresCorrectToolForDrops().strength(4.5F, 4.5F)));

    public static final RegistryObject<Block> DURASTEEL_ORE = BLOCKS.register("durasteel_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(6.0F, 6.0F)));

    public static final RegistryObject<Block> STAR_CORE_ORE = BLOCKS.register("star_core_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(7.0F, 7.0F)));

    public static final RegistryObject<Block> TITANIUM_ALLOY_FURNACE = BLOCKS.register("titanium_alloy_furnace",
            () -> new TitaniumAlloyFurnaceBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F)));

    public static void register(IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
    }
}
