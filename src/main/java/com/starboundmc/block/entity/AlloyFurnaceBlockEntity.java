package com.starboundmc.block.entity;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AlloyFurnaceBlockEntity extends BlockEntity
{
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private static final int DEFAULT_COOK_TIME = 200;
    private static final int STAR_CORE_COOK_TIME = 320;

    private final SimpleContainer inventory = new SimpleContainer(3);
    private final ContainerData data = new ContainerData()
    {
        @Override
        public int getCount()
        {
            return 4;
        }

        @Override
        public int get(int index)
        {
            return switch (index)
            {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value)
        {
            switch (index)
            {
                case 0 -> litTime = value;
                case 1 -> litDuration = value;
                case 2 -> cookingProgress = value;
                case 3 -> cookingTotalTime = value;
            }
        }
    };

    private int litTime;
    private int litDuration;
    private int cookingProgress;
    private int cookingTotalTime;

    public AlloyFurnaceBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.ALLOY_FURNACE.get(), pos, state);
        inventory.addListener(ignored -> setChanged());
    }

    public SimpleContainer getInventory()
    {
        return inventory;
    }

    public ContainerData getContainerData()
    {
        return data;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlloyFurnaceBlockEntity be)
    {
        if (level.isClientSide)
            return;

        boolean dirty = false;

        if (be.litTime > 0)
        {
            be.litTime--;
            dirty = true;
        }

        ItemStack fuel = be.inventory.getItem(FUEL_SLOT);
        boolean canSmelt = be.canSmelt();
        if (be.litTime == 0 && canSmelt)
        {
            int burnTime = fuel.getBurnTime(RecipeType.SMELTING);
            if (burnTime > 0)
            {
                be.litTime = burnTime;
                be.litDuration = burnTime;
                if (fuel.hasCraftingRemainingItem())
                {
                    be.inventory.setItem(FUEL_SLOT, fuel.getCraftingRemainingItem().copy());
                }
                else
                {
                    fuel.shrink(1);
                }
                dirty = true;
            }
        }

        if (be.litTime > 0 && canSmelt)
        {
            be.cookingTotalTime = be.getCookTime(be.inventory.getItem(INPUT_SLOT));
            be.cookingProgress++;
            if (be.cookingProgress >= be.cookingTotalTime)
            {
                be.cookingProgress = 0;
                be.smelt();
                dirty = true;
            }
        }
        else
        {
            be.cookingProgress = 0;
        }

        if (dirty)
            be.setChanged();
    }

    private boolean canSmelt()
    {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input.isEmpty())
            return false;

        ItemStack result = getSmeltingResult(input);
        if (result.isEmpty())
            return false;

        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty())
            return true;
        if (!output.is(result.getItem()))
            return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private ItemStack getSmeltingResult(ItemStack input)
    {
        if (input.is(ModItems.RAW_DURASTEEL.get()))
            return new ItemStack(ModItems.DURASTEEL_INGOT.get());
        if (input.is(ModItems.RAW_STAR_CORE.get()))
            return new ItemStack(ModItems.STAR_CORE_FRAGMENT.get());
        return ItemStack.EMPTY;
    }

    private int getCookTime(ItemStack input)
    {
        if (input.is(ModItems.RAW_STAR_CORE.get()))
            return STAR_CORE_COOK_TIME;
        return DEFAULT_COOK_TIME;
    }

    private void smelt()
    {
        ItemStack input = inventory.getItem(INPUT_SLOT);
        ItemStack result = getSmeltingResult(input);
        if (result.isEmpty())
            return;

        input.shrink(1);
        ItemStack output = inventory.getItem(OUTPUT_SLOT);
        if (output.isEmpty())
        {
            inventory.setItem(OUTPUT_SLOT, result);
        }
        else
        {
            output.grow(result.getCount());
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.saveAdditional(tag, registries);
        tag.put("Items", inventory.createTag(registries));
        tag.putInt("LitTime", litTime);
        tag.putInt("LitDuration", litDuration);
        tag.putInt("CookingProgress", cookingProgress);
        tag.putInt("CookingTotalTime", cookingTotalTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
    {
        super.loadAdditional(tag, registries);
        inventory.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registries);
        litTime = nonNegative(tag.getInt("LitTime"));
        litDuration = nonNegative(tag.getInt("LitDuration"));
        cookingProgress = nonNegative(tag.getInt("CookingProgress"));
        cookingTotalTime = nonNegative(tag.getInt("CookingTotalTime"));
    }

    private static int nonNegative(int value)
    {
        return Math.max(0, value);
    }
}
