// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.epp;

import com.starboundmc.StarboundMC;
import com.starboundmc.item.ModItems;
import com.starboundmc.network.*;
import com.starboundmc.story.ModAttachments;
import com.starboundmc.story.ShipEnvironmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = StarboundMC.MODID)
public final class EppEvents {
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> HEAT_DAMAGE =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "heat"));
    private EppEvents() { }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.tickCount % 20 != 0) return;
        tickSecond(player);
    }
    public static void tickSecond(ServerPlayer player) {
        var environment = PlayerEnvironmentService.at(player);
        ItemStack epp = EppEquipmentResolver.getActiveEpp(player).orElse(ItemStack.EMPTY);
        boolean equipped = !epp.isEmpty(), exempt = player.isCreative() || player.isSpectator();
        boolean refill = environment.pressurized() || player.containerMenu instanceof LifeSupportMenu menu && menu.refillsEpp(player);
        boolean breathable = environment.breathable();
        int capacity = EppItem.capacity(epp);
        int oxygen = equipped ? EppItem.oxygen(epp) : 0;
        var step = OxygenRules.step(oxygen, capacity, player.getData(ModAttachments.SUFFOCATION), equipped,
                breathable || exempt, refill, EppConfig.CONSUMPTION.get(), EppConfig.REFILL.get(), EppConfig.GRACE.get());
        if (equipped && oxygen != step.oxygen()) EppItem.setOxygen(epp, step.oxygen());
        player.setData(ModAttachments.SUFFOCATION, step.exposure());
        if (!exempt && step.damage()) player.hurt(player.damageSources().drown(), step.exposure() >= EppConfig.GRACE.get() + 5 ? 2 : 1);
        int warning = breathable || exempt ? 0 : OxygenRules.warning(step.oxygen(), equipped ? capacity : 0);
        int previous = player.getData(ModAttachments.EPP_WARNING);
        if (warning > previous && ShipEnvironmentService.isCoreOnline(player.getServer()))
            ModNetwork.sendToPlayer(player, new NovaBroadcastPacket("message.starboundmc.nova.oxygen." + warning));
        // Hysteresis: do not replay the same warning when a single refill unit crosses a threshold.
        if (warning > previous || breathable || (equipped && step.oxygen() > capacity / 2))
            player.setData(ModAttachments.EPP_WARNING, warning);
        var protection = EppProtection.from(epp);
        var cold = ExposureRules.step(player.getData(ModAttachments.COLD_EXPOSURE), exempt ? 0 : environment.coldTier(),
                protection.coldTier(), EppConfig.COLD_ACCUMULATION.get(), exempt ? ExposureRules.MAX : EppConfig.COLD_RECOVERY.get());
        player.setData(ModAttachments.COLD_EXPOSURE, cold.exposure());
        if (!exempt && cold.exposure() >= 50)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    40, cold.exposure() >= 75 ? 1 : 0, false, false, true));
        if (!exempt && cold.damage()) player.hurt(player.damageSources().freeze(), 1);
        int previousCold = player.getData(ModAttachments.COLD_WARNING);
        if (!exempt && cold.warning() > previousCold) {
            if (ShipEnvironmentService.isCoreOnline(player.getServer()))
                ModNetwork.sendToPlayer(player, new NovaBroadcastPacket("message.starboundmc.nova.cold." + cold.warning()));
            player.setData(ModAttachments.COLD_WARNING, cold.warning());
        } else if (cold.exposure() <= 10) player.setData(ModAttachments.COLD_WARNING, 0);
        var heat = ExposureRules.step(player.getData(ModAttachments.HEAT_EXPOSURE), exempt ? 0 : environment.heatTier(),
                protection.heatTier(), EppConfig.HEAT_ACCUMULATION.get(), exempt ? ExposureRules.MAX : EppConfig.HEAT_RECOVERY.get());
        player.setData(ModAttachments.HEAT_EXPOSURE, heat.exposure());
        if (!exempt && heat.exposure() >= 50)
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,
                    40, heat.exposure() >= 75 ? 1 : 0, false, false, true));
        if (!exempt && heat.damage()) player.hurt(new net.minecraft.world.damagesource.DamageSource(
                player.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(HEAT_DAMAGE)), 1);
        int previousHeat = player.getData(ModAttachments.HEAT_WARNING);
        if (!exempt && heat.warning() > previousHeat) {
            if (ShipEnvironmentService.isCoreOnline(player.getServer()))
                ModNetwork.sendToPlayer(player, new NovaBroadcastPacket("message.starboundmc.nova.heat." + heat.warning()));
            player.setData(ModAttachments.HEAT_WARNING, heat.warning());
        } else if (heat.exposure() <= 10) player.setData(ModAttachments.HEAT_WARNING, 0);
        int generation = EppItem.generation(epp);
        ModNetwork.sendToPlayer(player, new EppSnapshotPacket(step.oxygen(), capacity, step.exposure(), equipped,
                !breathable && !exempt, refill && equipped && step.oxygen() < capacity,
                generation, cold.exposure(), environment.coldTier(), protection.coldTier(),
                heat.exposure(), environment.heatTier(), protection.heatTier()));
        if (player.getData(ModAttachments.EPP_VISUAL) != generation) {
            player.setData(ModAttachments.EPP_VISUAL, generation);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new EppVisualPacket(player.getId(), generation));
        }
    }
    @SubscribeEvent public static void interrupt(LivingIncomingDamageEvent event) {
        if (event.getAmount() > 0 && event.getEntity() instanceof ServerPlayer player
                && player.isUsingItem() && player.getUseItem().is(ModItems.OXYGEN_CANISTER.get())) player.stopUsingItem();
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        ItemStack stack = player.getData(ModAttachments.EPP_EQUIPMENT);
        if (stack.isEmpty()) return;
        player.setData(ModAttachments.EPP_EQUIPMENT, ItemStack.EMPTY);
        event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer watcher && event.getTarget() instanceof ServerPlayer subject)
            ModNetwork.sendToPlayer(watcher, new EppVisualPacket(subject.getId(), generation(subject)));
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.sendToPlayer(player, new EppVisualPacket(player.getId(), generation(player)));
        }
    }
    @SubscribeEvent public static void dimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new EppVisualPacket(player.getId(), generation(player)));
    }
    private static int generation(ServerPlayer player) {
        return EppItem.generation(EppEquipmentResolver.getActiveEpp(player).orElse(ItemStack.EMPTY));
    }
}
