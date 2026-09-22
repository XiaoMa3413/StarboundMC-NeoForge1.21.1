// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.provider;

import com.starboundmc.StarboundMC;
import com.starboundmc.client.hud.ar.ArContext;
import com.starboundmc.client.hud.ar.ArGuidanceMode;
import com.starboundmc.client.hud.ar.ArTarget;
import com.starboundmc.client.hud.ar.ArTargetCategory;
import com.starboundmc.client.hud.ar.ArTargetProvider;
import com.starboundmc.client.space.RelayClientState;
import com.starboundmc.encounter.RelayGeometry;
import com.starboundmc.world.ShipDimensions;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Converts the currently active relay encounter into a renderer-agnostic POI target. */
public final class RelayPoiProvider implements ArTargetProvider {
    @Override
    public void collect(ArContext context, Consumer<ArTarget> output) {
        var snapshot = RelayClientState.snapshot;
        if (!context.evaNavigation()
                || !context.dimension().equals(ShipDimensions.SHIP_LEVEL)
                || !RelayClientState.local() || snapshot == null)
            return;
        var position = RelayGeometry.center(snapshot.origin());
        var id = ResourceLocation.fromNamespaceAndPath(StarboundMC.MODID, "relay_poi/" + snapshot.origin().asLong());
        output.accept(new ArTarget(id, ArTargetCategory.POI, position,
                ShipBeaconProvider.label(context.viewerPosition(), position,
                        "hud.starboundmc.relay.beacon"),
                ArGuidanceMode.TARGET, 80, Double.POSITIVE_INFINITY, 0xFFD17C));
    }
}
