package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    // Gameplay-heavy blocks are restored in vertical slices; stable IDs and
    // representative properties remain available throughout the migration.
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StarboundMC.MODID);

    public static final DeferredBlock<Stage2Blocks.Workbench> MATTER_MANIPULATOR_WORKBENCH = BLOCKS.registerBlock(
            "matter_manipulator_workbench", Stage2Blocks.Workbench::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE));
    public static final DeferredBlock<Stage2Blocks.Teleporter> TELEPORTER = BLOCKS.registerBlock(
            "teleporter", Stage2Blocks.Teleporter::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Stage2Blocks.ShipConsole> SHIP_CONSOLE = BLOCKS.registerBlock("ship_console",
            Stage2Blocks.ShipConsole::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 10));
    public static final DeferredBlock<StarmapTerminalBlock> STARMAP_TERMINAL = BLOCKS.registerBlock(
            "starmap_terminal", StarmapTerminalBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 8));
    public static final DeferredBlock<ShipEngineBlock> SHIP_ENGINE = BLOCKS.registerBlock("ship_engine",
            ShipEngineBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 14));
    public static final DeferredBlock<CaptainChairBlock> CAPTAIN_CHAIR = BLOCKS.registerBlock("captain_chair",
            CaptainChairBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).noOcclusion());
    public static final DeferredBlock<Stage2Blocks.FuelController> FUEL_CONTROLLER = BLOCKS.registerBlock(
            "fuel_controller", Stage2Blocks.FuelController::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Stage2Blocks.ShipCrate> SHIP_CRATE = BLOCKS.registerBlock(
            "ship_crate", Stage2Blocks.ShipCrate::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Stage2Blocks.ShipDoor> SHIP_DOOR = BLOCKS.registerBlock(
            "ship_door", Stage2Blocks.ShipDoor::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
    public static final DeferredBlock<Block> TUNGSTEN_ORE = BLOCKS.registerSimpleBlock("tungsten_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.5F, 3.5F));
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.registerSimpleBlock("titanium_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).requiresCorrectToolForDrops().strength(4.5F, 4.5F));
    public static final DeferredBlock<Block> DURASTEEL_ORE = BLOCKS.registerSimpleBlock("durasteel_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(6.0F, 6.0F));
    public static final DeferredBlock<Block> STAR_CORE_ORE = BLOCKS.registerSimpleBlock("star_core_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(7.0F, 7.0F));
    public static final DeferredBlock<Stage2Blocks.AlloyFurnace> TITANIUM_ALLOY_FURNACE = BLOCKS.registerBlock(
            "titanium_alloy_furnace", Stage2Blocks.AlloyFurnace::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F));

    private ModBlocks() {
    }

    private static DeferredBlock<Block> registerCopy(String name, Block block) {
        return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.ofFullCopy(block));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
