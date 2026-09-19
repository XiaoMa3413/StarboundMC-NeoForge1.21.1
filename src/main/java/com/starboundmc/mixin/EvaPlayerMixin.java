// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.mixin;

import com.starboundmc.epp.EvaMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class EvaPlayerMixin {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void starboundmc$evaTravel(Vec3 input, CallbackInfo ci) {
        if (EvaMovement.travel((Player) (Object) this)) ci.cancel();
    }
}
