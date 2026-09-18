package com.starboundmc.recipe;

import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Output tags let data packs classify their own printing recipes. */
public enum PrintingCategory {
    ALL, EQUIPMENT, COMPONENTS, MACHINES, BUILDING, OTHER;

    private final TagKey<Item> tag = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("starboundmc", "printing/" + name().toLowerCase(Locale.ROOT)));

    public String translationKey() {
        return "gui.starboundmc.voxel_printing.category." + name().toLowerCase(Locale.ROOT);
    }

    public boolean matches(ItemStack output) {
        if (this == ALL) return true;
        if (this != OTHER) return output.is(tag);
        return !EQUIPMENT.matches(output) && !COMPONENTS.matches(output)
                && !MACHINES.matches(output) && !BUILDING.matches(output);
    }
}
