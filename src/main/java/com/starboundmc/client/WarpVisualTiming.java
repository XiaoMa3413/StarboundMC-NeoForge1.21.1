package com.starboundmc.client;

/** Shared phase thresholds for the star map and in-world warp renderers. */
public final class WarpVisualTiming
{
    public static final float ARRIVAL_FADE_START = 0.72F;
    /** Source-system handoff begins before the destination is fully visible. */
    public static final float SOURCE_SYSTEM_FADE_START = 0.60F;
    public static final float SOURCE_SYSTEM_FADE_END = 0.80F;
    public static final float TARGET_SYSTEM_FADE_START = 0.66F;
    public static final float TARGET_SYSTEM_FADE_END = 0.84F;

    private WarpVisualTiming()
    {
    }
}
