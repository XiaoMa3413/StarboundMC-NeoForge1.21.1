package com.starboundmc.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * Hides the vanilla attribute block (which would show the attack speed value) and
 * re-adds only the attack damage line in the vanilla style.
 */
public class SurvivalKnifeItem extends Item {
    public SurvivalKnifeItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(CommonComponents.EMPTY);
        tooltip.add(Component.translatable("item.modifiers.mainhand").withStyle(ChatFormatting.GRAY));
        stack.forEachModifier(EquipmentSlotGroup.MAINHAND, (attribute, modifier) -> {
            if (modifier.is(Item.BASE_ATTACK_DAMAGE_ID)) {
                // Vanilla shows the combined value in tooltips: modifier plus the 1.0 player base.
                double total = modifier.amount() + 1.0;
                tooltip.add(CommonComponents.space().append(
                        Component.translatable(
                                "attribute.modifier.equals." + modifier.operation().id(),
                                ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(total),
                                Component.translatable(attribute.value().getDescriptionId())
                        )
                ).withStyle(ChatFormatting.DARK_GREEN));
            }
        });
    }
}
