package com.starboundmc.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/** Shared slot view over a machine's container for recipe matching. */
public final class MachineRecipeInput implements RecipeInput {
    private final ItemStack[] slots;

    public MachineRecipeInput(ItemStack... slots) {
        this.slots = slots;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < slots.length ? slots[index] : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return slots.length;
    }
}
