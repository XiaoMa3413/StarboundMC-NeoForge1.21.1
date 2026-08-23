package com.starboundmc.item;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StarboundMC.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarboundMC.MODID);

    public static final DeferredItem<Item> MATTER_MANIPULATOR = ITEMS.registerSimpleItem(
            "matter_manipulator", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MATTER_MANIPULATOR_MODULE =
            ITEMS.registerSimpleItem("matter_manipulator_module");

    public static final DeferredItem<BlockItem> MATTER_MANIPULATOR_WORKBENCH_ITEM =
            ITEMS.registerSimpleBlockItem("matter_manipulator_workbench", ModBlocks.MATTER_MANIPULATOR_WORKBENCH);
    public static final DeferredItem<BlockItem> TELEPORTER_ITEM =
            ITEMS.registerSimpleBlockItem("teleporter", ModBlocks.TELEPORTER);
    public static final DeferredItem<BlockItem> SHIP_CONSOLE_ITEM =
            ITEMS.registerSimpleBlockItem("ship_console", ModBlocks.SHIP_CONSOLE);
    public static final DeferredItem<BlockItem> CAPTAIN_CHAIR_ITEM =
            ITEMS.registerSimpleBlockItem("captain_chair", ModBlocks.CAPTAIN_CHAIR);
    public static final DeferredItem<BlockItem> FUEL_CONTROLLER_ITEM =
            ITEMS.registerSimpleBlockItem("fuel_controller", ModBlocks.FUEL_CONTROLLER);
    public static final DeferredItem<BlockItem> SHIP_CRATE_ITEM =
            ITEMS.registerSimpleBlockItem("ship_crate", ModBlocks.SHIP_CRATE);
    public static final DeferredItem<BlockItem> SHIP_DOOR_ITEM =
            ITEMS.registerSimpleBlockItem("ship_door", ModBlocks.SHIP_DOOR);
    public static final DeferredItem<BlockItem> SHIP_ENGINE_ITEM =
            ITEMS.registerSimpleBlockItem("ship_engine", ModBlocks.SHIP_ENGINE);
    public static final DeferredItem<BlockItem> TUNGSTEN_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("tungsten_ore", ModBlocks.TUNGSTEN_ORE);
    public static final DeferredItem<BlockItem> TITANIUM_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("titanium_ore", ModBlocks.TITANIUM_ORE);
    public static final DeferredItem<BlockItem> DURASTEEL_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("durasteel_ore", ModBlocks.DURASTEEL_ORE);
    public static final DeferredItem<BlockItem> STAR_CORE_ORE_ITEM =
            ITEMS.registerSimpleBlockItem("star_core_ore", ModBlocks.STAR_CORE_ORE);
    public static final DeferredItem<BlockItem> TITANIUM_ALLOY_FURNACE_ITEM =
            ITEMS.registerSimpleBlockItem("titanium_alloy_furnace", ModBlocks.TITANIUM_ALLOY_FURNACE);

    public static final DeferredItem<Item> RAW_TUNGSTEN = ITEMS.registerSimpleItem("raw_tungsten");
    public static final DeferredItem<Item> RAW_TITANIUM = ITEMS.registerSimpleItem("raw_titanium");
    public static final DeferredItem<Item> RAW_DURASTEEL = ITEMS.registerSimpleItem("raw_durasteel");
    public static final DeferredItem<Item> RAW_STAR_CORE = ITEMS.registerSimpleItem("raw_star_core");
    public static final DeferredItem<Item> TUNGSTEN_INGOT = ITEMS.registerSimpleItem("tungsten_ingot");
    public static final DeferredItem<Item> TITANIUM_INGOT = ITEMS.registerSimpleItem("titanium_ingot");
    public static final DeferredItem<Item> DURASTEEL_INGOT = ITEMS.registerSimpleItem("durasteel_ingot");
    public static final DeferredItem<Item> STAR_CORE_FRAGMENT = ITEMS.registerSimpleItem("star_core_fragment");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STARBOUNDMC_TAB =
            CREATIVE_MODE_TABS.register("starboundmc", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.starboundmc"))
                    .icon(MATTER_MANIPULATOR::toStack)
                    .displayItems((parameters, output) -> {
                        output.accept(MATTER_MANIPULATOR);
                        output.accept(MATTER_MANIPULATOR_MODULE);
                        output.accept(MATTER_MANIPULATOR_WORKBENCH_ITEM);
                        output.accept(TELEPORTER_ITEM);
                        output.accept(SHIP_CONSOLE_ITEM);
                        output.accept(CAPTAIN_CHAIR_ITEM);
                        output.accept(FUEL_CONTROLLER_ITEM);
                        output.accept(SHIP_CRATE_ITEM);
                        output.accept(SHIP_DOOR_ITEM);
                        output.accept(SHIP_ENGINE_ITEM);
                        output.accept(TUNGSTEN_ORE_ITEM);
                        output.accept(TITANIUM_ORE_ITEM);
                        output.accept(DURASTEEL_ORE_ITEM);
                        output.accept(STAR_CORE_ORE_ITEM);
                        output.accept(TITANIUM_ALLOY_FURNACE_ITEM);
                        output.accept(RAW_TUNGSTEN);
                        output.accept(RAW_TITANIUM);
                        output.accept(RAW_DURASTEEL);
                        output.accept(RAW_STAR_CORE);
                        output.accept(TUNGSTEN_INGOT);
                        output.accept(TITANIUM_INGOT);
                        output.accept(DURASTEEL_INGOT);
                        output.accept(STAR_CORE_FRAGMENT);
                    })
                    .build());

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
