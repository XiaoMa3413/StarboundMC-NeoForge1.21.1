package com.starboundmc.client.space;

/** Render-only time; reduce integer ticks before converting shader phases to float. */
public final class SpaceRenderClock {
    private static final double CAPTURE_TICKS = captureTicks();
    private SpaceRenderClock() {}

    public static float animationTicks(long gameTicks, float partialTick) {
        return (float) (Double.isNaN(CAPTURE_TICKS) ? gameTicks + (double) partialTick : CAPTURE_TICKS);
    }

    public static float twinklePhase(long gameTicks, float partialTick) {
        if (!Double.isNaN(CAPTURE_TICKS))
            return periodicPhase((long) CAPTURE_TICKS, (float) (CAPTURE_TICKS % 1), 2400);
        return periodicPhase(gameTicks, partialTick, 2400);
    }

    public static float periodicPhase(long ticks, float partialTick, int periodTicks) {
        if (periodTicks <= 0 || !Float.isFinite(partialTick))
            throw new IllegalArgumentException("Finite time and a positive period are required");
        double localTicks = Math.floorMod(ticks, periodTicks) + (double) partialTick;
        return (float) (localTicks / periodTicks * Math.PI * 2);
    }

    private static double captureTicks() {
        String value = System.getProperty("starboundmc.debug.spaceTimeTicks");
        if (value == null) return Double.NaN;
        double ticks = Double.parseDouble(value);
        if (!Double.isFinite(ticks) || ticks < 0)
            throw new IllegalArgumentException("spaceTimeTicks must be finite and non-negative");
        return ticks;
    }
}
