package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 54-slot storage crate menu. */
public class ShipCrateMenu extends AbstractContainerMenu
{
    public static final int SLOTS = 54;

    private final Container container;
    private final ContainerLevelAccess access;
    private final Runnable onChanged;

    public ShipCrateMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, new SimpleContainer(SLOTS), ContainerLevelAccess.NULL, () -> {});
    }

    public ShipCrateMenu(int containerId, Inventory playerInventory, Container container, Runnable onChanged)
    {
        this(containerId, playerInventory, container, ContainerLevelAccess.NULL, onChanged);
    }

    public ShipCrateMenu(int containerId, Inventory playerInventory, Container container, ContainerLevelAccess access, Runnable onChanged)
    {
        super(ModMenus.SHIP_CRATE_MENU.get(), containerId);
        this.container = container;
        this.access = access;
        this.onChanged = onChanged;
        container.startOpen(playerInventory.player);

        // Slot grid aligned with the workbench-style crate screen: cargo rows
        // 34..124, player rows 164..200, hotbar 218 (see ShipCrateScreen).
        for (int row = 0; row < 6; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 34 + row * 18));

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 164 + row * 18));

        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 218));
    }

    @Override
    public void slotsChanged(Container container)
    {
        super.slotsChanged(container);
        this.onChanged.run();
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        this.container.stopOpen(player);
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, ModBlocks.SHIP_CRATE.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < SLOTS)
            {
                if (!this.moveItemStackTo(stack, SLOTS, SLOTS + 36, true))
                    return ItemStack.EMPTY;
            }
            else if (!this.moveItemStackTo(stack, 0, SLOTS, false))
            {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (stack.getCount() == result.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }
        return result;
    }
}
