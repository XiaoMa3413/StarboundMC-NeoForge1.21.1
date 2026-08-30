package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;

/** Slotless server boundary for the standalone starmap terminal. */
public final class StarmapTerminalMenu extends AbstractContainerMenu implements WarpControlMenu {
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final boolean boundToBlock;

    public StarmapTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL, BlockPos.ZERO, false);
    }

    public StarmapTerminalMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        this(containerId, inventory, access, BlockPos.ZERO, false);
    }

    public StarmapTerminalMenu(int containerId, Inventory inventory,
                               ContainerLevelAccess access, BlockPos blockPos) {
        this(containerId, inventory, access, blockPos, true);
    }

    private StarmapTerminalMenu(int containerId, Inventory inventory,
                                ContainerLevelAccess access, BlockPos blockPos,
                                boolean boundToBlock) {
        super(ModMenus.STARMAP_TERMINAL_MENU.get(), containerId);
        this.access = access;
        this.blockPos = blockPos.immutable();
        this.boundToBlock = boundToBlock;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return boundToBlock && stillValid(access, player, ModBlocks.STARMAP_TERMINAL.get());
    }

    public boolean isBoundToBlock() {
        return boundToBlock;
    }

    public BlockPos blockPos() {
        return blockPos;
    }
}
