package com.starboundmc.mixin;

import com.starboundmc.client.weapon.GunMotion;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Client-only final pose hook: keep raised guns aligned while the body crouches. */
@Mixin(HumanoidModel.class)
public abstract class HumanoidGunPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void starboundmc$alignCrouchedGun(LivingEntity entity, float limbSwing, float limbSwingAmount,
            float age, float headYaw, float headPitch, CallbackInfo ci) {
        GunMotion.alignCrouchedPose((HumanoidModel<?>)(Object)this, entity);
    }
}
