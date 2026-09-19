package com.starboundmc.mobility;

import com.starboundmc.StarboundMC;
import com.starboundmc.epp.EvaMovement;
import com.starboundmc.epp.EvaState;
import com.starboundmc.story.ModAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = StarboundMC.MODID)
public final class MobilityEvents {
    private MobilityEvents() { }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            recharge(player);
            var state = player.getData(ModAttachments.MOBILITY_STATE);
            boolean equipped = MobilityEquipmentResolver.active(player).isPresent();
            int flags = (equipped ? 1 : 0) | (state.charged ? 2 : 0);
            if (flags != state.lastSent || player.tickCount % 20 == 0) {
                state.lastSent = flags;
                com.starboundmc.network.ModNetwork.sendToPlayer(player,
                        new com.starboundmc.network.MobilityStatePacket(equipped, state.charged));
            }
        }
    }
    public static void recharge(ServerPlayer player) {
        var state = player.getData(ModAttachments.MOBILITY_STATE);
        if (!player.isAlive() || player.isSpectator()) {
            state.charged = false; state.groundedTicks = 0; return;
        }
        if (player.onGround() && !player.getAbilities().flying && !player.isPassenger()
                && EvaMovement.serverMode(player) == EvaState.NORMAL) {
            state.groundedTicks = Math.min(3, state.groundedTicks + 1);
            if (state.groundedTicks >= 3) state.charged = true;
        } else state.groundedTicks = 0;
    }
    public static boolean boost(ServerPlayer player, int forward, int sideways) {
        var state = player.getData(ModAttachments.MOBILITY_STATE);
        if (!state.charged || !player.isAlive() || player.isSpectator() || player.onGround()
                || player.getAbilities().flying || player.isFallFlying() || player.isPassenger()
                || player.isInWaterOrBubble() || player.isInLava() || player.onClimbable()
                || EvaMovement.serverMode(player) != EvaState.NORMAL
                || MobilityEquipmentResolver.active(player).isEmpty()
                || Math.abs((long) forward) > 1 || Math.abs((long) sideways) > 1) return false;
        state.charged = false;
        state.groundedTicks = 0;
        double yaw = Math.toRadians(player.getYRot());
        Vec3 direction = new Vec3(-Math.sin(yaw) * forward + Math.cos(yaw) * sideways, 0,
                Math.cos(yaw) * forward + Math.sin(yaw) * sideways).normalize().scale(.18);
        var motion = player.getDeltaMovement();
        player.setDeltaMovement(Math.clamp(motion.x + direction.x, -.7, .7), .6,
                Math.clamp(motion.z + direction.z, -.7, .7));
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        // Protection is checked at impact, so removing the rig restores normal fall damage.
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + .1,
                player.getZ(), 10, .18, .08, .18, .04);
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE,
                SoundSource.PLAYERS, .4f, 1.6f);
        return true;
    }
    @SubscribeEvent public static void fallProtection(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getSource().is(DamageTypes.FALL)
                && MobilityEquipmentResolver.active(player).isPresent()) event.setCanceled(true);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        var stack = player.getData(ModAttachments.MOBILITY_EQUIPMENT);
        if (stack.isEmpty()) return;
        player.setData(ModAttachments.MOBILITY_EQUIPMENT, ItemStack.EMPTY);
        event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
    }
}
