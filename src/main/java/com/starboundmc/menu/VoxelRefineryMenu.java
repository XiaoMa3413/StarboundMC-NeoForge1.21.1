package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.VoxelRefineryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server boundary for the voxel refinery: one material input slot. */
public final class VoxelRefineryMenu extends AbstractContainerMenu {
    private final VoxelRefineryBlockEntity refinery;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final boolean boundToBlock;

    public VoxelRefineryMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, BlockPos.ZERO, false);
    }

    public VoxelRefineryMenu(int containerId, Inventory inventory, VoxelRefineryBlockEntity refinery,
                             ContainerLevelAccess access) {
        this(containerId, inventory, refinery, access, BlockPos.ZERO, true);
    }

    private VoxelRefineryMenu(int containerId, Inventory inventory, VoxelRefineryBlockEntity refinery,
                              ContainerLevelAccess access, BlockPos blockPos, boolean boundToBlock) {
        super(ModMenus.VOXEL_REFINERY_MENU.get(), containerId);
        this.refinery = refinery;
        this.access = access;
        this.blockPos = blockPos.immutable();
        this.boundToBlock = boundToBlock;

        if (refinery != null) {
            addSlot(new Slot(refinery, 0, 44, 36) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return true;
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
        if (!boundToBlock || refinery == null) {
            return true;
        }
        return stillValid(access, player, ModBlocks.VOXEL_REFINERY.get());
    }

    public boolean isBoundToBlock() {
        return boundToBlock;
    }

    public BlockPos blockPos() {
        return blockPos;
    }
}
