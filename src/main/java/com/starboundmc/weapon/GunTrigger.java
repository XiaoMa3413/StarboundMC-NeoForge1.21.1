package com.starboundmc.weapon;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "starboundmc")
public final class GunTrigger {
    private static final Map<ServerPlayer, Held> HELD = new WeakHashMap<>();
    private static final int TIMEOUT = 15;
    private static final class Held {
        final ItemStack stack;
        final long started;
        final ResourceKey<Level> dimension;
        long heartbeat;
        Held(ServerPlayer player) {
            stack = player.getMainHandItem(); started = heartbeat = player.serverLevel().getGameTime();
            dimension = player.level().dimension();
        }
    }
    private static boolean valid(ServerPlayer player, Held held) {
        return player.isAlive() && !player.isSpectator() && player.containerMenu == player.inventoryMenu
                && player.getMainHandItem() == held.stack && held.stack.getItem() instanceof EnergyGunItem
                && player.level().dimension().equals(held.dimension)
                && player.serverLevel().getGameTime() - held.heartbeat <= TIMEOUT;
    }
    public static void input(ServerPlayer player, int action) {
        if (action == GunTriggerPacket.CANCEL) { HELD.remove(player); return; }
        if (action == GunTriggerPacket.RELEASE) {
            var held = HELD.remove(player);
            if (held != null && valid(player, held) && held.stack.getItem() instanceof EnergyGunItem gun
                    && gun.profile().chargeTicks > 0) {
                int ticks = (int)Math.min(72000, player.serverLevel().getGameTime() - held.started);
                float damage = gun.profile().chargedDamage(ticks);
                if (damage > 0) gun.fire(player, held.stack, damage);
            }
            return;
        }
        if (action != GunTriggerPacket.PRESS) return;
        var held = HELD.get(player);
        if (held != null && valid(player, held)) { held.heartbeat = player.serverLevel().getGameTime(); return; }
        HELD.remove(player);
        held = new Held(player);
        if (!valid(player, held)) return;
        var gun = (EnergyGunItem)held.stack.getItem();
        if (player.getCooldowns().isOnCooldown(gun)) return;
        HELD.put(player, held);
        if (!WeaponCombat.hasAmmo(player, gun.ammunition(), gun.profile().ammoCost))
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("weapon.starboundmc.no_ammo",
                    gun.profile().ammoCost, gun.ammunition().getDescription()), true);
        if (gun.profile().chargeTicks == 0) gun.fire(player, held.stack, gun.profile().damage);
    }
    public static void tick(ServerPlayer player) {
        var held = HELD.get(player);
        if (held == null) return;
        if (!valid(player, held)) { HELD.remove(player); return; }
        var gun = (EnergyGunItem)held.stack.getItem();
        if (gun.profile().chargeTicks == 0) gun.fire(player, held.stack, gun.profile().damage);
        else if (player.serverLevel().getGameTime() % 4 == 0)
            WeaponEffects.charge(player.serverLevel(), player.getEyePosition().add(player.getLookAngle().scale(1.2))
                    .add(player.getLookAngle().cross(new net.minecraft.world.phys.Vec3(0, 1, 0)).scale(-.3)).add(0, -.25, 0));
    }
    @SubscribeEvent public static void tickPlayer(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) tick(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { HELD.remove(event.getEntity()); }
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof EnergyGunItem) event.setCanceled(true);
    }
    @SubscribeEvent public static void mine(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof EnergyGunItem) event.setCanceled(true);
    }
}
