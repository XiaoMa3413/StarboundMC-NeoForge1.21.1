package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;

/** Shared LDLib2 animation timings for the starmap's restrained transitions. */
final class StarmapUiAnimations {
    static final float LEVEL_DURATION = 0.18F;
    static final float SELECTION_DURATION = 0.14F;
    static final float PANEL_DURATION = 0.16F;

    private static final ISubscription NONE = () -> {};

    private StarmapUiAnimations() {}

    static ISubscription fade(UIElement target, float from, float to,
                              float duration, Runnable onFinished) {
        if (target.getModularUI() == null) {
            target.style(style -> style.opacity(to));
            if (onFinished != null)
                onFinished.run();
            return NONE;
        }
        return target.animation()
                .duration(duration)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.OPACITY,
                        FloatObjectPair.of(0.0F, from),
                        FloatObjectPair.of(1.0F, to))
                .onFinished(element -> {
                    if (onFinished != null)
                        onFinished.run();
                })
                .start();
    }

    static ISubscription tint(UIElement target, int from, int to,
                              float duration, Runnable onFinished) {
        if (target.getModularUI() == null) {
            target.style(style -> style.color(to));
            if (onFinished != null)
                onFinished.run();
            return NONE;
        }
        return target.animation()
                .duration(duration)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.COLOR,
                        FloatObjectPair.of(0.0F, from),
                        FloatObjectPair.of(1.0F, to))
                .onFinished(element -> {
                    if (onFinished != null)
                        onFinished.run();
                })
                .start();
    }

    static ISubscription none() {
        return NONE;
    }
}
