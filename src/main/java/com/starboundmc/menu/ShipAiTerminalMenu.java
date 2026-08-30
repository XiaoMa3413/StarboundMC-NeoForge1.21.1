package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

/** Slotless server boundary for the shipboard AI terminal prototype. */
public final class ShipAiTerminalMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;

    public ShipAiTerminalMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public ShipAiTerminalMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.SHIP_AI_TERMINAL_MENU.get(), containerId);
        this.access = access;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SHIP_AI_TERMINAL.get());
    }
}
