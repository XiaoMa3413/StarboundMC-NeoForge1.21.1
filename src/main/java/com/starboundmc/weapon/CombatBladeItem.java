package com.starboundmc.weapon;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.starboundmc.item.ModItems;

public final class CombatBladeItem extends SwordItem {
    private final WeaponProfile profile;
    public CombatBladeItem(Properties properties, WeaponProfile profile) {
        super(new BladeTier(profile), properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID,
                        profile.damage - 1, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID,
                        profile.attackSpeed - 4, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND).build()));
        this.profile = profile;
    }
    public WeaponProfile profile() { return profile; }
    private record BladeTier(WeaponProfile profile) implements Tier {
        public int getUses() { return profile.durability; }
        public float getSpeed() { return Tiers.IRON.getSpeed(); }
        public float getAttackDamageBonus() { return 0; }
        public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return Tiers.IRON.getIncorrectBlocksForDrops();
        }
        public int getEnchantmentValue() { return 14; }
        public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() {
            return net.minecraft.world.item.crafting.Ingredient.of(profile == WeaponProfile.TUNGSTEN_SABRE
                    ? ModItems.TUNGSTEN_INGOT.get() : profile == WeaponProfile.STARCORE_BLADE
                    ? ModItems.STAR_CORE_FRAGMENT.get() : ModItems.DURASTEEL_INGOT.get());
        }
    }
    @Override public boolean isValidRepairItem(ItemStack weapon, ItemStack ingredient) {
        return getTier().getRepairIngredient().test(ingredient);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (profile.range == 0) return InteractionResultHolder.pass(stack);
        if (hand != InteractionHand.MAIN_HAND || !player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(this))
            return InteractionResultHolder.fail(stack);
        if (player.getAttackStrengthScale(0) < 0.9f) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("weapon.starboundmc.recover"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel server) {
            player.getCooldowns().addCooldown(this, profile.cooldown);
            player.resetAttackStrengthTicker();
            var origin = player.getEyePosition();
            var look = player.getLookAngle();
            var targets = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(profile.range),
                    target -> WeaponCombat.canHit(player, target) && player.hasLineOfSight(target)
                            && target.getEyePosition().distanceToSqr(origin) <= profile.range * profile.range
                            && target.getEyePosition().subtract(origin).normalize().dot(look) >= 0.4)
                    .stream().sorted(java.util.Comparator.comparingDouble(player::distanceToSqr)).limit(profile.targets).toList();
            for (var target : targets) {
                if (target.hurt(level.damageSources().playerAttack(player), profile.damage + 2))
                    target.knockback(0.6, player.getX() - target.getX(), player.getZ() - target.getZ());
            }
            for (int i = -2; i <= 2; i++) {
                var pos = origin.add(look.yRot(i * 0.28f).scale(profile.range * 0.6));
                server.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1, 0.8f);
            stack.hurtAndBreak(profile == WeaponProfile.STARCORE_BLADE ? 16 : 8, player, EquipmentSlot.MAINHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.starboundmc." + profile.id() + ".tooltip"));
    }
}
