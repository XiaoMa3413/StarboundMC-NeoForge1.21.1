package com.starboundmc.client.space;

import java.util.ArrayList;
import java.util.List;

/** Plane crossings are geometric events, including emission before a captured ray ends. */
public final class SchwarzschildDisc {
    public static final double INNER_RADIUS=4.63, OUTER_RADIUS=9.35;
    public record Hit(double azimuth,double radius,double delay,double slope,int face) {}
    private SchwarzschildDisc() {}

    /** A=dot(observer radial,disc normal), B=dot(ray tangent,disc normal). */
    public static List<Hit> intersections(SchwarzschildPath path,double a,double b,double inner,double outer) {
        if (!Double.isFinite(a) || !Double.isFinite(b) || !Double.isFinite(inner) || !Double.isFinite(outer)
                || inner<3 || outer<=inner || outer>=SchwarzschildPath.FAR_RADIUS)
            throw new IllegalArgumentException("Invalid stable-orbit thin disc");
        // A ray exactly in an infinitely thin disc has no isolated intersection.
        if (Math.hypot(a,b)<1e-12) return List.of();
        double phi=firstCrossing(a,b);var hits=new ArrayList<Hit>();
        for(;phi<path.endAzimuth();phi+=Math.PI) {
            var s=path.sample(phi);double r=s.radius();
            if(r>=inner && r<=outer)hits.add(new Hit(phi,r,s.delay(),s.slope(),
                    -a*Math.sin(phi)+b*Math.cos(phi)>0?1:-1));
        }
        return List.copyOf(hits);
    }

    public static double firstCrossing(double a,double b) {
        double phi=Math.atan2(-a,b)%Math.PI;
        return phi<=1e-12?phi+Math.PI:phi;
    }

    /** Circular Schwarzschild orbit: local v=1/sqrt(2r-2), angular velocity in rs/c units. */
    public static double angularVelocity(double r) { return 1/Math.sqrt(2*r*r*r); }
    public static double frequencyRatio(double observerRadius,double emitterRadius,double photonOrbitCosine) {
        double speed=1/Math.sqrt(2*(emitterRadius-1));
        return Math.sqrt((1-1/emitterRadius)/(1-1/observerRadius))*Math.sqrt(1-speed*speed)
                /(1-speed*photonOrbitCosine);
    }
}
