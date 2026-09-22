package com.starboundmc.recipe;

import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Output tags let data packs classify their own printing recipes. */
public enum PrintingCategory {
    /**
     * Everything at once. This is a view filter rather than a tag: nothing carries it, so it is not
     * one of the categories a recipe can belong to — see {@link #concrete()}.
     */
    ALL,
    SURVIVAL, MATERIALS, MACHINES, BUILDING;

    private final TagKey<Item> tag = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("starboundmc", "printing/" + name().toLowerCase(Locale.ROOT)));

    public String translationKey() {
        return "gui.starboundmc.voxel_printing.category." + name().toLowerCase(Locale.ROOT);
    }

    public boolean matches(ItemStack output) {
        if (this == ALL) return true;
        if (this != MATERIALS) return output.is(tag);
        return output.is(tag) || !SURVIVAL.matches(output)
                && !MACHINES.matches(output) && !BUILDING.matches(output);
    }

    /**
     * The categories a recipe can actually belong to, in menu order. {@link #ALL} is excluded
     * because every output matches it, so including it would make "belongs to exactly one category"
     * meaningless.
     */
    public static java.util.List<PrintingCategory> concrete() {
        return java.util.List.of(SURVIVAL, MATERIALS, MACHINES, BUILDING);
    }
}
