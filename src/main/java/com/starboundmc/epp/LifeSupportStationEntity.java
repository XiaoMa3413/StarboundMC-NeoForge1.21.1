// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.block.ModBlockEntities;
import com.starboundmc.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Persistent one-canister filling socket. No player menu is required for processing. */
public final class LifeSupportStationEntity extends BlockEntity implements Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int progress;
    private boolean supplied;
    public LifeSupportStationEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.LIFE_SUPPORT_STATION.get(), pos, state); }
    public int progress() { return progress; }
    public boolean supplied() { return supplied; }
    public static void tick(Level level, BlockPos pos, BlockState state, LifeSupportStationEntity station) {
        if (!(level instanceof ServerLevel server)) return;
        station.supplied = PlayerEnvironmentService.at(server, pos).breathable() && level.getFluidState(pos).isEmpty();
        if (!station.supplied || !station.getItem(0).is(ModItems.EMPTY_OXYGEN_CANISTER.get())) {
            if (station.progress != 0) { station.progress = 0; station.setChanged(); }
            return;
        }
        if (++station.progress >= EppConfig.FILL_TICKS) {
            station.items.set(0, new ItemStack(ModItems.OXYGEN_CANISTER.get()));
            station.progress = 0;
        }
        station.setChanged();
    }
    @Override public int getContainerSize() { return 1; }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean isEmpty() { return items.getFirst().isEmpty(); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.is(ModItems.EMPTY_OXYGEN_CANISTER.get()); }
    @Override public ItemStack removeItem(int slot, int count) { var removed = ContainerHelper.removeItem(items, slot, count); if (!removed.isEmpty()) { progress = 0; setChanged(); } return removed; }
    @Override public ItemStack removeItemNoUpdate(int slot) { var removed = ContainerHelper.takeItem(items, slot); progress = 0; setChanged(); return removed; }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(1); progress = 0; setChanged(); }
    @Override public void clearContent() { items.clear(); progress = 0; setChanged(); }
    @Override public boolean stillValid(Player player) { return level != null && player.level() == level && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getCenter()) <= 64; }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); ContainerHelper.saveAllItems(tag, items, registries); tag.putInt("FillProgress", progress);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        progress = Math.clamp(tag.getInt("FillProgress"), 0, EppConfig.FILL_TICKS - 1);
    }
}
