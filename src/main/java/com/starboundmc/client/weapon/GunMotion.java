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

/** Cosmetic held-gun poses and first-person motion; never changes authoritative aim or charge. */
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

    public static void alignCrouchedPose(net.minecraft.client.model.HumanoidModel<?> model,
            net.minecraft.world.entity.LivingEntity entity) {
        if (!model.crouching || !(entity.getMainHandItem().getItem() instanceof EnergyGunItem)) return;
        var pose = entity.getMainArm() == HumanoidArm.RIGHT ? model.rightArmPose : model.leftArmPose;
        if (pose != net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD
                || entity.isFallFlying() || entity.isVisuallySwimming()) return;
        // Vanilla adds 0.4 rad to both arms after CROSSBOW_HOLD. Undo only that cosmetic
        // offset; preserve the lowered shoulder/body, head tracking and breathing motion.
        model.rightArm.xRot -= .4f;
        model.leftArm.xRot -= .4f;
    }

    @Override public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(
            net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand, ItemStack stack) {
        // Native pose follows replicated head angles for local and remote players alike.
        // Leave swimming, flight and active offhand use to vanilla animation.
        if (hand != net.minecraft.world.InteractionHand.MAIN_HAND || entity.isFallFlying() || entity.isVisuallySwimming()
                || (entity.isUsingItem() && entity.getUsedItemHand() != hand)) return null;
        return net.minecraft.client.model.HumanoidModel.ArmPose.CROSSBOW_HOLD;
    }

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
