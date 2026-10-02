package com.starboundmc.client.space;

import java.util.ArrayList;
import java.util.List;

/** Finite, timed reference trajectory. Time is positive look-back Schwarzschild coordinate time,
 * in rs/c units. Stop before the horizon/infinity singularities; only exterior disc hits use it. */
public final class SchwarzschildPath {
    public static final double NEAR_RADIUS = 1.05, FAR_RADIUS = 64;
    public record Sample(double azimuth, double inverseRadius, double slope, double delay) {
        public double radius() { return 1 / inverseRadius; }
    }
    private final double impact;
    private final SchwarzschildRay.Fate fate;
    private final List<Sample> samples;
    private final double maximumEnergyError;

    public SchwarzschildPath(double radius, double angle) { this(radius, angle, 1e-11); }

    public SchwarzschildPath(double radius, double angle, double tolerance) {
        impact = SchwarzschildRay.impact(radius, angle);
        if (radius >= FAR_RADIUS || impact < 1e-8 || !Double.isFinite(tolerance)
                || tolerance < 1e-14 || tolerance > 1e-5)
            throw new IllegalArgumentException("Finite path requires 1.5 < r < 64, nonradial ray and valid tolerance");
        var nodes = new ArrayList<Sample>();
        double[] state = {1 / radius, Math.cos(angle) / impact, 0};
        double phi = 0, h = Math.min(.02, .02 * impact), errorEnergy = 0;
        nodes.add(new Sample(0, state[0], state[1], 0));
        SchwarzschildRay.Fate end = SchwarzschildRay.Fate.UNRESOLVED;
        for (int steps = 0; steps < 200000 && phi < 64 * Math.PI; ) {
            h = Math.min(h, .15 * Math.min(state[0], 1 - state[0]) / Math.max(Math.abs(state[1]), 1e-8));
            double[] full = step(state, h), half = step(step(state, h / 2), h / 2);
            double error = 0;
            for (int k = 0; k < 3; k++)
                error = Math.max(error, Math.abs(half[k] - full[k]) / (15 * Math.max(1, Math.abs(half[k]))));
            if (!Double.isFinite(error)) throw new IllegalStateException("Nonfinite timed trajectory");
            if (error > tolerance) { h *= Math.max(.2, .9 * Math.pow(tolerance / error, .2)); continue; }
            steps++;
            for (int k = 0; k < 3; k++) half[k] += (half[k] - full[k]) / 15;
            if (half[0] <= 1 / FAR_RADIUS || half[0] >= 1 / NEAR_RADIUS) {
                double boundary = half[0] <= 1 / FAR_RADIUS ? 1 / FAR_RADIUS : 1 / NEAR_RADIUS;
                double lo = 0, hi = h;
                for (int j = 0; j < 40; j++) {
                    double mid = (lo + hi) / 2, u = step(state, mid)[0];
                    if ((boundary < state[0] && u > boundary) || (boundary > state[0] && u < boundary)) lo = mid;
                    else hi = mid;
                }
                h = (lo + hi) / 2;
                half = step(state, h);
                end = boundary == 1 / FAR_RADIUS ? SchwarzschildRay.Fate.ESCAPED : SchwarzschildRay.Fate.CAPTURED;
            }
            phi += h;
            state = half;
            double energy = state[1] * state[1] + state[0] * state[0] * (1 - state[0]);
            errorEnergy = Math.max(errorEnergy, Math.abs(energy * impact * impact - 1));
            nodes.add(new Sample(phi, state[0], state[1], state[2]));
            if (end != SchwarzschildRay.Fate.UNRESOLVED) break;
            h = Math.min(.03, h * Math.clamp(error == 0 ? 2 : .9 * Math.pow(tolerance / error, .2), .5, 2));
        }
        samples = List.copyOf(nodes);
        fate = end;
        maximumEnergyError = errorEnergy;
    }

    private double[] step(double[] s, double h) {
        double[] k1 = derivative(s), t = new double[3];
        for (int k = 0; k < 3; k++) t[k] = s[k] + h * k1[k] / 2;
        double[] k2 = derivative(t);
        for (int k = 0; k < 3; k++) t[k] = s[k] + h * k2[k] / 2;
        double[] k3 = derivative(t);
        for (int k = 0; k < 3; k++) t[k] = s[k] + h * k3[k];
        double[] k4 = derivative(t);
        for (int k = 0; k < 3; k++) t[k] = s[k] + h * (k1[k] + 2*k2[k] + 2*k3[k] + k4[k]) / 6;
        return t;
    }

    private double[] derivative(double[] s) {
        double u = s[0];
        return new double[]{s[1], -u + 1.5*u*u, timeDerivative(impact, u)};
    }

    public static double timeDerivative(double impact, double u) { return 1 / (impact * u * u * (1-u)); }

    public Sample sample(double phi) {
        if (!Double.isFinite(phi) || phi < 0 || phi > endAzimuth() + 1e-12)
            throw new IllegalArgumentException("Outside finite trajectory");
        int lo = 0, hi = samples.size()-1;
        while (hi-lo > 1) { int mid=(lo+hi)/2; if(samples.get(mid).azimuth() < phi)lo=mid;else hi=mid; }
        var a = samples.get(lo); var b = samples.get(hi);
        double h = b.azimuth()-a.azimuth(), t = Math.clamp((phi-a.azimuth())/h,0,1);
        double u = hermite(a.inverseRadius(),b.inverseRadius(),h*a.slope(),h*b.slope(),t);
        double delay = hermite(a.delay(),b.delay(),h*timeDerivative(impact,a.inverseRadius()),
                h*timeDerivative(impact,b.inverseRadius()),t);
        double slope = ((6*t*t-6*t)*a.inverseRadius()+(-6*t*t+6*t)*b.inverseRadius())/h
                +(3*t*t-4*t+1)*a.slope()+(3*t*t-2*t)*b.slope();
        return new Sample(phi,u,slope,delay);
    }

    public static double hermite(double a,double b,double da,double db,double t) {
        return (2*t*t*t-3*t*t+1)*a+(-2*t*t*t+3*t*t)*b+(t*t*t-2*t*t+t)*da+(t*t*t-t*t)*db;
    }
    public double endAzimuth() { return samples.getLast().azimuth(); }
    public double impact() { return impact; }
    public double maximumEnergyError() { return maximumEnergyError; }
    public SchwarzschildRay.Fate fate() { return fate; }
}
