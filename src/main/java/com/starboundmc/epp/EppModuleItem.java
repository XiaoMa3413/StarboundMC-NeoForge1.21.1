// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class EppModuleItem extends Item {
    public enum Hazard { COLD, HEAT, RADIATION }
    private final Hazard hazard;
    private final int tier;
    public EppModuleItem(Properties properties, Hazard hazard, int tier) {
        super(properties);
        if (tier < 1 || tier > 3) throw new IllegalArgumentException("Module tier must be 1..3");
        this.hazard = hazard; this.tier = tier;
    }
    public Hazard hazard() { return hazard; }
    public int tier() { return tier; }
    public boolean compatibleWith(EppItem chassis) {
        return chassis.moduleSlots() > 0 && (hazard != Hazard.RADIATION || tier <= chassis.maxModuleTier());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        if (hazard == Hazard.RADIATION) lines.add(Component.translatable("tooltip.starboundmc.epp.module", tier));
        if (hazard == Hazard.COLD) lines.add(Component.translatable("tooltip.starboundmc.epp.heating"));
        if (hazard == Hazard.HEAT) lines.add(Component.translatable("tooltip.starboundmc.epp.cooling"));
        lines.add(Component.translatable("tooltip.starboundmc.epp.service_only"));
    }
}
