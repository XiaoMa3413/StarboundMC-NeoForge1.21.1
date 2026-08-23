package com.starboundmc.item;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StarboundMC.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarboundMC.MODID);

    public static final RegistryObject<Item> MATTER_MANIPULATOR = ITEMS.register("matter_manipulator",
            () -> new MatterManipulatorItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MATTER_MANIPULATOR_MODULE = ITEMS.register("matter_manipulator_module",
            () -> new MatterManipulatorModuleItem(new Item.Properties()));

    public static final RegistryObject<Item> MATTER_MANIPULATOR_WORKBENCH_ITEM = ITEMS.register("matter_manipulator_workbench",
            () -> new BlockItem(ModBlocks.MATTER_MANIPULATOR_WORKBENCH.get(), new Item.Properties()));

    public static final RegistryObject<Item> TELEPORTER_ITEM = ITEMS.register("teleporter",
            () -> new BlockItem(ModBlocks.TELEPORTER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIP_CONSOLE_ITEM = ITEMS.register("ship_console",
            () -> new BlockItem(ModBlocks.SHIP_CONSOLE.get(), new Item.Properties()));

    public static final RegistryObject<Item> CAPTAIN_CHAIR_ITEM = ITEMS.register("captain_chair",
            () -> new BlockItem(ModBlocks.CAPTAIN_CHAIR.get(), new Item.Properties()));

    public static final RegistryObject<Item> FUEL_CONTROLLER_ITEM = ITEMS.register("fuel_controller",
            () -> new BlockItem(ModBlocks.FUEL_CONTROLLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIP_CRATE_ITEM = ITEMS.register("ship_crate",
            () -> new BlockItem(ModBlocks.SHIP_CRATE.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIP_DOOR_ITEM = ITEMS.register("ship_door",
            () -> new BlockItem(ModBlocks.SHIP_DOOR.get(), new Item.Properties()));

    public static final RegistryObject<Item> SHIP_ENGINE_ITEM = ITEMS.register("ship_engine",
            () -> new BlockItem(ModBlocks.SHIP_ENGINE.get(), new Item.Properties()));

    public static final RegistryObject<Item> TUNGSTEN_ORE_ITEM = ITEMS.register("tungsten_ore",
            () -> new BlockItem(ModBlocks.TUNGSTEN_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_ORE_ITEM = ITEMS.register("titanium_ore",
            () -> new BlockItem(ModBlocks.TITANIUM_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> DURASTEEL_ORE_ITEM = ITEMS.register("durasteel_ore",
            () -> new BlockItem(ModBlocks.DURASTEEL_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> STAR_CORE_ORE_ITEM = ITEMS.register("star_core_ore",
            () -> new BlockItem(ModBlocks.STAR_CORE_ORE.get(), new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_ALLOY_FURNACE_ITEM = ITEMS.register("titanium_alloy_furnace",
            () -> new BlockItem(ModBlocks.TITANIUM_ALLOY_FURNACE.get(), new Item.Properties()));

    public static final RegistryObject<Item> RAW_TUNGSTEN = ITEMS.register("raw_tungsten",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAW_TITANIUM = ITEMS.register("raw_titanium",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAW_DURASTEEL = ITEMS.register("raw_durasteel",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> RAW_STAR_CORE = ITEMS.register("raw_star_core",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TUNGSTEN_INGOT = ITEMS.register("tungsten_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> DURASTEEL_INGOT = ITEMS.register("durasteel_ingot",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> STAR_CORE_FRAGMENT = ITEMS.register("star_core_fragment",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> STARBOUNDMC_TAB = CREATIVE_MODE_TABS.register("starboundmc",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.starboundmc"))
                    .icon(() -> MATTER_MANIPULATOR.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(MATTER_MANIPULATOR.get());
                        output.accept(MATTER_MANIPULATOR_MODULE.get());
                        output.accept(MATTER_MANIPULATOR_WORKBENCH_ITEM.get());
                        output.accept(TELEPORTER_ITEM.get());
                        output.accept(SHIP_CONSOLE_ITEM.get());
                        output.accept(CAPTAIN_CHAIR_ITEM.get());
                        output.accept(FUEL_CONTROLLER_ITEM.get());
                        output.accept(SHIP_CRATE_ITEM.get());
                        output.accept(SHIP_DOOR_ITEM.get());
                        output.accept(SHIP_ENGINE_ITEM.get());
                        output.accept(TUNGSTEN_ORE_ITEM.get());
                        output.accept(TITANIUM_ORE_ITEM.get());
                        output.accept(DURASTEEL_ORE_ITEM.get());
                        output.accept(STAR_CORE_ORE_ITEM.get());
                        output.accept(TITANIUM_ALLOY_FURNACE_ITEM.get());
                        output.accept(RAW_TUNGSTEN.get());
                        output.accept(RAW_TITANIUM.get());
                        output.accept(RAW_DURASTEEL.get());
                        output.accept(RAW_STAR_CORE.get());
                        output.accept(TUNGSTEN_INGOT.get());
                        output.accept(TITANIUM_INGOT.get());
                        output.accept(DURASTEEL_INGOT.get());
                        output.accept(STAR_CORE_FRAGMENT.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
