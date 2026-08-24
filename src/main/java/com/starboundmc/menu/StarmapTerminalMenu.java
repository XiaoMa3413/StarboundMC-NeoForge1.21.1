package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Slotless server boundary for the standalone starmap terminal. */
public final class StarmapTerminalMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    public StarmapTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public StarmapTerminalMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.STARMAP_TERMINAL_MENU.get(), containerId);
        this.access = access;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.STARMAP_TERMINAL.get());
    }
}
