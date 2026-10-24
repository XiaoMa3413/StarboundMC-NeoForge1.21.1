package com.starboundmc.client.weapon;

import com.starboundmc.network.ModNetwork;
import com.starboundmc.weapon.EnergyGunItem;
import com.starboundmc.weapon.GunTriggerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "starboundmc", value = Dist.CLIENT)
public final class GunInput {
    private static ItemStack held;
    private static int heartbeat;
    private static int slot;
    private static long pressedAt;
    public static double chargeSeconds() { return held == null ? 0 : (System.nanoTime() - pressedAt) / 1e9; }
    private static boolean ready(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.screen == null && mc.isWindowActive()
                && !mc.player.isSpectator() && mc.player.isAlive()
                && mc.player.getMainHandItem().getItem() instanceof EnergyGunItem;
    }
    private static void press(Minecraft mc) {
        held = mc.player.getMainHandItem(); slot = mc.player.getInventory().selected; heartbeat = 0; pressedAt = System.nanoTime();
        ModNetwork.sendToServer(new GunTriggerPacket(GunTriggerPacket.PRESS));
    }
    @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered event) {
        var mc = Minecraft.getInstance();
        if (!event.isAttack() || mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof EnergyGunItem)) return;
        event.setCanceled(true); event.setSwingHand(false);
        if (ready(mc) && held == null) press(mc);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!ready(mc) || held != null && (mc.player.getMainHandItem().getItem() != held.getItem()
                || mc.player.getInventory().selected != slot)) {
            if (held != null && mc.getConnection() != null) ModNetwork.sendToServer(new GunTriggerPacket(GunTriggerPacket.CANCEL));
            held = null; GunMotion.clear(); return;
        }
        if (mc.options.keyAttack.isDown()) {
            if (held == null) press(mc);
            else if (++heartbeat >= 5) { heartbeat = 0; ModNetwork.sendToServer(new GunTriggerPacket(GunTriggerPacket.PRESS)); }
        } else if (held != null) {
            ModNetwork.sendToServer(new GunTriggerPacket(GunTriggerPacket.RELEASE)); held = null;
        }
    }
}
