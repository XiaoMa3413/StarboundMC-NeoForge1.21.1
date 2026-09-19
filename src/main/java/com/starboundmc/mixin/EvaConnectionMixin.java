// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.mixin;

import com.starboundmc.epp.EvaMovement;
import com.starboundmc.epp.EvaState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class EvaConnectionMixin {
    @Shadow public ServerPlayer player;
    @Shadow private boolean clientIsFloating;

    // Exempt only server-verified zero gravity; never grant creative flight abilities.
    // Tick also covers entering zero gravity between movement packets. Movement-packet
    // speed, loaded-chunk and collision validation is unchanged.
    @Inject(method = "tick", at = @At("HEAD"))
    private void starboundmc$allowZeroGravity(CallbackInfo ci) {
        if (EvaMovement.serverMode(player) != EvaState.NORMAL) clientIsFloating = false;
    }
}
