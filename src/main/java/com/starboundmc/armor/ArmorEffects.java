package com.starboundmc.armor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Equipment is authoritative. No saved cooldowns or potion effects can survive removing a set. */
@EventBusSubscriber(modid = "starboundmc")
public final class ArmorEffects {
    public static boolean wears(Player player, ArmorProfile profile, EquipmentSlot slot) {
        return player.getItemBySlot(slot).getItem() instanceof CombatArmorItem item
                && item.profile == profile && item.getEquipmentSlot() == slot;
    }
    public static boolean fullSet(Player player, ArmorProfile profile) {
        return ModArmor.TYPES.stream().allMatch(type -> wears(player, profile, type.getSlot()));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) refresh(player);
    }
    public static void refresh(ServerPlayer player) {
        for (var profile : ArmorProfile.values()) {
            boolean active = player.isAlive() && !player.isSpectator() && fullSet(player, profile);
            var id = ResourceLocation.fromNamespaceAndPath("starboundmc", "armor_set/" + profile.id());
            for (var bonus : profile.fullSet()) {
                var attribute = player.getAttribute(bonus.attribute());
                if (attribute == null) continue;
                if (active && !attribute.hasModifier(id)) attribute.addTransientModifier(new AttributeModifier(id, bonus.amount(), bonus.operation()));
                else if (!active) attribute.removeModifier(id);
            }
        }
        if (player.getHealth() > player.getMaxHealth()) player.setHealth(player.getMaxHealth());
    }
    public static double projectileMultiplier(Player player) {
        if (!player.isAlive() || player.isSpectator()) return 1;
        double bonus = 0;
        for (var profile : ArmorProfile.values()) bonus += profile.projectileBonus(
                wears(player, profile, EquipmentSlot.HEAD), fullSet(player, profile));
        return 1 + bonus;
    }
    @SubscribeEvent public static void projectile(LivingIncomingDamageEvent event) {
        if (!event.getEntity().level().isClientSide && event.getSource().is(DamageTypeTags.IS_PROJECTILE)
                && event.getSource().getEntity() instanceof ServerPlayer player)
            event.setAmount(event.getAmount() * (float) projectileMultiplier(player));
    }
}
