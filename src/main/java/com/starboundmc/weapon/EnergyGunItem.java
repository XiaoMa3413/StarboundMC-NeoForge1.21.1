package com.starboundmc.weapon;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;

public final class EnergyGunItem extends Item {
    private final WeaponProfile profile;
    public EnergyGunItem(Properties properties, WeaponProfile profile) {
        super(properties.durability(profile.durability)); this.profile = profile;
    }
    public WeaponProfile profile() { return profile; }
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        // Durability arrives after every shot; it must not lower the gun as if switching weapons.
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
    public Item ammunition() { return profile == WeaponProfile.COIL_PISTOL ? Items.IRON_NUGGET : Items.REDSTONE; }

    boolean fire(Player player, ItemStack stack, float damage) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator()
                || player.getMainHandItem() != stack || player.getCooldowns().isOnCooldown(this)
                || !WeaponCombat.consumeAmmo(player, ammunition(), profile.ammoCost)) return false;
        player.getCooldowns().addCooldown(this, profile.cooldown);
        var end = WeaponCombat.wallEnd(player, profile.range);
        var targets = WeaponCombat.rayTargets(player, end, profile.targets);
        for (var target : targets) {
            boolean hurt = target.hurt(WeaponCombat.boltDamage(level, player), damage);
            if (hurt && profile.fireSeconds > 0) target.igniteForSeconds(profile.fireSeconds);
        }
        // Non-piercing shots stop visibly on the first body, even if invulnerable frames reject damage.
        if (profile.targets == 1 && !targets.isEmpty()) end = targets.getFirst().getBoundingBox()
                .inflate(0.15).clip(player.getEyePosition(), end).orElse(end);
        if (profile == WeaponProfile.STARCORE_RAILGUN) {
            var muzzle = player.getEyePosition().add(player.getLookAngle().scale(1.2)).add(0, -.2, 0);
            WeaponEffects.railTrail(level, muzzle, end);
            WeaponCombat.blast(level, player, end, damage, targets);
            WeaponEffects.detonation(level, end);
        } else WeaponCombat.beam(level, player.getEyePosition(), end, profile.fireSeconds > 0);
        level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS,
                0.45f, profile.chargeTicks > 0 ? 0.6f : 1.6f);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            com.starboundmc.network.ModNetwork.sendToPlayer(serverPlayer, new GunShotPacket(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this), player.getInventory().selected));
        stack.hurtAndBreak(profile.chargeTicks > 0 ? 4 : 1, player, EquipmentSlot.MAINHAND);
        return true;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("item.starboundmc." + profile.id() + ".tooltip").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("weapon.starboundmc.gun_stats", profile.damage, profile.range,
                profile.cooldown / 20.0).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("weapon.starboundmc.ammo", profile.ammoCost,
                ammunition().getDescription()).withStyle(ChatFormatting.GRAY));
    }
    @Override public boolean isValidRepairItem(ItemStack weapon, ItemStack ingredient) {
        return ingredient.is(profile == WeaponProfile.COIL_PISTOL
                ? com.starboundmc.item.ModItems.TUNGSTEN_INGOT.get() : profile == WeaponProfile.STARCORE_RAILGUN
                ? com.starboundmc.item.ModItems.STAR_CORE_FRAGMENT.get() : com.starboundmc.item.ModItems.DURASTEEL_INGOT.get());
    }
}
