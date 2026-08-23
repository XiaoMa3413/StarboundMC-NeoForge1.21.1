package com.starboundmc.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class MatterManipulatorModuleItem extends Item
{
    public MatterManipulatorModuleItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
    {
        tooltip.add(Component.translatable("item.starboundmc.matter_manipulator_module.tooltip"));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
