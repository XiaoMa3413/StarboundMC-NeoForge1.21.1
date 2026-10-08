package com.starboundmc.client.space;

/** Unit-radius optical columns, independent of atmosphere colour and exposure. */
public final class AtmosphereSolarOptics {
    public static final int WIDTH = 256, HEIGHT = 128;
    public static final double TOP = 1.08, RAYLEIGH_HEIGHT = .0016, MIE_HEIGHT = .00032;
    private AtmosphereSolarOptics() {}

    public static float[] generate() {
        float[] columns = new float[WIDTH * HEIGHT * 2];
        for (int y = 0; y < HEIGHT; y++) {
            double v = y / (double) (HEIGHT - 1);
            double radius = 1 + (TOP - 1) * v * v;
            double horizon = -Math.sqrt(Math.max(0, 1 - 1 / (radius * radius)));
            for (int x = 0; x < WIDTH; x++) {
                double u = x / (double) (WIDTH - 1);
                double mu = horizon + (1 - horizon) * u * u;
                double[] column = column(radius, mu);
                int index = 2 * (y * WIDTH + x);
                columns[index] = (float) column[0];
                columns[index + 1] = (float) column[1];
            }
        }
        return columns;
    }

    public static double[] column(double radius, double mu) {
        if (!Double.isFinite(radius) || radius < 1 || radius > TOP || !Double.isFinite(mu) || mu < -1 || mu > 1)
            throw new IllegalArgumentException("Optical column requires a finite radius in [1,top] and cosine in [-1,1]");
        double horizon = -Math.sqrt(Math.max(0, 1 - 1 / (radius * radius)));
        if (mu < horizon) throw new IllegalArgumentException("Solar path intersects ground");
        double b = radius * mu;
        double end = Math.max(0, -b + Math.sqrt(Math.max(0, b * b + TOP * TOP - radius * radius)));
        double closest = Math.clamp(-b, 0, end);
        double rayleigh = 0, mie = 0;
        for (int i = 0; i < 256; i++) {
            double a = sample(i / 256.0, end, closest), c = sample((i + 1) / 256.0, end, closest);
            double t = (a + c) * .5;
            double altitude = Math.max(0, Math.sqrt(Math.max(1, radius * radius + 2 * b * t + t * t)) - 1);
            rayleigh += Math.exp(-altitude / RAYLEIGH_HEIGHT) * (c - a);
            mie += Math.exp(-altitude / MIE_HEIGHT) * (c - a);
        }
        return new double[] {rayleigh, mie};
    }

    private static double sample(double u, double end, double closest) {
        if (closest <= 0) return end * u * u;
        if (closest >= end) return end * (1 - (1 - u) * (1 - u));
        return u < .5 ? closest * (1 - (1 - 2 * u) * (1 - 2 * u))
                : closest + (end - closest) * (2 * u - 1) * (2 * u - 1);
    }
}
