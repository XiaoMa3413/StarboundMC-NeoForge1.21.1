// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.item.ModItems;
import com.starboundmc.menu.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class LifeSupportMenu extends AbstractContainerMenu {
    private final Container socket;
    private final DataSlot progress = DataSlot.standalone(), supplied = DataSlot.standalone();
    public LifeSupportMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(1)); }
    public LifeSupportMenu(int id, Inventory inventory, Container socket) {
        super(ModMenus.LIFE_SUPPORT_MENU.get(), id); this.socket = socket;
        addSlot(new Slot(socket, 0, 30, 49) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.EMPTY_OXYGEN_CANISTER.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 44 + col * 18, 128 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 44 + col * 18, 186));
        addDataSlot(progress); addDataSlot(supplied);
    }
    public int progress() { return progress.get(); }
    public boolean supplied() { return supplied.get() != 0; }
    public boolean refillsEpp(Player player) { return stillValid(player) && socket instanceof LifeSupportStationEntity station && station.supplied(); }
    @Override public void broadcastChanges() {
        if (socket instanceof LifeSupportStationEntity station) { progress.set(station.progress()); supplied.set(station.supplied() ? 1 : 0); }
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) { return !player.isSpectator() && socket instanceof LifeSupportStationEntity && socket.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        boolean moved = index == 0 ? moveItemStackTo(stack, 1, 37, true)
                : stack.is(ModItems.EMPTY_OXYGEN_CANISTER.get()) ? moveItemStackTo(stack, 0, 1, false)
                : index < 28 ? moveItemStackTo(stack, 28, 37, false) : moveItemStackTo(stack, 1, 28, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
