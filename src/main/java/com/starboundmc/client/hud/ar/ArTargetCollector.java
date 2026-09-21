// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

/** Aggregates, distance-filters, de-duplicates and prioritizes provider output. */
public final class ArTargetCollector {
    private static final int MAX_TARGETS = 8;
    private final List<ArTargetProvider> providers;

    public ArTargetCollector(List<ArTargetProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    public List<ArTarget> collect(ArContext context) {
        var candidates = new ArrayList<ArTarget>();
        for (var provider : providers)
            provider.collect(context, candidates::add);

        var unique = new LinkedHashMap<net.minecraft.resources.ResourceLocation, ArTarget>();
        for (var target : candidates) {
            double distanceSquared = target.worldPosition().distanceToSqr(context.viewerPosition());
            if (Double.isFinite(target.maxDistance())
                    && distanceSquared > target.maxDistance() * target.maxDistance())
                continue;
            unique.merge(target.id(), target, (first, second) -> preferred(context, first, second));
        }

        return unique.values().stream()
                .sorted(Comparator.comparingInt(ArTarget::priority).reversed()
                        .thenComparingDouble(target -> target.worldPosition()
                                .distanceToSqr(context.viewerPosition()))
                        .thenComparing(target -> target.id().toString()))
                .limit(MAX_TARGETS)
                .toList();
    }

    private static ArTarget preferred(ArContext context, ArTarget first, ArTarget second) {
        if (first.priority() != second.priority())
            return first.priority() > second.priority() ? first : second;
        return first.worldPosition().distanceToSqr(context.viewerPosition())
                <= second.worldPosition().distanceToSqr(context.viewerPosition()) ? first : second;
    }
}
