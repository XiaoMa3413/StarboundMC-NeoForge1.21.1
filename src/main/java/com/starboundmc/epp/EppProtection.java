// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.item.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Derived only from the unique active chassis and supported installed modules. */
public record EppProtection(boolean lifeSupport, int coldTier, int heatTier, int radiationTier) {
    public static final EppProtection NONE = new EppProtection(false, 0, 0, 0);
    public static EppProtection from(ItemStack pack) {
        if (!EppEquipmentResolver.valid(pack)) return NONE;
        var chassis = (EppItem) pack.getItem();
        var modules = pack.getOrDefault(ModDataComponents.EPP_MODULES, ItemContainerContents.EMPTY);
        int cold = 0, heat = 0, radiation = 0;
        for (int slot = 0; slot < Math.min(chassis.moduleSlots(), modules.getSlots()); slot++) {
            var stack = modules.getStackInSlot(slot);
            if (stack.getCount() != 1 || !(stack.getItem() instanceof EppModuleItem module)
                    || module.tier() > chassis.maxModuleTier()) continue;
            switch (module.hazard()) {
                case COLD -> cold = Math.max(cold, module.tier());
                case HEAT -> heat = Math.max(heat, module.tier());
                case RADIATION -> radiation = Math.max(radiation, module.tier());
            }
        }
        return new EppProtection(true, cold, heat, radiation);
    }
}
