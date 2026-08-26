package com.starboundmc.client.starmap;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Transform2D;
import com.lowdragmc.lowdraglib2.gui.ui.style.PropertyRegistry;
import com.lowdragmc.lowdraglib2.math.interpolate.Eases;
import com.lowdragmc.lowdraglib2.math.interpolate.IEase;
import com.lowdragmc.lowdraglib2.syncdata.ISubscription;
import it.unimi.dsi.fastutil.floats.FloatObjectPair;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared LDLib2 animation timings for the starmap's restrained transitions. */
final class StarmapUiAnimations {
    static final float LEVEL_DURATION = 0.18F;
    static final float SELECTION_DURATION = 0.14F;
    static final float PANEL_EXPAND_DURATION = 0.11F;
    static final float PANEL_CONTENT_DURATION = 0.05F;
    static final float PANEL_COLLAPSE_DURATION = 0.08F;

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

    /** Tints independent elements in one timeline so draw order cannot read as a scan. */
    static ISubscription tintTogether(List<? extends UIElement> targets, int from, int to,
                                      float duration, Runnable onFinished) {
        if (targets.isEmpty()) {
            if (onFinished != null)
                onFinished.run();
            return NONE;
        }
        targets.forEach(target -> target.style(style -> style.color(from)));
        UIElement first = targets.getFirst();
        if (first.getModularUI() == null) {
            targets.forEach(target -> target.style(style -> style.color(to)));
            if (onFinished != null)
                onFinished.run();
            return NONE;
        }
        var animation = first.animation();
        for (int i = 1; i < targets.size(); i++)
            animation.select(targets.get(i));
        AtomicBoolean callbackDelivered = new AtomicBoolean();
        return animation.duration(duration)
                .ease(Eases.QUAD_OUT)
                .style(PropertyRegistry.COLOR,
                        FloatObjectPair.of(0.0F, from),
                        FloatObjectPair.of(1.0F, to))
                .onFinished(element -> {
                    if (onFinished != null && callbackDelivered.compareAndSet(false, true))
                        onFinished.run();
                })
                .start();
    }

    static ISubscription transform(UIElement target, Transform2D from, Transform2D to,
                                   float duration, IEase ease, Runnable onFinished) {
        if (target.getModularUI() == null) {
            target.style(style -> style.transform2D(to));
            if (onFinished != null)
                onFinished.run();
            return NONE;
        }
        return target.animation()
                .duration(duration)
                .ease(ease)
                .style(PropertyRegistry.TRANSFORM_2D,
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
