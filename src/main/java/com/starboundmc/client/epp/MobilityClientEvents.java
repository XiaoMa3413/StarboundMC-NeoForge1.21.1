package com.starboundmc.client.epp;

import com.starboundmc.StarboundMC;
import com.starboundmc.epp.EvaMovement;
import com.starboundmc.epp.EvaState;
import com.starboundmc.network.JumpBoostPacket;
import com.starboundmc.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class MobilityClientEvents {
    private static boolean jumping;
    private static Object lastPlayer;
    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        var input = event.getInput();
        boolean pressed = input.jumping;
        boolean equipped = player.getData(com.starboundmc.story.ModAttachments.MOBILITY_STATE).equipped;
        if (player != lastPlayer) { jumping = input.jumping; lastPlayer = player; }
        if (equipped && input.jumping && !jumping && !player.onGround() && Minecraft.getInstance().screen == null
                && !player.getAbilities().flying && EvaMovement.mode(player) == EvaState.NORMAL) {
            ModNetwork.sendToServer(new JumpBoostPacket((input.up ? 1 : 0) - (input.down ? 1 : 0),
                    (input.left ? 1 : 0) - (input.right ? 1 : 0)));
        }
        // Preserve vanilla input, including creative double-tap flight.
        jumping = pressed;
    }
}
