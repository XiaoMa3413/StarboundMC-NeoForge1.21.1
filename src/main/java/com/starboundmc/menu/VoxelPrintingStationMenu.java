package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.VoxelPrintingStationBlockEntity;
import com.starboundmc.recipe.VoxelPrintingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;

/**
 * Server boundary for the voxel printing station: three material input
 * slots plus one output slot. Wallet-backed printing arrives with M2/M3.
 */
public final class VoxelPrintingStationMenu extends AbstractContainerMenu {
    private static final int MACHINE_START = 0;
    private static final int PLAYER_START = VoxelPrintingStationBlockEntity.TOTAL_SLOTS;
    private static final int PLAYER_INVENTORY_END = PLAYER_START + 27;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final VoxelPrintingStationBlockEntity station;
    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final boolean boundToBlock;

    public VoxelPrintingStationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL, BlockPos.ZERO, false);
    }

    /** Client-side factory constructor; the server writes the machine position when opening the menu. */
    public VoxelPrintingStationMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, null, ContainerLevelAccess.NULL,
                data == null ? BlockPos.ZERO : data.readBlockPos(), data != null);
    }

    public VoxelPrintingStationMenu(int containerId, Inventory inventory,
                                    VoxelPrintingStationBlockEntity station,
                                    ContainerLevelAccess access) {
        this(containerId, inventory, station, access,
                station != null ? station.getBlockPos() : BlockPos.ZERO, true);
    }

    private VoxelPrintingStationMenu(int containerId, Inventory inventory,
                                     VoxelPrintingStationBlockEntity station,
                                     ContainerLevelAccess access, BlockPos blockPos, boolean boundToBlock) {
        super(ModMenus.VOXEL_PRINTING_STATION_MENU.get(), containerId);
        this.station = station;
        this.access = access;
        this.blockPos = blockPos.immutable();
        this.boundToBlock = boundToBlock;

        // The client-side menu is created without the server block entity.
        // Keep the same machine-slot count/order there so container content
        // packets cannot index past the client's slot list.
        net.minecraft.world.Container slotContainer = station != null
                ? station : new SimpleContainer(VoxelPrintingStationBlockEntity.TOTAL_SLOTS);
        for (int i = 0; i < VoxelPrintingStationBlockEntity.MATERIAL_SLOTS; i++) {
            addSlot(new Slot(slotContainer, i, 134 + i * 18, 73) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isPrintingMaterial(stack, inventory.player.level());
                }
            });
        }
        addSlot(new Slot(slotContainer, VoxelPrintingStationBlockEntity.OUTPUT_SLOT, 217, 73) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerInventory(inventory, 42, 157);
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
        } else if (isPrintingMaterial(stack, player.level())) {
            if (!moveItemStackTo(stack, MACHINE_START,
                    VoxelPrintingStationBlockEntity.MATERIAL_SLOTS, false)) {
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

    private static boolean isPrintingMaterial(ItemStack stack, Level level) {
        if (stack.isEmpty()) {
            return false;
        }
        return level.getRecipeManager().getAllRecipesFor(VoxelPrintingRecipe.TYPE).stream()
                .flatMap(holder -> holder.value().materials().stream())
                .anyMatch(material -> material.ingredient().test(stack));
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
