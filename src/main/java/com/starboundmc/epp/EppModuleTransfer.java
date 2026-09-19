package com.starboundmc.epp;

import com.starboundmc.item.ModDataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import java.util.ArrayList;

/** Move exactly one cursor stack and one module socket under the active menu's authority. */
public final class EppModuleTransfer {
    private EppModuleTransfer() { }
    public static boolean exchange(ServerPlayer player, int containerId, int stateId, int slotId, int moduleIndex) {
        AbstractContainerMenu menu = player.containerMenu;
        if (!player.isAlive() || player.isSpectator() || menu.containerId != containerId
                || !menu.stillValid(player) || menu.getStateId() != stateId
                || slotId < 0 || slotId >= menu.slots.size()) return false;
        var slot = menu.getSlot(slotId);
        var pack = slot.getItem();
        if (!(pack.getItem() instanceof EppItem chassis) || pack.getCount() != 1
                || !slot.isActive() || !slot.mayPickup(player) || !slot.mayPlace(pack)
                || moduleIndex < 0 || moduleIndex >= chassis.moduleSlots()) return false;
        var carried = menu.getCarried();
        if (!carried.isEmpty() && (carried.getCount() != 1
                || !(carried.getItem() instanceof EppModuleItem module) || !module.compatibleWith(chassis))) return false;
        var contents = pack.getOrDefault(ModDataComponents.EPP_MODULES, ItemContainerContents.EMPTY);
        var modules = new ArrayList<ItemStack>();
        contents.stream().forEach(stack -> modules.add(stack.copy()));
        // Never truncate unexpected stored contents.
        if (modules.size() > chassis.moduleSlots()) return false;
        while (modules.size() < chassis.moduleSlots()) modules.add(ItemStack.EMPTY);
        var removed = modules.get(moduleIndex);
        if (carried.isEmpty() && removed.isEmpty()) return false;
        modules.set(moduleIndex, carried.copy());
        var changed = pack.copy();
        changed.set(ModDataComponents.EPP_MODULES, ItemContainerContents.fromItems(modules));
        slot.set(changed);
        slot.setChanged();
        menu.setCarried(removed);
        menu.broadcastChanges();
        return true;
    }
}
