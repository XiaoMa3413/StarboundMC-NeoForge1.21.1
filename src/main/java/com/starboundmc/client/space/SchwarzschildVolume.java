package com.starboundmc.client.space;

import java.util.ArrayList;
import java.util.List;

/** Reference for a finite-height research medium, not a hydrodynamic solution.
 * Radius is Schwarzschild areal radius. Height profile is compact in z/r; local
 * grey extinction is normalized to a normal column independent of thickness. */
public final class SchwarzschildVolume {
    public static final double ASPECT=.016, COLUMN=.12, PROFILE_NORM=1.7176132634088521;
    public record Interval(double start,double end) {}
    private SchwarzschildVolume() {}

    public static double profile(double height) {
        double p=Math.max(0,Math.exp(-height*height/2)-Math.exp(-4.5));
        return p*p/PROFILE_NORM;
    }
    public static List<Interval> intervals(SchwarzschildPath path,double a,double b,double aspect) {
        if(!Double.isFinite(a) || !Double.isFinite(b) || Math.hypot(a,b)>1+1e-8
                || !Double.isFinite(aspect) || aspect<=0 || aspect>=.2)
            throw new IllegalArgumentException("Invalid volume orientation/thickness");
        var ranges=path.shellRanges(SchwarzschildDisc.INNER_RADIUS,SchwarzschildDisc.OUTER_RADIUS);
        var result=new ArrayList<Interval>();double amplitude=Math.hypot(a,b),support=3*aspect;
        double first=amplitude<1e-12?0:Math.atan2(-a,b)%Math.PI;
        if(first<0)first+=Math.PI;
        for(int half=0;half<2;half++) {
            double lo=ranges[half*2],hi=ranges[half*2+1];if(hi-lo<1e-12)continue;
            if(amplitude<=support) {result.add(new Interval(lo,hi));continue;}
            double width=Math.asin(support/amplitude);
            int k=(int)Math.ceil((lo-width-first)/Math.PI);
            for(double center=first+k*Math.PI;center-width<hi;center+=Math.PI) {
                double start=Math.max(lo,center-width),end=Math.min(hi,center+width);
                if(end>start)result.add(new Interval(start,end));
            }
        }
        return List.copyOf(result);
    }
    /** Proper spatial length measured by a static local observer per dphi.
     * Follows from the null-ray energy identity and the Schwarzschild metric. */
    public static double lengthRate(SchwarzschildPath path,SchwarzschildPath.Sample s) {
        double u=s.inverseRadius();return 1/(path.impact()*u*u*Math.sqrt(1-u));
    }
    /** Homogeneous grey column, integrated on independent timed ODE trajectories.
     * The physical mode additionally transforms local length into the orbiting
     * medium's rest frame; its off-midplane circular flow is a model closure. */
    public static double opticalDepth(SchwarzschildPath path,double a,double b,double orbitNormal,
            double aspect,double column,boolean physical,int steps) {
        if(column<0 || !Double.isFinite(column) || steps<2)throw new IllegalArgumentException("Invalid quadrature");
        double tau=0;
        for(var range:intervals(path,a,b,aspect)) {
            double delta=(range.end-range.start)/steps;
            for(int i=0;i<steps;i++) {
                double phi=range.start+(i+.5)*delta;var s=path.sample(phi);
                double m=a*Math.cos(phi)+b*Math.sin(phi),u=s.inverseRadius();
                double comoving=1;
                if(physical) {
                    double v=1/Math.sqrt(2*(s.radius()-1));
                    double mu=Math.clamp(-path.impact()*u*Math.sqrt(1-u)*orbitNormal/Math.sqrt(1-m*m),-1,1);
                    comoving=(1-v*mu)/Math.sqrt(1-v*v);
                }
                tau+=column*profile(m/aspect)/(aspect*s.radius())*lengthRate(path,s)*comoving*delta;
            }
        }
        return tau;
    }
}
