package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.AlloyFurnaceBlockEntity;
import com.starboundmc.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;

public class AlloyFurnaceMenu extends AbstractContainerMenu
{
    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public AlloyFurnaceMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, new SimpleContainer(3), new SimpleContainerData(4));
    }

    public AlloyFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data)
    {
        this(containerId, playerInventory, container, data, ContainerLevelAccess.NULL);
    }

    public AlloyFurnaceMenu(int containerId, Inventory playerInventory, Container container, ContainerData data, ContainerLevelAccess access)
    {
        super(ModMenus.ALLOY_FURNACE_MENU.get(), containerId);
        this.container = container;
        this.data = data;
        this.access = access;

        this.addSlot(new Slot(container, AlloyFurnaceBlockEntity.INPUT_SLOT, 56, 17));
        this.addSlot(new Slot(container, AlloyFurnaceBlockEntity.FUEL_SLOT, 56, 53));
        this.addSlot(new Slot(container, AlloyFurnaceBlockEntity.OUTPUT_SLOT, 116, 35)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return false;
            }
        });

        this.addDataSlots(data);

        // Player grid aligned with the workbench-style furnace screen:
        // rows 96..132, hotbar 150 (see AlloyFurnaceScreen).
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 150));
        }
    }

    public int getLitTime()
    {
        return data.get(0);
    }

    public int getLitDuration()
    {
        return data.get(1);
    }

    public int getCookingProgress()
    {
        return data.get(2);
    }

    public int getCookingTotalTime()
    {
        return data.get(3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();
            if (index == AlloyFurnaceBlockEntity.OUTPUT_SLOT)
            {
                if (!this.moveItemStackTo(slotStack, 3, 39, true))
                    return ItemStack.EMPTY;
            }
            else if (index == AlloyFurnaceBlockEntity.INPUT_SLOT || index == AlloyFurnaceBlockEntity.FUEL_SLOT)
            {
                if (!this.moveItemStackTo(slotStack, 3, 39, false))
                    return ItemStack.EMPTY;
            }
            else
            {
                if (isAlloyInput(slotStack))
                {
                    if (!this.moveItemStackTo(slotStack, AlloyFurnaceBlockEntity.INPUT_SLOT, AlloyFurnaceBlockEntity.INPUT_SLOT + 1, false))
                        return ItemStack.EMPTY;
                }
                else if (ForgeHooks.getBurnTime(slotStack, RecipeType.SMELTING) > 0)
                {
                    if (!this.moveItemStackTo(slotStack, AlloyFurnaceBlockEntity.FUEL_SLOT, AlloyFurnaceBlockEntity.FUEL_SLOT + 1, false))
                        return ItemStack.EMPTY;
                }
                else if (index < 30)
                {
                    if (!this.moveItemStackTo(slotStack, 30, 39, false))
                        return ItemStack.EMPTY;
                }
                else if (index < 39)
                {
                    if (!this.moveItemStackTo(slotStack, 3, 30, false))
                        return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (slotStack.getCount() == itemStack.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, slotStack);
        }
        return itemStack;
    }

    private static boolean isAlloyInput(ItemStack stack)
    {
        return stack.is(ModItems.RAW_DURASTEEL.get()) || stack.is(ModItems.RAW_STAR_CORE.get());
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, ModBlocks.TITANIUM_ALLOY_FURNACE.get());
    }
}
