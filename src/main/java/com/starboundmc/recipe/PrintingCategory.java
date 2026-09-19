package com.starboundmc.recipe;

import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Output tags let data packs classify their own printing recipes. */
public enum PrintingCategory {
    SURVIVAL, MATERIALS, MACHINES, BUILDING;

    private final TagKey<Item> tag = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("starboundmc", "printing/" + name().toLowerCase(Locale.ROOT)));

    public String translationKey() {
        return "gui.starboundmc.voxel_printing.category." + name().toLowerCase(Locale.ROOT);
    }

    public boolean matches(ItemStack output) {
        if (this != MATERIALS) return output.is(tag);
        return output.is(tag) || !SURVIVAL.matches(output)
                && !MACHINES.matches(output) && !BUILDING.matches(output);
    }
}
