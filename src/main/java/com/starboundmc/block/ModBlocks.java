package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    // Stage 1 keeps every published ID and representative property. Stage 2
    // restores the dedicated gameplay block implementations.
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StarboundMC.MODID);

    public static final DeferredBlock<Block> MATTER_MANIPULATOR_WORKBENCH = registerCopy(
            "matter_manipulator_workbench", Blocks.SMITHING_TABLE);
    public static final DeferredBlock<Block> TELEPORTER = registerCopy("teleporter", Blocks.IRON_BLOCK);
    public static final DeferredBlock<Block> SHIP_CONSOLE = BLOCKS.registerSimpleBlock("ship_console",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 10));
    public static final DeferredBlock<Block> SHIP_ENGINE = BLOCKS.registerSimpleBlock("ship_engine",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 14));
    public static final DeferredBlock<Block> CAPTAIN_CHAIR = BLOCKS.registerSimpleBlock("captain_chair",
            BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> FUEL_CONTROLLER = registerCopy("fuel_controller", Blocks.IRON_BLOCK);
    public static final DeferredBlock<Block> SHIP_CRATE = registerCopy("ship_crate", Blocks.IRON_BLOCK);
    public static final DeferredBlock<Block> SHIP_DOOR = registerCopy("ship_door", Blocks.IRON_BLOCK);
    public static final DeferredBlock<Block> TUNGSTEN_ORE = BLOCKS.registerSimpleBlock("tungsten_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.5F, 3.5F));
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.registerSimpleBlock("titanium_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).requiresCorrectToolForDrops().strength(4.5F, 4.5F));
    public static final DeferredBlock<Block> DURASTEEL_ORE = BLOCKS.registerSimpleBlock("durasteel_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(6.0F, 6.0F));
    public static final DeferredBlock<Block> STAR_CORE_ORE = BLOCKS.registerSimpleBlock("star_core_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(7.0F, 7.0F));
    public static final DeferredBlock<Block> TITANIUM_ALLOY_FURNACE = BLOCKS.registerSimpleBlock(
            "titanium_alloy_furnace",
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
