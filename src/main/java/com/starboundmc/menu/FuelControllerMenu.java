package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.block.entity.FuelControllerBlockEntity;
import com.starboundmc.warp.ShipFuelService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Fuel controller menu: five persistent fuel slots (backed by the block
 * entity) plus the player inventory. The "refuel" button (via AddFuelPacket)
 * adds ALL fuel at once — every item that fits entirely is consumed, and the
 * surplus stays in the slots when the tank is full.
 */
public class FuelControllerMenu extends AbstractContainerMenu
{
    public static final int FUEL_SLOTS = 5;
    public static final int FUEL_START = 0;
    public static final int PLAYER_START = FUEL_SLOTS;

    private final Container fuelContainer;
    private final ContainerLevelAccess access;

    /** Client-side factory: empty container, contents arrive via container sync. */
    public FuelControllerMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, new SimpleContainer(FUEL_SLOTS), ContainerLevelAccess.NULL);
    }

    public FuelControllerMenu(int containerId, Inventory playerInventory, Container fuelContainer, ContainerLevelAccess access)
    {
        super(ModMenus.FUEL_CONTROLLER_MENU.get(), containerId);
        this.fuelContainer = fuelContainer;
        this.access = access;

        // Slot grid aligned with the workbench-style fuel console screen:
        // fuel row at y=56, player rows 136..168, hotbar 190.
        int x0 = 43; // center a 5-slot row in a 176px panel
        for (int col = 0; col < FUEL_SLOTS; col++)
        {
            this.addSlot(new Slot(this.fuelContainer, col, x0 + col * 18, 56)
            {
                @Override
                public boolean mayPlace(ItemStack stack)
                {
                    return ShipFuelService.fuelValue(stack.getItem()) > 0;
                }
            });
        }

        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 136 + row * 18));

        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 190));
    }

    /**
     * Adds ALL fuel items in one click: consumes every item that fits into
     * the tank (whole stacks included); when the tank is full or the
     * remaining capacity is smaller than an item's value, the surplus stays
     * in the slots.
     */
    public void addAllFuelItems(ServerPlayer player)
    {
        boolean changed = false;
        for (int i = 0; i < FUEL_SLOTS; i++)
        {
            while (!this.fuelContainer.getItem(i).isEmpty())
            {
                int value = ShipFuelService.fuelValue(this.fuelContainer.getItem(i).getItem());
                if (value <= 0)
                    break;
                if (player.getServer() == null
                        || ShipFuelService.acceptedAmount(ShipFuelService.getFuel(player.getServer()), value) < value)
                {
                    player.displayClientMessage(Component.translatable("message.starboundmc.fuel.full"), true);
                    this.syncAfterFuelChange(changed);
                    return;
                }
                // Consume via the container API so the block entity is marked
                // dirty (persisted) and the client slot updates correctly.
                ItemStack removed = this.fuelContainer.removeItem(i, 1);
                if (removed.isEmpty())
                    break;
                int added = ShipFuelService.addFuel(player.getServer(), value);
                if (added != value)
                    throw new IllegalStateException("Fuel capacity changed during a server-thread transaction");
                changed = true;
            }
            // Replace a shrunk-to-zero stack with a real EMPTY stack.
            if (this.fuelContainer.getItem(i).isEmpty())
            {
                this.fuelContainer.setItem(i, ItemStack.EMPTY);
                changed = true;
            }
        }
        this.syncAfterFuelChange(changed);
        if (player.getServer() != null)
            ShipFuelService.syncToAll(player.getServer());
        if (player.getServer() != null
                && ShipFuelService.getFuel(player.getServer()) >= ShipFuelService.MAX_FUEL)
        {
            player.displayClientMessage(Component.translatable("message.starboundmc.fuel.full"), true);
        }
    }

    /** Mark the fuel container dirty and push the new slot contents to the client. */
    private void syncAfterFuelChange(boolean changed)
    {
        if (changed)
        {
            this.fuelContainer.setChanged();
            this.broadcastChanges();
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return this.fuelContainer instanceof FuelControllerBlockEntity fuel
                ? fuel.stillValid(player)
                : stillValid(this.access, player, ModBlocks.FUEL_CONTROLLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < PLAYER_START)
            {
                if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_START + 36, true))
                    return ItemStack.EMPTY;
            }
            else if (ShipFuelService.fuelValue(stack.getItem()) > 0)
            {
                if (!this.moveItemStackTo(stack, FUEL_START, PLAYER_START, false))
                    return ItemStack.EMPTY;
            }
            else if (index < PLAYER_START + 27)
            {
                if (!this.moveItemStackTo(stack, PLAYER_START + 27, PLAYER_START + 36, false))
                    return ItemStack.EMPTY;
            }
            else if (index < PLAYER_START + 36)
            {
                if (!this.moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false))
                    return ItemStack.EMPTY;
            }

            if (stack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (stack.getCount() == result.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, stack);
        }
        return result;
    }
}
