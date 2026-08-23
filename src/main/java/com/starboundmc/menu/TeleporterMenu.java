package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Container for the teleporter; it holds no item slots. The UI is driven by packets. */
public class TeleporterMenu extends AbstractContainerMenu
{
    private final ContainerLevelAccess access;
    public final BlockPos pos;

    public TeleporterMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, BlockPos.ZERO);
    }

    public TeleporterMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, BlockPos pos)
    {
        super(ModMenus.TELEPORTER_MENU.get(), containerId);
        this.access = access;
        this.pos = pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, ModBlocks.TELEPORTER.get());
    }
}
