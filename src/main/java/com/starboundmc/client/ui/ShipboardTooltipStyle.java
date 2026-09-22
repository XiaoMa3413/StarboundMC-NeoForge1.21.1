// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.ui;

import com.starboundmc.StarboundMC;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** Palette is opt-in per root, so personal inventories and unrelated screens stay independent. */
@EventBusSubscriber(modid = StarboundMC.MODID, value = Dist.CLIENT)
public final class ShipboardTooltipStyle {
    private ShipboardTooltipStyle() {}

    @SubscribeEvent
    public static void color(RenderTooltipEvent.Color event) {
        if (Minecraft.getInstance().screen instanceof StarboundModularScreen<?, ?> screen
                && screen.usesShipboardTooltips()) {
            event.setBackground(0xF8101A22);
            event.setBorderStart(0xFF526975);
            event.setBorderEnd(0xFF344B55);
        }
    }
}
