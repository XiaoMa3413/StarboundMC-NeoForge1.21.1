package com.starboundmc.armor;

import java.util.*;
import com.starboundmc.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class ModArmor {
    public static final List<ArmorItem.Type> TYPES = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE,
            ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "starboundmc");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("starboundmc");
    private static final Map<ArmorProfile, Map<ArmorItem.Type, DeferredItem<CombatArmorItem>>> ARMOR = new EnumMap<>(ArmorProfile.class);
    static {
        for (var profile : ArmorProfile.values()) {
            var material = MATERIALS.register(profile.id(), () -> new ArmorMaterial(
                    Map.of(ArmorItem.Type.HELMET, profile.helmet, ArmorItem.Type.CHESTPLATE, profile.chestplate,
                            ArmorItem.Type.LEGGINGS, profile.leggings, ArmorItem.Type.BOOTS, profile.boots),
                    profile.enchantability, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(repairItem(profile)),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("starboundmc", profile.id()))),
                    profile.toughness, 0));
            Map<ArmorItem.Type, DeferredItem<CombatArmorItem>> pieces = new EnumMap<>(ArmorItem.Type.class);
            for (var type : TYPES) pieces.put(type, ITEMS.registerItem(profile.id() + "_" + type.getName(),
                    properties -> new CombatArmorItem(material, type, properties, profile)));
            ARMOR.put(profile, pieces);
        }
    }
    public static Item repairItem(ArmorProfile profile) {
        return switch (profile) {
            case PIONEER -> ModItems.TUNGSTEN_INGOT.get();
            case VANGUARD -> ModItems.DURASTEEL_INGOT.get();
            case RANGER -> ModItems.TITANIUM_INGOT.get();
            case STARWARDEN -> ModItems.STAR_CORE_FRAGMENT.get();
        };
    }
    public static CombatArmorItem item(ArmorProfile profile, ArmorItem.Type type) { return ARMOR.get(profile).get(type).get(); }
    public static void creative(CreativeModeTab.Output output) {
        for (var profile : ArmorProfile.values()) for (var type : TYPES) output.accept(item(profile, type));
    }
    public static void register(IEventBus bus) { MATERIALS.register(bus); ITEMS.register(bus); }
}
