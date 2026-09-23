package com.starboundmc.client;

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Parity check between the legacy CPU atmosphere glow and the shader port.
 *
 * <p>The shader reproduces the CPU bake's limb ramp per fragment, so the two
 * must agree exactly for the same geometry. Getting a reference frame wrong
 * here does not fail to compile — it silently moves every shell point outside
 * the ramp and the glow renders pure black — which is precisely the failure
 * this test exists to catch. Both computations below are transcribed from the
 * respective sources; the shader side must be updated whenever the GLSL
 * changes.</p>
 */
final class AtmosphereLimbParityTest {
    private static final float SHELL_FACTOR = 1.15F;
    private static final float PLANET_RADIUS = 50.0F;

    @Test
    void shaderLimbRampMatchesTheCpuBake() {
        Random random = new Random(20260923L);
        for (int trial = 0; trial < 200; trial++) {
            // Camera at the origin; the body centre somewhere in front of it.
            float distance = 60.0F + random.nextFloat() * 260.0F;
            float centerX = (random.nextFloat() - 0.5F) * 40.0F;
            float centerY = (random.nextFloat() - 0.5F) * 40.0F;
            float centerZ = -distance;
            float scale = 0.05F + random.nextFloat() * 1.2F;
            float peak = 0.05F + random.nextFloat() * 0.25F;
            float alpha = random.nextFloat();

            float planetRadius = PLANET_RADIUS * scale;
            float shellRadius = planetRadius * SHELL_FACTOR;

            // A shell point on the shell sphere, seen from the camera.
            double u = random.nextDouble() * Math.PI * 2.0;
            double v = random.nextDouble() * Math.PI;
            float sx = centerX + shellRadius * (float) (Math.sin(v) * Math.cos(u));
            float sy = centerY + shellRadius * (float) (Math.cos(v));
            float sz = centerZ + shellRadius * (float) (Math.sin(v) * Math.sin(u));

            float cpu = cpuAlpha(centerX, centerY, centerZ, planetRadius, shellRadius,
                    sx, sy, sz, peak, alpha);
            float shader = shaderAlpha(centerX, centerY, centerZ, planetRadius, shellRadius,
                    sx, sy, sz, peak * alpha);
            assertEquals(cpu, shader, 1.0e-4F, "limb ramp diverged at trial " + trial);
        }
    }

    /** Transcribed from PlanetRenderer's legacy renderAtmosphereGlow. */
    private static float cpuAlpha(float cx, float cy, float cz, float planetRadius,
                                  float shellRadius, float sx, float sy, float sz,
                                  float peak, float alpha) {
        float distC = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float axisX = cx / distC;
        float axisY = cy / distC;
        float axisZ = cz / distC;

        float limbAngle = (float) Math.asin(Math.min(1.0, planetRadius / distC));
        float outerAngle = (float) Math.asin(Math.min(1.0, shellRadius / distC));
        float angleRange = Math.max(0.0001F, outerAngle - limbAngle);
        float innerAngle = Math.max(0.0F, limbAngle - 0.05F);
        float innerRange = Math.max(0.0001F, limbAngle - innerAngle);

        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        float dot = axisX * sx / len + axisY * sy / len + axisZ * sz / len;
        dot = Math.max(-1.0F, Math.min(1.0F, dot));
        float angle = (float) Math.acos(dot);

        if (angle >= innerAngle && angle <= limbAngle) {
            float t = (angle - innerAngle) / innerRange;
            return peak * smoothstep(t) * alpha;
        }
        if (angle > limbAngle && angle <= outerAngle) {
            float t = (angle - limbAngle) / angleRange;
            return peak * (float) Math.pow(1.0F - t, 1.5) * alpha;
        }
        return 0.0F;
    }

    /** Transcribed from atmosphere.fsh with the sun-facing factor at full. */
    private static float shaderAlpha(float cx, float cy, float cz, float planetRadius,
                                     float shellRadius, float sx, float sy, float sz,
                                     float density) {
        float d = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float toCenterX = cx / Math.max(d, 0.0001F);
        float toCenterY = cy / Math.max(d, 0.0001F);
        float toCenterZ = cz / Math.max(d, 0.0001F);

        // The camera is the view-space origin, so the shell position itself is
        // the direction from the camera.
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        float dot = toCenterX * sx / len + toCenterY * sy / len + toCenterZ * sz / len;
        dot = Math.max(-1.0F, Math.min(1.0F, dot));
        float angle = (float) Math.acos(dot);

        float limbAngle = (float) Math.asin(clamp(planetRadius / Math.max(d, 0.0001F), 0.0F, 1.0F));
        float outerAngle = (float) Math.asin(clamp(shellRadius / Math.max(d, 0.0001F), 0.0F, 1.0F));
        float innerAngle = Math.max(0.0F, limbAngle - 0.05F);

        float limbFactor = 0.0F;
        if (angle >= innerAngle && angle <= limbAngle) {
            float t = clamp((angle - innerAngle) / Math.max(limbAngle - innerAngle, 0.0001F), 0.0F, 1.0F);
            limbFactor = t * t * (3.0F - 2.0F * t);
        } else if (angle > limbAngle && angle <= outerAngle) {
            float t = clamp((angle - limbAngle) / Math.max(outerAngle - limbAngle, 0.0001F), 0.0F, 1.0F);
            limbFactor = (float) Math.pow(1.0F - t, 1.5);
        }
        return density * limbFactor;
    }

    private static float smoothstep(float t) {
        t = clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
