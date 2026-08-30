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
    private final boolean boundToBlock;

    public TeleporterMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, ContainerLevelAccess.NULL, BlockPos.ZERO, false);
    }

    public TeleporterMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, BlockPos pos)
    {
        this(containerId, playerInventory, access, pos, true);
    }

    private TeleporterMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access,
                           BlockPos pos, boolean boundToBlock)
    {
        super(ModMenus.TELEPORTER_MENU.get(), containerId);
        this.access = access;
        this.pos = pos.immutable();
        this.boundToBlock = boundToBlock;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return boundToBlock && stillValid(this.access, player, ModBlocks.TELEPORTER.get());
    }

    public boolean isBoundToBlock()
    {
        return boundToBlock;
    }
}
