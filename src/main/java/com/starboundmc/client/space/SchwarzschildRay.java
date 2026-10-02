package com.starboundmc.client.space;

/**
 * Double precision reference for null geodesics of a static observer outside the photon sphere.
 * Units: Schwarzschild radius = 1, u = 1/r, local angle measured from the inward radial direction.
 * u'' = -u + 3u^2/2, (u')^2 + u^2 - u^3 = 1/b^2. No Kerr spin or disc emission is implied.
 */
public final class SchwarzschildRay {
    public static final double PHOTON_RADIUS=1.5, CRITICAL_IMPACT=3*Math.sqrt(3)/2;
    public enum Fate { ESCAPED, CAPTURED, UNRESOLVED }
    public record Result(Fate fate,double azimuth,double impact,double closestRadius,
                         double relativeEnergyError,int steps) {}
    private SchwarzschildRay() {}

    public static double impact(double radius,double inwardAngle) {
        validate(radius,inwardAngle);
        return radius*Math.sin(inwardAngle)/Math.sqrt(1-1/radius);
    }

    public static double shadowAngle(double radius) {
        validate(radius,0);
        return Math.asin(Math.min(1,CRITICAL_IMPACT*Math.sqrt(1-1/radius)/radius));
    }

    public static Result trace(double radius,double inwardAngle) { return trace(radius,inwardAngle,1e-11); }

    public static Result trace(double radius,double inwardAngle,double tolerance) {
        validate(radius,inwardAngle);
        if(!Double.isFinite(tolerance) || tolerance<1e-14 || tolerance>1e-5)
            throw new IllegalArgumentException("Tolerance must be 1e-14..1e-5");
        double b=impact(radius,inwardAngle);
        boolean inward=inwardAngle<Math.PI/2;
        if(Math.abs(Math.sin(inwardAngle))<1e-12)
            return new Result(inward?Fate.CAPTURED:Fate.ESCAPED,0,0,inward?1:radius,0,0);
        if(inward && Math.abs(b-CRITICAL_IMPACT)<CRITICAL_IMPACT*1e-12)
            return new Result(Fate.UNRESOLVED,Double.POSITIVE_INFINITY,b,PHOTON_RADIUS,0,0);
        double energy=1/(b*b),u=1/radius;
        // The local tetrad contributes sqrt(1-rs/r) to the impact parameter.
        double velocity=Math.cos(inwardAngle)/b;
        double phi=0,maximumU=u,maxError=0,h=Math.min(.02,.02*b);
        int steps=0;
        while(phi<64*Math.PI && steps<200000) {
            double[] full=rk4(u,velocity,h),half=rk4(u,velocity,h/2);
            half=rk4(half[0],half[1],h/2);
            double error=Math.max(Math.abs(full[0]-half[0]),Math.abs(full[1]-half[1]))/15;
            double allowed=tolerance*Math.max(1,Math.max(Math.abs(u),Math.abs(velocity)));
            if(error>allowed && h>1e-10) { h*=Math.max(.2,.9*Math.pow(allowed/error,.2)); continue; }
            steps++;
            double nextU=half[0]+(half[0]-full[0])/15,nextV=half[1]+(half[1]-full[1])/15;
            if(velocity>0 && nextV<0) {
                double lo=0,hi=h;
                for(int i=0;i<35;i++) {
                    double mid=(lo+hi)*.5;
                    if(rk4(u,velocity,mid)[1]>0)lo=mid;else hi=mid;
                }
                maximumU=Math.max(maximumU,rk4(u,velocity,(lo+hi)*.5)[0]);
            }
            if(nextU<=0 || nextU>=1) {
                double boundary=nextU<=0 ? 0 : 1,lo=0,hi=h;
                for(int i=0;i<35;i++) {
                    double mid=(lo+hi)*.5;
                    double value=rk4(u,velocity,mid)[0];
                    if((boundary==0 && value>0) || (boundary==1 && value<1)) lo=mid; else hi=mid;
                }
                double event=(lo+hi)*.5; double[] last=rk4(u,velocity,event);
                maxError=Math.max(maxError,energyError(last[0],last[1],energy));
                Fate fate=boundary==0?Fate.ESCAPED:Fate.CAPTURED;
                Fate expected=inward && b<CRITICAL_IMPACT?Fate.CAPTURED:Fate.ESCAPED;
                // Numerical drift near the separatrix must never turn into the opposite fate.
                if(fate!=expected)fate=Fate.UNRESOLVED;
                return new Result(fate,phi+event,b,
                        boundary==1?1:1/Math.max(maximumU,u),maxError,steps);
            }
            phi+=h;u=nextU;velocity=nextV;maximumU=Math.max(maximumU,u);
            maxError=Math.max(maxError,energyError(u,velocity,energy));
            h=Math.min(.04,h*Math.clamp(error==0?2:.9*Math.pow(allowed/error,.2),.5,2));
        }
        return new Result(Fate.UNRESOLVED,phi,b,1/maximumU,maxError,steps);
    }

    private static double[] rk4(double u,double v,double h) {
        double a1=acceleration(u),v2=v+h*a1/2,u2=u+h*v/2;
        double a2=acceleration(u2),v3=v+h*a2/2,u3=u+h*v2/2;
        double a3=acceleration(u3),v4=v+h*a3,u4=u+h*v3;
        return new double[]{u+h*(v+2*v2+2*v3+v4)/6,v+h*(a1+2*a2+2*a3+acceleration(u4))/6};
    }
    private static double acceleration(double u) { return -u+1.5*u*u; }
    private static double energyError(double u,double v,double energy) {
        return Math.abs(v*v+u*u-u*u*u-energy)/energy;
    }
    private static void validate(double radius,double angle) {
        if(!Double.isFinite(radius) || radius<=PHOTON_RADIUS || !Double.isFinite(angle) || angle<0 || angle>Math.PI)
            throw new IllegalArgumentException("Reference requires r > 1.5 and angle 0..pi");
    }
}
