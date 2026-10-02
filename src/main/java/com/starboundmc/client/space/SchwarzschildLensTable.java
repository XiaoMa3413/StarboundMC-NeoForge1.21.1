package com.starboundmc.client.space;

/**
 * Small research accelerator for one static observer radius. Escape azimuth is unwrapped before
 * interpolation. Log angle distance from the shadow edge resolves multiple windings without
 * interpolating across capture/escape. Row 1 covers outward rays linearly.
 * A moving observer will need a radius-dependent table; this is deliberately not a world object.
 */
public final class SchwarzschildLensTable {
    public static final int WIDTH=2048;
    public static final double MIN_EDGE_ANGLE=1e-6, OBSERVER_RADIUS=20;
    private final double radius,shadow,logRange;
    private final float[] azimuth=new float[WIDTH*2];

    public SchwarzschildLensTable(double radius) {
        this.radius=radius;shadow=SchwarzschildRay.shadowAngle(radius);
        logRange=Math.log((Math.PI/2-shadow)/MIN_EDGE_ANGLE);
        if(logRange<=0) throw new IllegalArgumentException("Observer too close to photon sphere");
        for(int row=0;row<2;row++) for(int x=0;x<WIDTH;x++) {
            double t=x/(double)(WIDTH-1);
            double angle=row==0 ? shadow+MIN_EDGE_ANGLE*Math.exp(t*logRange) : Math.PI/2+t*Math.PI/2;
            var ray=SchwarzschildRay.trace(radius,Math.min(angle,Math.PI));
            if(ray.fate()!=SchwarzschildRay.Fate.ESCAPED) throw new IllegalStateException("Unresolved lens table ray");
            azimuth[row*WIDTH+x]=(float)ray.azimuth();
        }
    }

    /** Captured rays have no background direction and return NaN. */
    public double lookup(double angle) {
        if(!Double.isFinite(angle) || angle<0 || angle>Math.PI) throw new IllegalArgumentException("Invalid angle");
        if(angle<=shadow) return Double.NaN;
        int row=angle>Math.PI/2 ? 1 : 0;
        double t=row==1 ? (angle-Math.PI/2)/(Math.PI/2)
                : Math.log(Math.max(angle-shadow,MIN_EDGE_ANGLE)/MIN_EDGE_ANGLE)/logRange;
        double pixel=Math.clamp(t,0,1)*(WIDTH-1);int left=(int)pixel,right=Math.min(left+1,WIDTH-1);
        return azimuth[row*WIDTH+left]*(1-(pixel-left))+azimuth[row*WIDTH+right]*(pixel-left);
    }
    public float[] data() { return azimuth.clone(); }
    public double radius() { return radius; }
    public double shadow() { return shadow; }
    public double logRange() { return logRange; }
}
