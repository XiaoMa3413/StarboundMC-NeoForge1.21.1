// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.provider;

import com.starboundmc.StarboundMC;
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

/** Adapts the current single-ship teleporter anchor into a generic AR target. */
public final class ShipBeaconProvider implements ArTargetProvider {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            StarboundMC.MODID, "ship_beacon");

    @Override
    public void collect(ArContext context, Consumer<ArTarget> output) {
        if (!context.evaNavigation()
                || !context.dimension().equals(ShipDimensions.SHIP_LEVEL))
            return;
        Vec3 position = Vec3.atBottomCenterOf(ShipStructure.SHIP_TELEPORTER_POS).add(0, 1, 0);
        output.accept(new ArTarget(ID, ArTargetCategory.NAVIGATION, position,
                label(context.viewerPosition(), position, "hud.starboundmc.eva.beacon"),
                ArGuidanceMode.TARGET, 100, Double.POSITIVE_INFINITY, 0x95E8E2));
    }

    static Component label(Vec3 viewer, Vec3 target, String key) {
        Vec3 delta = target.subtract(viewer);
        return Component.translatable(key, Math.round(delta.length()),
                (delta.y >= 0 ? "+" : "") + Math.round(delta.y));
    }
}
