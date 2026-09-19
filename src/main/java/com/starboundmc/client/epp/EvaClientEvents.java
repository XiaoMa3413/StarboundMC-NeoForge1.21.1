// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.epp;

import com.starboundmc.StarboundMC;
import com.starboundmc.epp.EvaMotion;
import com.starboundmc.epp.EvaMovement;
import com.starboundmc.epp.EvaState;
import com.starboundmc.network.EvaInputPacket;
import com.starboundmc.network.ModNetwork;
import com.starboundmc.story.ModAttachments;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class EvaClientEvents {
    private EvaClientEvents() { }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        if (EvaMovement.mode(player) == EvaState.NORMAL) return;
        var state = player.getData(ModAttachments.EVA);
        var input = event.getInput();
        int keys = 0;
        if (Minecraft.getInstance().screen == null && state.mode == EvaState.THRUST) {
            keys = (input.up ? EvaMotion.FORWARD : 0) | (input.down ? EvaMotion.BACK : 0)
                    | (input.left ? EvaMotion.LEFT : 0) | (input.right ? EvaMotion.RIGHT : 0)
                    | (input.jumping ? EvaMotion.UP : 0) | (input.shiftKeyDown ? EvaMotion.DOWN : 0);
        }
        state.input = keys;
        state.lastInputTick = player.tickCount;
        if (keys != state.lastSentInput || player.tickCount % 5 == 0) {
            ModNetwork.sendToServer(new EvaInputPacket(keys));
            state.lastSentInput = keys;
        }
        // Suppress ground jumping, crouch edge-clamping and ordinary acceleration in zero gravity.
        input.jumping = false;
        input.shiftKeyDown = false;
        input.leftImpulse = 0;
        input.forwardImpulse = 0;
        player.setSprinting(false);
    }
}
