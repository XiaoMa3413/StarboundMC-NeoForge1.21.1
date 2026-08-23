package com.starboundmc.client.space;

import java.util.Arrays;

/** Bounded, allocation-free temporal crossfade state keyed by star-system id. */
final class StellarLodTransitions
{
    static final float TICKS_PER_LEVEL = 18.0F;
    private static final float STALE_TICKS = 40.0F;

    private final String[] systemIds;
    private final float[] detail;
    private final float[] lastTick;

    StellarLodTransitions(int capacity)
    {
        systemIds = new String[capacity];
        detail = new float[capacity];
        lastTick = new float[capacity];
    }

    float update(String systemId, StellarLod target, float animationTick)
    {
        int slot = find(systemId);
        float targetDetail = detailOf(target);
        if (slot < 0)
        {
            slot = replacementSlot();
            systemIds[slot] = systemId;
            detail[slot] = targetDetail;
            lastTick[slot] = animationTick;
            return targetDetail;
        }

        float elapsed = animationTick - lastTick[slot];
        if (!Float.isFinite(elapsed) || elapsed < 0.0F || elapsed > STALE_TICKS)
        {
            detail[slot] = targetDetail;
        }
        else if (elapsed > 0.0F)
        {
            float step = elapsed / TICKS_PER_LEVEL;
            detail[slot] = moveTowards(detail[slot], targetDetail, step);
        }
        lastTick[slot] = animationTick;
        return detail[slot];
    }

    void reset()
    {
        Arrays.fill(systemIds, null);
    }

    static float pointWeight(float detail)
    {
        return 1.0F - smoothstep(detail);
    }

    static float simplifiedWeight(float detail)
    {
        return detail <= 1.0F ? smoothstep(detail) : 1.0F - smoothstep(detail - 1.0F);
    }

    static float fullWeight(float detail)
    {
        return smoothstep(detail - 1.0F);
    }

    private int find(String systemId)
    {
        for (int i = 0; i < systemIds.length; i++)
            if (systemId.equals(systemIds[i]))
                return i;
        return -1;
    }

    private int replacementSlot()
    {
        int oldest = 0;
        for (int i = 0; i < systemIds.length; i++)
        {
            if (systemIds[i] == null)
                return i;
            if (lastTick[i] < lastTick[oldest])
                oldest = i;
        }
        return oldest;
    }

    private static float detailOf(StellarLod lod)
    {
        return switch (lod)
        {
            case POINT -> 0.0F;
            case SIMPLIFIED -> 1.0F;
            case FULL -> 2.0F;
        };
    }

    private static float moveTowards(float current, float target, float maximumDelta)
    {
        if (current < target)
            return Math.min(current + maximumDelta, target);
        return Math.max(current - maximumDelta, target);
    }

    private static float smoothstep(float value)
    {
        float t = Math.max(0.0F, Math.min(1.0F, value));
        return t * t * (3.0F - 2.0F * t);
    }
}
