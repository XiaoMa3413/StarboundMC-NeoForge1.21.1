package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.VoxelRefineryBlockEntity;
import com.starboundmc.recipe.MachineRecipeInput;
import com.starboundmc.recipe.VoxelDecompositionRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;

import java.util.Optional;

/** Server boundary for the voxel refinery: one material input slot. */
public final class VoxelRefineryMenu extends AbstractContainerMenu {
    private static final int MACHINE_START = 0;
    private static final int PLAYER_START = VoxelRefineryBlockEntity.INPUT_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_START + 27;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final VoxelRefineryBlockEntity refinery;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final boolean boundToBlock;

    public VoxelRefineryMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, BlockPos.ZERO, false);
    }

    /** Client-side factory constructor; the server writes the machine position when opening the menu. */
    public VoxelRefineryMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL,
                data == null ? BlockPos.ZERO : data.readBlockPos(), data != null);
    }

    public VoxelRefineryMenu(int containerId, Inventory inventory, VoxelRefineryBlockEntity refinery,
                             ContainerLevelAccess access) {
        this(containerId, inventory, refinery, access,
                refinery != null ? refinery.getBlockPos() : BlockPos.ZERO, true);
    }

    private VoxelRefineryMenu(int containerId, Inventory inventory, VoxelRefineryBlockEntity refinery,
                              ContainerLevelAccess access, BlockPos blockPos, boolean boundToBlock) {
        super(ModMenus.VOXEL_REFINERY_MENU.get(), containerId);
        this.refinery = refinery;
        this.access = access;
        this.blockPos = blockPos.immutable();
        this.boundToBlock = boundToBlock;

        // The client-side menu is created without the server block entity.
        // Keep the same machine-slot count/order there so container content
        // packets cannot index past the client's slot list.
        net.minecraft.world.Container slotContainer = refinery != null
                ? refinery : new SimpleContainer(VoxelRefineryBlockEntity.INPUT_SLOTS);
        addSlot(new Slot(slotContainer, 0, 44, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return matchingRecipe(stack, inventory.player.level()).isPresent();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                if (refinery != null) {
                    refinery.stopAfterManualInputTake();
                }
            }

        });
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
        if (index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (matchingRecipe(stack, player.level()).isPresent()) {
            if (!moveItemStackTo(stack, MACHINE_START, PLAYER_START, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INVENTORY_END) {
            if (!moveItemStackTo(stack, PLAYER_INVENTORY_END, PLAYER_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_START, PLAYER_INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    public Optional<RecipeHolder<VoxelDecompositionRecipe>> matchingRecipe(Level level) {
        return matchingRecipe(getSlot(MACHINE_START).getItem(), level);
    }

    private static Optional<RecipeHolder<VoxelDecompositionRecipe>> matchingRecipe(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                VoxelDecompositionRecipe.TYPE, new MachineRecipeInput(stack), level);
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
