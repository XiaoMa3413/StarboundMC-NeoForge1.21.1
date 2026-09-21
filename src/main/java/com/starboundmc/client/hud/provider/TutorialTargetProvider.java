// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.provider;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.hud.HudBootController;
import com.starboundmc.client.hud.ar.ArContext;
import com.starboundmc.client.hud.ar.ArGuidanceMode;
import com.starboundmc.client.hud.ar.ArTarget;
import com.starboundmc.client.hud.ar.ArTargetCategory;
import com.starboundmc.client.hud.ar.ArTargetProvider;
import com.starboundmc.world.ShipDimensions;
import com.starboundmc.world.ShipStructure;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** First-use world marker that points at the real shipboard AI terminal. */
public final class TutorialTargetProvider implements ArTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "tutorial_ai_terminal");

    @Override
    public void collect(ArContext context, Consumer<ArTarget> output) {
        if (!HudBootController.INSTANCE.terminalGuidanceActive()
                || !context.dimension().equals(ShipDimensions.SHIP_LEVEL))
            return;
        Vec3 position = Vec3.atCenterOf(ShipStructure.SHIP_AI_TERMINAL_POS);
        output.accept(new ArTarget(ID, ArTargetCategory.INTERACTION, position,
                Component.translatable("hud.starboundmc.ar.ai_terminal",
                        Math.round(context.viewerPosition().distanceTo(position))),
                ArGuidanceMode.INTERACTION, 140, 96, 0xFFD17C));
    }
}
