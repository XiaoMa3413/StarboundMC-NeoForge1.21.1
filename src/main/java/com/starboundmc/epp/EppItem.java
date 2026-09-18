// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.item.ModDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class EppItem extends Item {
    public EppItem(Properties properties) { super(properties); }
    public static int oxygen(ItemStack stack) { return Math.clamp(stack.getOrDefault(ModDataComponents.EPP_OXYGEN, 0), 0, EppConfig.CAPACITY.get()); }
    public static void setOxygen(ItemStack stack, int units) { stack.set(ModDataComponents.EPP_OXYGEN, Math.clamp(units, 0, EppConfig.CAPACITY.get())); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.starboundmc.epp.oxygen", oxygen(stack), EppConfig.CAPACITY.get()));
        lines.add(Component.translatable("tooltip.starboundmc.epp.equip"));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13f * oxygen(stack) / EppConfig.CAPACITY.get()); }
    @Override public int getBarColor(ItemStack stack) { return 0x61D6D0; }
}
