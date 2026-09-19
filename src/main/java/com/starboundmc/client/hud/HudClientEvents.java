// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud;

import com.starboundmc.StarboundMC;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Advances connection-local HUD presentation without coupling it to N.O.V.A. rendering. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class HudClientEvents {
    private HudClientEvents() { }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        HudBootController.INSTANCE.tick(minecraft.player == null || minecraft.level == null
                || minecraft.options.hideGui || minecraft.isPaused() || minecraft.screen != null);
    }
}
