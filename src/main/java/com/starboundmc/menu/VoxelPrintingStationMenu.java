package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Server boundary for the voxel printing station: three material input
 * slots plus one output slot. Wallet-backed printing arrives with M2/M3.
 */
public final class VoxelPrintingStationMenu extends AbstractContainerMenu {
    private final VoxelPrintingStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final boolean boundToBlock;

    public VoxelPrintingStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, BlockPos.ZERO, false);
    }

    public VoxelPrintingStationMenu(int containerId, Inventory inventory,
                                    VoxelPrintingStationBlockEntity station,
                                    ContainerLevelAccess access) {
        this(containerId, inventory, station, access, BlockPos.ZERO, true);
    }

    private VoxelPrintingStationMenu(int containerId, Inventory inventory,
                                     VoxelPrintingStationBlockEntity station,
                                     ContainerLevelAccess access, BlockPos blockPos, boolean boundToBlock) {
        super(ModMenus.VOXEL_PRINTING_STATION_MENU.get(), containerId);
        this.station = station;
        this.access = access;
        this.blockPos = blockPos.immutable();
        this.boundToBlock = boundToBlock;

        if (station != null) {
            for (int i = 0; i < VoxelPrintingStationBlockEntity.MATERIAL_SLOTS; i++) {
                addSlot(new Slot(station, i, 30 + i * 18, 36));
            }
            addSlot(new Slot(station, VoxelPrintingStationBlockEntity.MATERIAL_SLOTS, 124, 36) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addPlayerInventory(inventory, 8, 84);
    }

    private void addPlayerInventory(Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, x + column * 18, y + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, x + column * 18, y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (!boundToBlock || station == null) {
            return true;
        }
        return stillValid(access, player, ModBlocks.VOXEL_PRINTING_STATION.get());
    }

    public boolean isBoundToBlock() {
        return boundToBlock;
    }

    public BlockPos blockPos() {
        return blockPos;
    }
}
