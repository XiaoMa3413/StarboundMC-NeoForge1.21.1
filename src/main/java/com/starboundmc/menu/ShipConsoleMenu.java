package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Container for the ship console; it holds no item slots. */
public class ShipConsoleMenu extends AbstractContainerMenu
{
    private final ContainerLevelAccess access;

    public ShipConsoleMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public ShipConsoleMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access)
    {
        super(ModMenus.SHIP_CONSOLE_MENU.get(), containerId);
        this.access = access;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, ModBlocks.SHIP_CONSOLE.get());
    }
}
