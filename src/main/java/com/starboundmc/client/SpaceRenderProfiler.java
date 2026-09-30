package com.starboundmc.client;

import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

import java.util.Arrays;
import java.util.Locale;

/** Opt-in asynchronous timestamp pairs; never waits for an unfinished GPU query. */
final class SpaceRenderProfiler {
    enum Pass { TOTAL, BACKGROUND, STARFIELD, SYSTEM_STARS, CORONA, PLANETS, WARP, COMPOSITE }
    private static final boolean ENABLED = Boolean.getBoolean("starboundmc.debug.spaceSmoke")
            || Boolean.getBoolean("starboundmc.debug.spaceProfile");
    private static final int CAPACITY = 256;
    private static final Timer[] TIMERS = new Timer[Pass.values().length];
    private static boolean initialized, gpuSupported;
    private static long lastLog;

    private SpaceRenderProfiler() {}

    static void begin(Pass pass) {
        if (!ENABLED) return;
        if (!initialized) {
            gpuSupported = GL.getCapabilities().OpenGL33 || GL.getCapabilities().GL_ARB_timer_query;
            for (int i = 0; i < TIMERS.length; i++) TIMERS[i] = new Timer();
            initialized = true;
        }
        TIMERS[pass.ordinal()].begin();
    }

    static void end(Pass pass) {
        if (!ENABLED || !initialized) return;
        TIMERS[pass.ordinal()].end();
        if (pass == Pass.TOTAL && !Boolean.getBoolean("starboundmc.debug.spaceSmoke")) {
            long now = System.nanoTime();
            if (now - lastLog > 10_000_000_000L) {
                lastLog = now;
                com.mojang.logging.LogUtils.getLogger().info("Space render profile\n{}", report());
            }
        }
    }

    static void resetSamples() {
        if (!initialized) return;
        for (Timer timer : TIMERS) {
            timer.cpuCount = timer.gpuCount = 0;
            for (Query query : timer.queries) query.discard = true;
        }
    }

    static String report() {
        if (!ENABLED || !initialized) return "Space profiler disabled";
        StringBuilder result = new StringBuilder("renderer=").append(GL11.glGetString(GL11.GL_RENDERER))
                .append("\nversion=").append(GL11.glGetString(GL11.GL_VERSION))
                .append("\ngpuTimestamps=").append(gpuSupported).append('\n');
        for (Pass pass : Pass.values()) {
            Timer timer = TIMERS[pass.ordinal()];
            timer.collect();
            result.append(pass).append(" cpu ").append(summary(timer.cpu, timer.cpuCount))
                    .append(" gpu ").append(summary(timer.gpu, timer.gpuCount)).append('\n');
        }
        return result.toString();
    }

    static void release() {
        if (!initialized) return;
        for (Timer timer : TIMERS) for (Query query : timer.queries) {
            if (query.start != 0) GL15.glDeleteQueries(query.start);
            if (query.end != 0) GL15.glDeleteQueries(query.end);
        }
        initialized = false;
    }

    private static String summary(double[] values, int count) {
        int n = Math.min(count, CAPACITY);
        if (n == 0) return "samples=0";
        double[] sorted = Arrays.copyOf(values, n);
        Arrays.sort(sorted);
        return String.format(Locale.ROOT, "samples=%d p50_ms=%.4f p95_ms=%.4f", n,
                sorted[n / 2], sorted[Math.min(n - 1, (int) (n * .95))]);
    }

    private static final class Query {
        int start, end;
        boolean pending, discard;
    }

    private static final class Timer {
        final Query[] queries = { new Query(), new Query(), new Query(), new Query() };
        final double[] cpu = new double[CAPACITY], gpu = new double[CAPACITY];
        int cpuCount, gpuCount;
        long cpuStart;
        Query active;

        void begin() {
            collect();
            active = null;
            if (gpuSupported) for (Query query : queries) if (!query.pending) {
                if (query.start == 0) {
                    query.start = GL15.glGenQueries();
                    query.end = GL15.glGenQueries();
                }
                query.discard = false;
                ARBTimerQuery.glQueryCounter(query.start, ARBTimerQuery.GL_TIMESTAMP);
                active = query;
                break;
            }
            cpuStart = System.nanoTime();
        }

        void end() {
            cpu[cpuCount++ % CAPACITY] = (System.nanoTime() - cpuStart) / 1e6;
            if (active != null) {
                ARBTimerQuery.glQueryCounter(active.end, ARBTimerQuery.GL_TIMESTAMP);
                active.pending = true;
                active = null;
            }
        }

        void collect() {
            for (Query query : queries) if (query.pending
                    && GL15.glGetQueryObjecti(query.end, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
                if (!query.discard) {
                    long start = ARBTimerQuery.glGetQueryObjectui64(query.start, GL15.GL_QUERY_RESULT);
                    long end = ARBTimerQuery.glGetQueryObjectui64(query.end, GL15.GL_QUERY_RESULT);
                    gpu[gpuCount++ % CAPACITY] = (end - start) / 1e6;
                }
                query.pending = false;
            }
        }
    }
}
