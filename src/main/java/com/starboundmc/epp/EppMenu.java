// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.menu.ModMenus;
import com.starboundmc.story.ModAttachments;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Player-owned socket; vanilla container synchronization is the sole item interaction path. */
public final class EppMenu extends AbstractContainerMenu {
    public static final int WIDTH = 248, HEIGHT = 214;
    private final Player owner;
    public EppMenu(int id, Inventory inventory) {
        super(ModMenus.EPP_MENU.get(), id);
        owner = inventory.player;
        var socket = new SimpleContainer(1);
        if (!owner.level().isClientSide) {
            socket.setItem(0, owner.getData(ModAttachments.EPP_EQUIPMENT));
            socket.addListener(container -> owner.setData(ModAttachments.EPP_EQUIPMENT, container.getItem(0)));
        }
        addSlot(new Slot(socket, 0, 30, 49) {
            @Override public boolean mayPlace(ItemStack stack) { return EppEquipmentResolver.valid(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        inventorySlots(inventory);
    }
    private void inventorySlots(Inventory inventory) {
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 44 + col * 18, 128 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 44 + col * 18, 186));
    }
    @Override public boolean stillValid(Player player) { return player == owner && player.isAlive() && !player.isSpectator(); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved = index == 0 ? moveItemStackTo(stack, 1, 37, true)
                : EppEquipmentResolver.valid(stack) ? moveItemStackTo(stack, 0, 1, false)
                : index < 28 ? moveItemStackTo(stack, 28, 37, false) : moveItemStackTo(stack, 1, 28, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
}
