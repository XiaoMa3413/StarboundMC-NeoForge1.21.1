package com.starboundmc.menu;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.item.MatterManipulatorItem;
import com.starboundmc.item.MatterManipulatorModuleItem;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class UpgradeMenu extends AbstractContainerMenu
{
    public static final int MM_SLOT = 0;
    public static final int TRACK_SPEED = 0;
    public static final int TRACK_RANGE = 1;
    public static final int TRACK_MINING = 2;
    public static final int TRACK_FORTUNE = 3;

    private final ContainerLevelAccess access;
    private final SimpleContainer manipulatorContainer = new SimpleContainer(1);

    /** Module cost by the level being reached: 1st level = 1, 2nd = 2, 3rd = 4. */
    public static int modulesRequiredForTargetLevel(int targetLevel)
    {
        return switch (targetLevel)
        {
            case 2 -> 2;
            case 3 -> 4;
            default -> 1;
        };
    }

    public UpgradeMenu(int containerId, Inventory playerInventory)
    {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public UpgradeMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access)
    {
        super(ModMenus.UPGRADE_MENU.get(), containerId);
        this.access = access;

        this.addSlot(new Slot(manipulatorContainer, 0, 80, 16)
        {
            @Override
            public boolean mayPlace(ItemStack stack)
            {
                return stack.getItem() instanceof MatterManipulatorItem;
            }
        });

        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 9; col++)
            {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 174 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++)
        {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 228));
        }
    }

    public SimpleContainer getManipulatorContainer()
    {
        return manipulatorContainer;
    }

    public void tryUpgrade(Player player, int track)
    {
        ItemStack stack = manipulatorContainer.getItem(0);
        if (!(stack.getItem() instanceof MatterManipulatorItem))
        {
            player.displayClientMessage(Component.translatable("message.starboundmc.upgrade.no_manipulator"), true);
            return;
        }

        // Self-heal: rebuild the fortune enchantment list to match the NBT
        // level — repairs older items carrying duplicated fortune entries
        // (ItemStack.enchant() used to append without replacing).
        MatterManipulatorItem.setFortuneLevel(stack, MatterManipulatorItem.getFortuneLevel(stack));
        this.manipulatorContainer.setChanged();
        this.broadcastChanges();

        int level;
        int maxLevel;
        switch (track)
        {
            case TRACK_SPEED ->
            {
                level = MatterManipulatorItem.getSpeedLevel(stack);
                maxLevel = MatterManipulatorItem.MAX_UPGRADES;
            }
            case TRACK_RANGE ->
            {
                level = MatterManipulatorItem.getRangeLevel(stack);
                maxLevel = MatterManipulatorItem.MAX_UPGRADES;
            }
            case TRACK_MINING ->
            {
                level = MatterManipulatorItem.getMiningLevel(stack);
                maxLevel = MatterManipulatorItem.MAX_MINING_LEVEL;
            }
            case TRACK_FORTUNE ->
            {
                level = MatterManipulatorItem.getFortuneLevel(stack);
                maxLevel = MatterManipulatorItem.MAX_FORTUNE_UPGRADES;
            }
            default -> { return; }
        }

        if (level >= maxLevel)
        {
            player.displayClientMessage(Component.translatable("message.starboundmc.upgrade.max"), true);
            return;
        }

        int nextLevel = level + 1;
        int required = modulesRequiredForTargetLevel(nextLevel);
        if (!consumeModules(player, required))
        {
            player.displayClientMessage(Component.translatable("message.starboundmc.upgrade.need_modules", required), true);
            return;
        }

        switch (track)
        {
            case TRACK_SPEED -> MatterManipulatorItem.setSpeedLevel(stack, nextLevel);
            case TRACK_RANGE -> MatterManipulatorItem.setRangeLevel(stack, nextLevel);
            case TRACK_MINING -> MatterManipulatorItem.setMiningUpgrades(stack, nextLevel);
            case TRACK_FORTUNE -> MatterManipulatorItem.setFortuneLevel(stack, nextLevel);
        }
        manipulatorContainer.setChanged();
        this.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable(switch (track)
        {
            case TRACK_SPEED -> "message.starboundmc.upgrade.success_speed";
            case TRACK_RANGE -> "message.starboundmc.upgrade.success_range";
            case TRACK_MINING -> "message.starboundmc.upgrade.success_mining";
            case TRACK_FORTUNE -> "message.starboundmc.upgrade.success_fortune";
            default -> "message.starboundmc.upgrade.success_speed";
        }, nextLevel), true);
    }

    private static boolean consumeModules(Player player, int amount)
    {
        int total = 0;
        for (ItemStack stack : player.getInventory().items)
        {
            if (stack.getItem() instanceof MatterManipulatorModuleItem)
                total += stack.getCount();
        }
        if (total < amount)
            return false;

        int toRemove = amount;
        for (int i = 0; i < player.getInventory().items.size() && toRemove > 0; i++)
        {
            ItemStack stack = player.getInventory().items.get(i);
            if (stack.getItem() instanceof MatterManipulatorModuleItem)
            {
                int remove = Math.min(stack.getCount(), toRemove);
                stack.shrink(remove);
                toRemove -= remove;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem())
        {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();
            if (index == MM_SLOT)
            {
                if (!this.moveItemStackTo(slotStack, 1, 37, true))
                    return ItemStack.EMPTY;
            }
            else if (slotStack.getItem() instanceof MatterManipulatorItem)
            {
                if (!this.moveItemStackTo(slotStack, MM_SLOT, MM_SLOT + 1, false))
                    return ItemStack.EMPTY;
            }
            else if (index < 28)
            {
                if (!this.moveItemStackTo(slotStack, 28, 37, false))
                    return ItemStack.EMPTY;
            }
            else if (index < 37)
            {
                if (!this.moveItemStackTo(slotStack, 1, 28, false))
                    return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty())
                slot.set(ItemStack.EMPTY);
            else
                slot.setChanged();

            if (slotStack.getCount() == itemStack.getCount())
                return ItemStack.EMPTY;

            slot.onTake(player, slotStack);
        }
        return itemStack;
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(this.access, player, ModBlocks.MATTER_MANIPULATOR_WORKBENCH.get());
    }

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (!player.level().isClientSide)
        {
            ItemStack stack = manipulatorContainer.getItem(0);
            if (!stack.isEmpty())
            {
                player.getInventory().placeItemBackInInventory(stack);
                manipulatorContainer.setItem(0, ItemStack.EMPTY);
            }
        }
    }
}
