// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.client.space;

import com.starboundmc.encounter.RelayGeometry;
import net.minecraft.world.phys.Vec3;

/** Pure placement and handoff policy for the relay's distant visual proxy. */
public final class RelayProxyView {
    public static final double START_DISTANCE_FACTOR = 8;
    public static final double LOCAL_READY_HOLD_TICKS = 8;
    public static final double FADE_OUT_TICKS = 16;
    public static final double FADE_IN_TICKS = 4;

    private RelayProxyView() { }

    public record Placement(Vec3 center, double scale, double desiredDistance) { }

    /** Eased remote-to-local travel; it slows down as it reaches the reserved anchor. */
    public static double approachDistanceFactor(double progress) {
        double remaining = 1 - Math.clamp(progress, 0, 1);
        return 1 + (START_DISTANCE_FACTOR - 1) * remaining * remaining;
    }

    /**
     * Keeps a remote proxy inside the configured block render distance while preserving
     * its angular size. Near the station, the returned placement becomes the real anchor
     * with scale 1, so the proxy and block structure share the same silhouette.
     */
    public static Placement placement(Vec3 camera, Vec3 target, double distanceFactor, int renderChunks) {
        if (!Double.isFinite(distanceFactor) || distanceFactor < 1) distanceFactor = 1;
        var desired = RelayGeometry.SHIP_CENTER.add(target.subtract(RelayGeometry.SHIP_CENTER).scale(distanceFactor));
        var offset = desired.subtract(camera);
        double distance = offset.length();
        if (distance < 1.0e-6) return new Placement(desired, 1, distance);
        double scale = Math.min(1, renderBudget(renderChunks) / distance);
        return new Placement(camera.add(offset.scale(scale)), scale, distance);
    }

    /** Leaves headroom for normal chunk fog at the selected render distance. */
    public static double renderBudget(int renderChunks) {
        return Math.max(24, Math.clamp(renderChunks, 2, 32) * 16.0 - 12);
    }

    /** Temporal blend used for both the local-structure handoff and chunk-unload recovery. */
    public static float advanceAlpha(float current, boolean localReady, double elapsedTicks) {
        float alpha = Math.clamp(current, 0, 1);
        if (!Double.isFinite(elapsedTicks) || elapsedTicks <= 0) return alpha;
        double duration = localReady ? FADE_OUT_TICKS : FADE_IN_TICKS;
        double target = localReady ? 0 : 1;
        double step = elapsedTicks / duration;
        return (float) (target < alpha ? Math.max(target, alpha - step) : Math.min(target, alpha + step));
    }
}
