package com.starboundmc.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Tooltip also supplies the printer's existing item-description panel. */
public final class SublightIgnitionCoreItem extends Item {
    public SublightIgnitionCoreItem(Properties properties) { super(properties); }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
                                          List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.starboundmc.sublight_ignition_core.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
