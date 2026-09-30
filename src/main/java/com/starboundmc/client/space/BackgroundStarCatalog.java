package com.starboundmc.client.space;

import java.util.Random;

/** Seeded, world-oriented directions; generated once per GPU resource budget. */
public final class BackgroundStarCatalog {
    public static final long SEED = 0x5B4D434CL;
    private BackgroundStarCatalog() {}

    public record Star(float x, float y, float z, float size, float phase,
                       float red, float green, float blue, float brightness) {}

    public static Star[] generate(int count) {
        if (count <= 0 || count > 32000) throw new IllegalArgumentException("Star budget must be 1..32000");
        Random random = new Random(SEED);
        Star[] stars = new Star[count];
        double nLength = Math.sqrt(.24 * .24 + .87 * .87 + .43 * .43);
        double nx = .24 / nLength, ny = .87 / nLength, nz = .43 / nLength;
        double tLength = Math.hypot(nx, nz);
        double tx = nz / tLength, tz = -nx / tLength;
        double bx = ny * tz, by = nz * tx - nx * tz, bz = -ny * tx;
        for (int i = 0; i < count; i++) {
            double x, y, z;
            if (random.nextFloat() < .35F) {
                double angle = random.nextDouble() * Math.PI * 2;
                double latitude = random.nextGaussian() * .10;
                x = tx * Math.cos(angle) + bx * Math.sin(angle) + nx * latitude;
                y = by * Math.sin(angle) + ny * latitude;
                z = tz * Math.cos(angle) + bz * Math.sin(angle) + nz * latitude;
            } else {
                y = random.nextDouble() * 2 - 1;
                double radial = Math.sqrt(1 - y * y);
                double angle = random.nextDouble() * Math.PI * 2;
                x = radial * Math.cos(angle); z = radial * Math.sin(angle);
            }
            double length = Math.sqrt(x * x + y * y + z * z);
            float magnitude = random.nextFloat();
            float brightness = .22F + .78F * (float) Math.pow(magnitude, 3);
            float size = .07F + brightness * .17F;
            if (magnitude > .995F) size *= 1.7F;
            float color = random.nextFloat();
            float r = color < .18F ? 1 : color > .80F ? .68F : .92F;
            float g = color < .18F ? .76F : color > .80F ? .82F : .94F;
            float b = color < .18F ? .55F : 1;
            stars[i] = new Star((float) (x / length), (float) (y / length), (float) (z / length),
                    size, random.nextFloat() * (float) (Math.PI * 2), r, g, b, brightness);
        }
        return stars;
    }
}
