package com.starboundmc.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.starboundmc.item.ModItems;
import com.starboundmc.weapon.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.*;

/** First-person model motion only; never changes aim or authoritative charge. */
@EventBusSubscriber(modid = "starboundmc", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class GunMotion implements IClientItemExtensions {
    private static Item shotItem;
    private static int shotSlot;
    private static long shotTime;
    public static double lastPull, lastKick;
    public static int acknowledgedShots;

    @SubscribeEvent public static void register(RegisterClientExtensionsEvent event) {
        event.registerItem(new GunMotion(), ModItems.COIL_PISTOL.get(), ModItems.PLASMA_CARBINE.get(), ModItems.STARCORE_RAILGUN.get());
    }
    public static void shot(GunShotPacket packet) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !mc.player.isAlive()
                || mc.player.getInventory().selected != packet.slot()
                || !BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).equals(packet.item())) return;
        shotItem = mc.player.getMainHandItem().getItem(); shotSlot = packet.slot();
        shotTime = System.nanoTime(); acknowledgedShots++;
    }
    public static void clear() { shotItem = null; lastPull = lastKick = 0; }

    @Override public boolean applyForgeHandTransform(PoseStack poses, LocalPlayer player, HumanoidArm arm,
            ItemStack stack, float partialTick, float equip, float swing) {
        if (arm != player.getMainArm() || !(stack.getItem() instanceof EnergyGunItem gun)) return false;
        double age = (System.nanoTime() - shotTime) / 1e9;
        double kick = stack.getItem() == shotItem && shotSlot == player.getInventory().selected
                ? GunMotionCurve.recoil(age) : 0;
        double pull = gun.profile().chargeTicks > 0 ? GunMotionCurve.charge(GunInput.chargeSeconds()) : 0;
        double strength = gun.profile() == WeaponProfile.PLASMA_CARBINE ? .035
                : gun.profile() == WeaponProfile.STARCORE_RAILGUN ? .10 : .055;
        lastPull = pull * .12; lastKick = kick * strength;
        poses.translate(0, -pull * .015, lastPull + lastKick);
        poses.mulPose(Axis.XP.rotationDegrees((float)(kick * strength * 40)));
        return false;
    }
}
