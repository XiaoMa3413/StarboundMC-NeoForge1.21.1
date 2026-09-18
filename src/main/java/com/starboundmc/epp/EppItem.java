// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.item.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class EppItem extends Item {
    private final int generation;
    public EppItem(Properties properties) { this(properties, 1); }
    public EppItem(Properties properties, int generation) {
        super(properties);
        if (generation < 1 || generation > 2) throw new IllegalArgumentException("Unsupported EPP generation");
        this.generation = generation;
    }
    public int moduleSlots() { return generation == 1 ? 0 : 1; }
    public int maxModuleTier() { return generation == 1 ? 0 : 1; }
    public static int generation(ItemStack stack) { return stack.getItem() instanceof EppItem item ? item.generation : 0; }
    public static int capacity(ItemStack stack) { return generation(stack) >= 2 ? EppConfig.MK2_CAPACITY.get() : EppConfig.CAPACITY.get(); }
    public static int oxygen(ItemStack stack) { return Math.clamp(stack.getOrDefault(ModDataComponents.EPP_OXYGEN, 0), 0, capacity(stack)); }
    public static void setOxygen(ItemStack stack, int units) { stack.set(ModDataComponents.EPP_OXYGEN, Math.clamp(units, 0, capacity(stack))); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.starboundmc.epp.oxygen", oxygen(stack), capacity(stack)));
        if (moduleSlots() > 0) {
            lines.add(Component.translatable("tooltip.starboundmc.epp.slots", moduleSlots(), maxModuleTier()));
            var modules = stack.getOrDefault(ModDataComponents.EPP_MODULES, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
            modules.nonEmptyStream().forEach(module -> lines.add(Component.translatable("tooltip.starboundmc.epp.installed", module.getHoverName())));
            lines.add(Component.translatable("tooltip.starboundmc.epp.cold_protection", EppProtection.from(stack).coldTier()));
            lines.add(Component.translatable("tooltip.starboundmc.epp.eva_pending"));
        }
        lines.add(Component.translatable("tooltip.starboundmc.epp.equip"));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * oxygen(stack) / capacity(stack)); }
    @Override public int getBarColor(ItemStack stack) { return 0x61D6D0; }
}
