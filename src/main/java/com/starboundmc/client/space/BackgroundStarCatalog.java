package com.starboundmc.client.space;

import java.util.Random;

/** Seeded, world-oriented directions; generated once per GPU resource budget. */
public final class BackgroundStarCatalog {
    public static final long SEED = 0x5B4D434CL;
    private BackgroundStarCatalog() {}

    /** Colour is linear RGB; flux is integrated luminance, sigma is in screen pixels. */
    public record Star(float x, float y, float z, float sigmaPixels,
                       float red, float green, float blue, float flux, float dustFraction) {}

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
                // Wider inner disk and uneven star clouds, aligned with the direction background.
                double inner = Math.pow(Math.max(Math.cos(angle),0),5);
                double latitude = random.nextGaussian() * (.055 + .045*inner);
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
            // Higher budgets add predominantly faint stars instead of multiplying bright highlights.
            double magnitude = 1.2 + 7.8*Math.pow(random.nextDouble(),.35)
                    + .65*Math.max(0,Math.log((i+1)/2000.0)/Math.log(2));
            float flux = StarPhotometry.flux(magnitude);
            float population = random.nextFloat();
            double kelvin = population < .18 ? 3500+population/.18*1500
                    : population < .85 ? 5000+(population-.18)/.67*2500
                    : 7500+(population-.85)/.15*7500;
            var color = StarPhotometry.color(kelvin);
            // Foreground stars remain visible in front of the same dust that masks distant populations.
            float dustFraction = random.nextFloat() < .24 ? 0 : .65F+random.nextFloat()*.35F;
            stars[i] = new Star((float) (x / length), (float) (y / length), (float) (z / length),
                    .42F+.06F*(float)Math.sqrt(Math.min(flux/8,1)),
                    color.red(),color.green(),color.blue(),flux,dustFraction);
        }
        return stars;
    }
}
