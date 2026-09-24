package com.starboundmc.block;

import com.starboundmc.StarboundMC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    // Gameplay-heavy blocks are restored in vertical slices; stable IDs and
    // representative properties remain available throughout the migration.
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(StarboundMC.MODID);
    public static final DeferredBlock<com.starboundmc.epp.EppServiceStationBlock> EPP_SERVICE_STATION = BLOCKS.registerBlock(
            "epp_service_station", com.starboundmc.epp.EppServiceStationBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5F).noOcclusion());
    public static final DeferredBlock<com.starboundmc.epp.LifeSupportStationBlock> LIFE_SUPPORT_STATION = BLOCKS.registerBlock(
            "life_support_station", com.starboundmc.epp.LifeSupportStationBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.5F).noOcclusion());

    public static final DeferredBlock<Stage2Blocks.Workbench> MATTER_MANIPULATOR_WORKBENCH = BLOCKS.registerBlock(
            "matter_manipulator_workbench", Stage2Blocks.Workbench::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE));
    public static final DeferredBlock<Stage2Blocks.Teleporter> TELEPORTER = BLOCKS.registerBlock(
            "teleporter", Stage2Blocks.Teleporter::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .noOcclusion().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK));
    public static final DeferredBlock<Stage2Blocks.ShipConsole> SHIP_CONSOLE = BLOCKS.registerBlock("ship_console",
            Stage2Blocks.ShipConsole::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).lightLevel(state -> 10));
    public static final DeferredBlock<StarmapTerminalBlock> STARMAP_TERMINAL = BLOCKS.registerBlock(
            "starmap_terminal", StarmapTerminalBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion().lightLevel(state -> 8));
    public static final DeferredBlock<ShipAiTerminalBlock> SHIP_AI_TERMINAL = BLOCKS.registerBlock(
            "ship_ai_terminal", ShipAiTerminalBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 4));
    public static final DeferredBlock<ShipEngineBlock> SHIP_ENGINE = BLOCKS.registerBlock("ship_engine",
            ShipEngineBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<ShipEngineUnitBlock> SHIP_ENGINE_UNIT = BLOCKS.registerBlock("ship_engine_unit",
            ShipEngineUnitBlock::new, BlockBehaviour.Properties.of().strength(4.0F, 8.0F)
                    .sound(SoundType.METAL).noOcclusion().lightLevel(state -> 8));
    public static final DeferredBlock<CaptainChairBlock> CAPTAIN_CHAIR = BLOCKS.registerBlock("captain_chair",
            CaptainChairBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).noOcclusion());
    public static final DeferredBlock<Stage2Blocks.FuelController> FUEL_CONTROLLER = BLOCKS.registerBlock(
            "fuel_controller", Stage2Blocks.FuelController::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Stage2Blocks.ShipCrate> SHIP_CRATE = BLOCKS.registerBlock(
            "ship_crate", Stage2Blocks.ShipCrate::new, BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Stage2Blocks.ShipDoor> SHIP_DOOR = BLOCKS.registerBlock(
            "ship_door", Stage2Blocks.ShipDoor::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion());
    public static final DeferredBlock<Block> TUNGSTEN_ORE = BLOCKS.registerSimpleBlock("tungsten_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.5F, 3.5F));
    public static final DeferredBlock<Block> TITANIUM_ORE = BLOCKS.registerSimpleBlock("titanium_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_ORE).requiresCorrectToolForDrops().strength(4.5F, 4.5F));
    public static final DeferredBlock<Block> DURASTEEL_ORE = BLOCKS.registerSimpleBlock("durasteel_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(6.0F, 6.0F));
    public static final DeferredBlock<Block> STAR_CORE_ORE = BLOCKS.registerSimpleBlock("star_core_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN).requiresCorrectToolForDrops().strength(7.0F, 7.0F));
    public static final DeferredBlock<Block> FUEL_CRYSTAL_ORE = BLOCKS.registerSimpleBlock("fuel_crystal_ore",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
                    .lightLevel(state -> 3));
    public static final DeferredBlock<Stage2Blocks.AlloyFurnace> TITANIUM_ALLOY_FURNACE = BLOCKS.registerBlock(
            "titanium_alloy_furnace", Stage2Blocks.AlloyFurnace::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).requiresCorrectToolForDrops().strength(5.0F, 6.0F));
    public static final DeferredBlock<VoxelRefineryBlock> VOXEL_REFINERY = BLOCKS.registerBlock(
            "voxel_refinery", VoxelRefineryBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 4));
    public static final DeferredBlock<VoxelPrintingStationBlock> VOXEL_PRINTING_STATION = BLOCKS.registerBlock(
            "voxel_printing_station", VoxelPrintingStationBlock::new,
            BlockBehaviour.Properties.of()
                    .strength(3.5F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .lightLevel(state -> 6));

    // ---- Hull set: the mod's own industrial building language ----
    // Surface outposts are built from these instead of vanilla stone/brick, so
    // an abandoned mining claim reads as the same civilisation as the ship:
    // blue-grey plating, cyan instrument accents, amber hazard banding. They are
    // plain full cubes (no block entity) and mine with a pickaxe like the ores.
    public static final DeferredBlock<Block> HULL_PLATING = BLOCKS.registerSimpleBlock("hull_plating",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(4.0F, 8.0F));
    public static final DeferredBlock<Block> REINFORCED_HULL = BLOCKS.registerSimpleBlock("reinforced_hull",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(5.0F, 10.0F));
    public static final DeferredBlock<Block> HULL_WINDOW = BLOCKS.registerSimpleBlock("hull_window",
            BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                    .strength(1.5F, 3.0F)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false));
    public static final DeferredBlock<Block> INDUSTRIAL_LIGHT = BLOCKS.registerSimpleBlock("industrial_light",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(3.0F, 6.0F)
                    .lightLevel(state -> 15));
    public static final DeferredBlock<Block> HULL_HAZARD = BLOCKS.registerSimpleBlock("hull_hazard",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(4.0F, 8.0F));
    /**
     * Walkable grating. Its openings are genuinely see-through (the model
     * declares {@code minecraft:cutout}), so it cannot also occlude: a
     * see-through block that still culls its neighbours' faces would hide the
     * floor underneath and leave the holes showing void. The transparency flags
     * follow vanilla leaves and glass — not a spawn surface, not suffocating,
     * not view-blocking.
     */
    public static final DeferredBlock<Block> HULL_GRATE = BLOCKS.registerSimpleBlock("hull_grate",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(4.0F, 8.0F)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false));

    /**
     * Signal head for the surface beacons, replacing the salvaged ship engine
     * that used to sit on the landing mast. Deliberately not a light source:
     * the emissive band is painted rather than lit, so a mast on the horizon
     * reads as a marker without flooding the surrounding regolith. It is a
     * shaped model, hence {@code noOcclusion}.
     */
    public static final DeferredBlock<Block> BEACON_EMITTER = BLOCKS.registerSimpleBlock("beacon_emitter",
            BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                    .requiresCorrectToolForDrops().strength(3.0F, 6.0F)
                    .noOcclusion()
                    .isValidSpawn((state, level, pos, type) -> false));

    private ModBlocks() {
    }

    private static DeferredBlock<Block> registerCopy(String name, Block block) {
        return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.ofFullCopy(block));
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
