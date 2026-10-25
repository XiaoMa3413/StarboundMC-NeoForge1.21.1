package com.starboundmc.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;

public final class CombatArmorItem extends ArmorItem {
    public final ArmorProfile profile;
    public CombatArmorItem(Holder<ArmorMaterial> material, Type type, Properties properties, ArmorProfile profile) {
        super(material, type, properties.durability(type.getDurability(profile.durability)));
        this.profile = profile;
    }
    @Override public ItemAttributeModifiers getDefaultAttributeModifiers() {
        var builder = ItemAttributeModifiers.builder();
        for (var entry : super.getDefaultAttributeModifiers().modifiers())
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        for (var bonus : profile.piece(type)) builder.add(bonus.attribute(), new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath("starboundmc", "armor_piece/" + type.getName()),
                bonus.amount(), bonus.operation()), EquipmentSlotGroup.bySlot(type.getSlot()));
        return builder.build();
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("armor.starboundmc." + profile.id() + "." + type.getName()).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("armor.starboundmc." + profile.id() + ".set").withStyle(ChatFormatting.GOLD));
    }
}
