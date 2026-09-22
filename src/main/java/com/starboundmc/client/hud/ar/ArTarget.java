// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Immutable renderer-facing description of one known world-space target. */
public record ArTarget(ResourceLocation id, ArTargetCategory category, Vec3 worldPosition,
                       Component label, ArGuidanceMode guidance, int priority,
                       double maxDistance, int rgb, String identificationKey) {
    /** Dynamic label arguments (distance, altitude) do not identify a new object. */
    public ArTarget(ResourceLocation id, ArTargetCategory category, Vec3 worldPosition,
                    Component label, ArGuidanceMode guidance, int priority, double maxDistance, int rgb) {
        this(id, category, worldPosition, label, guidance, priority, maxDistance, rgb, id.toString());
    }

    public ArTarget {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(worldPosition, "worldPosition");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(guidance, "guidance");
        Objects.requireNonNull(identificationKey, "identificationKey");
        if (!(maxDistance > 0) || Double.isNaN(maxDistance))
            throw new IllegalArgumentException("maxDistance must be positive");
        rgb &= 0xFFFFFF;
    }
}
