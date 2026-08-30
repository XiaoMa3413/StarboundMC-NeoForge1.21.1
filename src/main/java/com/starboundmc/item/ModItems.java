package com.starboundmc.item;

import com.starboundmc.StarboundMC;
import com.starboundmc.block.ModBlocks;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.food.FoodConstants;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(StarboundMC.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, StarboundMC.MODID);

    public static final DeferredItem<MatterManipulatorItem> MATTER_MANIPULATOR = ITEMS.registerItem(
            "matter_manipulator", MatterManipulatorItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<MatterManipulatorModuleItem> MATTER_MANIPULATOR_MODULE =
            ITEMS.registerItem("matter_manipulator_module", MatterManipulatorModuleItem::new);

    public static final DeferredItem<BlockItem> MATTER_MANIPULATOR_WORKBENCH_ITEM =
            ITEMS.registerSimpleBlockItem("matter_manipulator_workbench", ModBlocks.MATTER_MANIPULATOR_WORKBENCH);
    public static final DeferredItem<BlockItem> TELEPORTER_ITEM =
            ITEMS.registerSimpleBlockItem("teleporter", ModBlocks.TELEPORTER);
    public static final DeferredItem<BlockItem> SHIP_CONSOLE_ITEM =
            ITEMS.registerSimpleBlockItem("ship_console", ModBlocks.SHIP_CONSOLE);
    public static final DeferredItem<BlockItem> STARMAP_TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("starmap_terminal", ModBlocks.STARMAP_TERMINAL);
    public static final DeferredItem<BlockItem> SHIP_AI_TERMINAL_ITEM =
            ITEMS.registerSimpleBlockItem("ship_ai_terminal", ModBlocks.SHIP_AI_TERMINAL);
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

    // Steak hunger with double steak saturation (8 * 1.6 * 2 = 25.6) and always edible;
    // eats in 1.2s, three quarters of the vanilla 1.6s.
    public static final DeferredItem<Item> EMERGENCY_FOOD_CAN = ITEMS.registerSimpleItem(
            "emergency_food_can",
            new Item.Properties().food(new FoodProperties(
                    8, FoodConstants.saturationByModifier(8, 1.6F), true, 1.2F, Optional.empty(), List.of())));

    // Fixed 5 attack damage shown in the tooltip (4 modifier + 1 player base) and a spam-friendly
    // 3 swings per second (-1.0 vs the sword's -2.4), fast enough for click-spam combat without
    // eating the vanilla attack cooldown damage penalty. No durability component, so the knife
    // never takes damage; enchantment value stays 0, so it cannot be enchanted. The attack speed
    // modifier works in combat but is kept out of the tooltip by SurvivalKnifeItem.
    public static final DeferredItem<SurvivalKnifeItem> SURVIVAL_KNIFE = ITEMS.registerItem(
            "survival_knife", SurvivalKnifeItem::new,
            new Item.Properties().attributes(survivalKnifeAttributes().withTooltip(false)));

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
                        output.accept(STARMAP_TERMINAL_ITEM);
                        output.accept(SHIP_AI_TERMINAL_ITEM);
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
                        output.accept(EMERGENCY_FOOD_CAN);
                        output.accept(SURVIVAL_KNIFE);
                    })
                    .build());

    private static ItemAttributeModifiers survivalKnifeAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "survival_knife_damage"),
                        4.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "survival_knife_speed"),
                        -1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
