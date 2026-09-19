// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.hud.ar;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Minimal per-frame client context shared by target providers. */
public record ArContext(ResourceKey<Level> dimension, Vec3 viewerPosition,
                        boolean evaNavigation) {
    public ArContext(ResourceKey<Level> dimension, Vec3 viewerPosition) {
        this(dimension, viewerPosition, false);
    }

    public ArContext {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(viewerPosition, "viewerPosition");
    }
}
