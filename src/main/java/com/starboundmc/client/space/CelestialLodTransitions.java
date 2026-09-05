package com.starboundmc.client.space;

import java.util.Arrays;

/** Allocation-free temporal blend state for non-stellar bodies. */
public final class CelestialLodTransitions
{
    /** Half a second per adjacent tier at the normal 20 TPS game rate. */
    public static final float TICKS_PER_LEVEL = 10.0F;
    private static final float STALE_TICKS = 40.0F;

    private final String[] bodyIds;
    private final float[] detail;
    private final float[] lastTick;

    public CelestialLodTransitions(int capacity)
    {
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity must be positive");
        bodyIds = new String[capacity];
        detail = new float[capacity];
        lastTick = new float[capacity];
    }

    /** Advances a body's blend toward the requested LOD and returns [0,3]. */
    public float update(String bodyId, CelestialLod target, float animationTick)
    {
        if (bodyId == null || bodyId.isBlank())
            throw new IllegalArgumentException("bodyId must not be blank");
        int slot = find(bodyId);
        float targetDetail = detailOf(target == null ? CelestialLod.CULLED : target);
        if (slot < 0)
        {
            slot = replacementSlot();
            bodyIds[slot] = bodyId;
            detail[slot] = targetDetail;
            lastTick[slot] = animationTick;
            return targetDetail;
        }

        float elapsed = animationTick - lastTick[slot];
        if (!Float.isFinite(elapsed) || elapsed < 0.0F || elapsed > STALE_TICKS)
            detail[slot] = targetDetail;
        else if (elapsed > 0.0F)
            detail[slot] = moveTowards(detail[slot], targetDetail, elapsed / TICKS_PER_LEVEL);
        lastTick[slot] = animationTick;
        return detail[slot];
    }

    /** Returns the last settled/transitioning level for a body, or CULLED. */
    public CelestialLod currentLod(String bodyId)
    {
        int slot = find(bodyId);
        if (slot < 0)
            return CelestialLod.CULLED;
        return lodOf(detail[slot]);
    }

    public void reset()
    {
        Arrays.fill(bodyIds, null);
        Arrays.fill(detail, 0.0F);
        Arrays.fill(lastTick, 0.0F);
    }

    public static float pointWeight(float detail)
    {
        if (detail <= 0.0F || detail >= 2.0F)
            return 0.0F;
        return detail <= 1.0F ? smoothstep(detail) : 1.0F - smoothstep(detail - 1.0F);
    }

    public static float reducedWeight(float detail)
    {
        if (detail <= 1.0F || detail >= 3.0F)
            return 0.0F;
        return detail <= 2.0F ? smoothstep(detail - 1.0F) : 1.0F - smoothstep(detail - 2.0F);
    }

    public static float fullWeight(float detail)
    {
        if (detail <= 2.0F)
            return 0.0F;
        return detail >= 3.0F ? 1.0F : smoothstep(detail - 2.0F);
    }

    private int find(String bodyId)
    {
        if (bodyId == null)
            return -1;
        for (int i = 0; i < bodyIds.length; i++)
            if (bodyId.equals(bodyIds[i]))
                return i;
        return -1;
    }

    private int replacementSlot()
    {
        int oldest = 0;
        for (int i = 0; i < bodyIds.length; i++)
        {
            if (bodyIds[i] == null)
                return i;
            if (lastTick[i] < lastTick[oldest])
                oldest = i;
        }
        return oldest;
    }

    private static float detailOf(CelestialLod lod)
    {
        return switch (lod)
        {
            case CULLED -> 0.0F;
            case POINT -> 1.0F;
            case REDUCED -> 2.0F;
            case FULL -> 3.0F;
        };
    }

    private static CelestialLod lodOf(float value)
    {
        if (value >= 2.5F)
            return CelestialLod.FULL;
        if (value >= 1.5F)
            return CelestialLod.REDUCED;
        if (value >= 0.5F)
            return CelestialLod.POINT;
        return CelestialLod.CULLED;
    }

    private static float moveTowards(float current, float target, float amount)
    {
        if (current < target)
            return Math.min(current + amount, target);
        return Math.max(current - amount, target);
    }

    private static float smoothstep(float value)
    {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }
}
