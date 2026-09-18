// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.block.ModBlocks;
import com.starboundmc.item.ModDataComponents;
import com.starboundmc.item.ModItems;
import com.starboundmc.menu.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;
import java.util.List;

public final class EppServiceMenu extends AbstractContainerMenu {
    public static final int WIDTH = 284, HEIGHT = 236;
    public static final int UPGRADE = 0, INSTALL = 1, REMOVE = 2;
    private final SimpleContainer tray = new SimpleContainer(3);
    private final BlockPos pos;
    private final Player owner;
    private final net.minecraft.world.level.Level level;
    private final DataSlot safe = DataSlot.standalone();
    public EppServiceMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, buffer.readBlockPos());
    }
    public EppServiceMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.EPP_SERVICE_MENU.get(), id);
        this.pos = pos.immutable(); owner = inventory.player; level = owner.level();
        for (int i = 0; i < 3; i++) {
            final int input = i;
            addSlot(new Slot(tray, i, 44 + i * 90, 47) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return switch (input) {
                        case 0 -> EppEquipmentResolver.valid(stack);
                        case 1 -> stack.getItem() instanceof EppModuleItem;
                        default -> stack.is(ModItems.EPP_MK2_UPGRADE_KIT.get());
                    };
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, 9 + row * 9 + col, 62 + col * 18, 150 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 62 + col * 18, 208));
        addDataSlot(safe);
    }
    public BlockPos blockPos() { return pos; }
    public boolean safeEnvironment() { return safe.get() != 0; }
    private boolean canService() {
        if (!(owner instanceof ServerPlayer player) || !stillValid(player)) return false;
        var station = PlayerEnvironmentService.at(player.serverLevel(), pos);
        var environment = PlayerEnvironmentService.at(player);
        return safe(environment) && safe(station) && !player.isUnderWater()
                && player.level().getFluidState(pos).isEmpty();
    }
    private static boolean safe(EnvironmentState environment) {
        return environment.breathable() && environment.coldTier() == 0
                && environment.heatTier() == 0 && environment.radiationTier() == 0;
    }
    @Override public boolean stillValid(Player player) {
        return player == owner && player.level() == level && player.isAlive() && !player.isSpectator()
                && player.level().getBlockState(pos).is(ModBlocks.EPP_SERVICE_STATION.get())
                && player.distanceToSqr(pos.getCenter()) <= 64;
    }
    @Override public void broadcastChanges() {
        if (!owner.level().isClientSide) safe.set(canService() ? 1 : 0);
        super.broadcastChanges();
    }
    public boolean canUpgrade() {
        return EppItem.generation(tray.getItem(0)) == 1 && tray.getItem(2).is(ModItems.EPP_MK2_UPGRADE_KIT.get());
    }
    public boolean canInstall() {
        var pack = tray.getItem(0); var stack = tray.getItem(1);
        return pack.getItem() instanceof EppItem chassis && chassis.moduleSlots() > 0
                && stack.getCount() == 1 && stack.getItem() instanceof EppModuleItem module
                && module.tier() <= chassis.maxModuleTier() && modules(pack).nonEmptyStream().findAny().isEmpty();
    }
    public boolean canRemove() {
        return EppEquipmentResolver.valid(tray.getItem(0)) && tray.getItem(1).isEmpty()
                && modules(tray.getItem(0)).nonEmptyStream().findAny().isPresent();
    }
    private static ItemContainerContents modules(ItemStack pack) {
        return pack.getOrDefault(ModDataComponents.EPP_MODULES, ItemContainerContents.EMPTY);
    }
    @Override public boolean clickMenuButton(Player player, int action) {
        // Vanilla containerId routing is supplemented with ownership, block, range and air checks.
        if (player != owner || player.containerMenu != this || !canService()) return false;
        var pack = tray.getItem(0);
        switch (action) {
            case UPGRADE -> {
                if (!canUpgrade()) return false;
                int oxygen = EppItem.oxygen(pack);
                var upgraded = new ItemStack(ModItems.EPP_MK2.get());
                upgraded.applyComponents(pack.getComponentsPatch());
                EppItem.setOxygen(upgraded, oxygen);
                var retained = new ArrayList<ItemStack>();
                var returned = new ArrayList<ItemStack>();
                var chassis = (EppItem) upgraded.getItem();
                for (var moduleStack : modules(pack).nonEmptyStream().toList()) {
                    if (retained.size() < chassis.moduleSlots() && moduleStack.getCount() == 1
                            && moduleStack.getItem() instanceof EppModuleItem module && module.tier() <= chassis.maxModuleTier())
                        retained.add(moduleStack);
                    else returned.add(moduleStack);
                }
                upgraded.set(ModDataComponents.EPP_MODULES, ItemContainerContents.fromItems(retained));
                tray.setItem(0, upgraded);
                tray.removeItem(2, 1);
                // Vanilla returns to inventory or drops at the owner if it is full; never erase contents.
                returned.forEach(player.getInventory()::placeItemBackInInventory);
            }
            case INSTALL -> {
                if (!canInstall()) return false;
                pack.set(ModDataComponents.EPP_MODULES, ItemContainerContents.fromItems(List.of(tray.removeItem(1, 1))));
            }
            case REMOVE -> {
                if (!canRemove()) return false;
                var installed = new ArrayList<>(modules(pack).nonEmptyStream().toList());
                tray.setItem(1, installed.removeFirst());
                pack.set(ModDataComponents.EPP_MODULES, ItemContainerContents.fromItems(installed));
            }
            default -> { return false; }
        }
        tray.setChanged(); broadcastChanges(); return true;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, tray);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var original = stack.copy();
        boolean moved;
        if (index < 3) moved = moveItemStackTo(stack, 3, 39, true);
        else if (EppEquipmentResolver.valid(stack)) moved = moveItemStackTo(stack, 0, 1, false);
        else if (stack.getItem() instanceof EppModuleItem) moved = moveItemStackTo(stack, 1, 2, false);
        else if (stack.is(ModItems.EPP_MK2_UPGRADE_KIT.get())) moved = moveItemStackTo(stack, 2, 3, false);
        else moved = index < 30 ? moveItemStackTo(stack, 30, 39, false) : moveItemStackTo(stack, 3, 30, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return original;
    }
}
