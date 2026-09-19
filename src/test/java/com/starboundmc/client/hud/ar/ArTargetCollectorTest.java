// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArTargetCollectorTest {
    private static final ArContext CONTEXT = new ArContext(Level.OVERWORLD, Vec3.ZERO);

    @Test
    void aggregatesFiltersSortsAndDeduplicatesProviderOutput() {
        ArTargetProvider first = (context, output) -> {
            output.accept(target("near", 5, 20, 100));
            output.accept(target("duplicate", 8, 10, 100));
            output.accept(target("far", 50, 50, 100));
        };
        ArTargetProvider second = (context, output) -> {
            output.accept(target("duplicate", 3, 40, 100));
            output.accept(target("filtered", 20, 60, 5));
        };

        var result = new ArTargetCollector(List.of(first, second)).collect(CONTEXT);
        assertEquals(List.of("far", "duplicate", "near"), result.stream()
                .map(target -> target.id().getPath()).toList());
        assertEquals(3, result.get(1).worldPosition().x);
    }

    @Test
    void capsTheRenderedSetToEightTargets() {
        ArTargetProvider provider = (context, output) -> {
            for (int i = 0; i < 12; i++)
                output.accept(target("target_" + i, i + 1, i, 100));
        };
        var result = new ArTargetCollector(List.of(provider)).collect(CONTEXT);
        assertEquals(8, result.size());
        assertEquals(List.of(11, 10, 9, 8, 7, 6, 5, 4), result.stream()
                .map(ArTarget::priority).toList());
    }

    private static ArTarget target(String path, double x, int priority, double maxDistance) {
        return new ArTarget(ResourceLocation.fromNamespaceAndPath("starboundmc", path),
                ArTargetCategory.POI, new Vec3(x, 0, 0), Component.literal(path),
                ArGuidanceMode.TARGET, priority, maxDistance, 0xFFFFFF);
    }
}
