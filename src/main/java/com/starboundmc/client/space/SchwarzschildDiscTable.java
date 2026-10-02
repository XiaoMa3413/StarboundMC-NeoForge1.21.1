package com.starboundmc.client.space;

/** Fixed-observer research accelerator. Three separate angle branches never interpolate capture
 * with escape. Each column stores a terminal header and a normalized finite timed trajectory. */
public final class SchwarzschildDiscTable {
    public static final int WIDTH=1024, INTERVALS=128, STRIDE=INTERVALS+3, PALETTE_ROW=3*STRIDE;
    public static final int FLOW_ROW=PALETTE_ROW+1, FLOW_ROWS=PeriodicFlowNoise.COUNT/WIDTH, HEIGHT=FLOW_ROW+FLOW_ROWS;
    public static final int PALETTE_SIZE=256;
    public static final double MIN_TEMPERATURE=1500, MAX_TEMPERATURE=12000;
    public static final double EDGE=SchwarzschildLensTable.MIN_EDGE_ANGLE;
    private final double radius,shadow;
    private final float[] data=new float[WIDTH*HEIGHT*4];

    public SchwarzschildDiscTable(double radius) {
        this.radius=radius;shadow=SchwarzschildRay.shadowAngle(radius);
        for(int branch=0;branch<3;branch++)for(int x=0;x<WIDTH;x++) {
            double angle=angle(branch,x/(double)(WIDTH-1));
            var path=new SchwarzschildPath(radius,angle);
            var ray=SchwarzschildRay.trace(radius,angle);
            if(path.fate()!=ray.fate() || path.fate()==SchwarzschildRay.Fate.UNRESOLVED)
                throw new IllegalStateException("Unresolved timed table column");
            put(x,branch*STRIDE,path.endAzimuth(),ray.azimuth(),path.impact(),branch==0?-1:1);
            for(int y=0;y<=INTERVALS;y++) {
                var s=path.sample(path.endAzimuth()*y/INTERVALS);
                put(x,branch*STRIDE+1+y,s.inverseRadius(),s.slope(),s.delay(),path.angularDerivative(s.azimuth()));
            }
            var shell=path.shellRanges(SchwarzschildDisc.INNER_RADIUS,SchwarzschildDisc.OUTER_RADIUS);
            put(x,branch*STRIDE+STRIDE-1,shell[0],shell[1],shell[2],shell[3]);
        }
        for(int x=0;x<PALETTE_SIZE;x++) {
            var c=StarPhotometry.blackbodyRadiance(MIN_TEMPERATURE+(MAX_TEMPERATURE-MIN_TEMPERATURE)*x/(PALETTE_SIZE-1));
            put(x,PALETTE_ROW,c.red(),c.green(),c.blue(),0);
        }
        for(int z=0;z<64;z++)for(int y=0;y<64;y++)for(int x=0;x<64;x++) {
            int pixel=(z*64+y)*64+x;
            put(pixel%WIDTH,FLOW_ROW+pixel/WIDTH,PeriodicFlowNoise.value(x,y,z),PeriodicFlowNoise.value(x+1,y,z),
                    PeriodicFlowNoise.value(x,y+1,z),PeriodicFlowNoise.value(x+1,y+1,z));
        }
    }

    private void put(int x,int y,double a,double b,double c,double d) {
        int p=(y*WIDTH+x)*4;data[p]=(float)a;data[p+1]=(float)b;data[p+2]=(float)c;data[p+3]=(float)d;
    }
    public double angle(int branch,double t) {
        return switch(branch) {
            case 0 -> Math.max(1e-6,shadow-EDGE*Math.exp(t*Math.log(shadow/EDGE)));
            case 1 -> shadow+EDGE*Math.exp(t*Math.log((Math.PI/2-shadow)/EDGE));
            case 2 -> Math.min(Math.PI-1e-6,Math.PI/2+t*Math.PI/2);
            default -> throw new IllegalArgumentException("Invalid branch");
        };
    }
    private double pixel(double angle,int branch) {
        double t=switch(branch) {
            case 0 -> Math.log(Math.max(shadow-angle,EDGE)/EDGE)/Math.log(shadow/EDGE);
            case 1 -> Math.log(Math.max(angle-shadow,EDGE)/EDGE)/Math.log((Math.PI/2-shadow)/EDGE);
            default -> (angle-Math.PI/2)/(Math.PI/2);
        };
        return Math.clamp(t,0,1)*(WIDTH-1);
    }
    public SchwarzschildPath.Sample lookup(double angle,double phi) {
        if(!Double.isFinite(angle) || angle<0 || angle>Math.PI || !Double.isFinite(phi) || phi<0)
            throw new IllegalArgumentException("Invalid table query");
        int branch=angle<shadow?0:angle<=Math.PI/2?1:2;
        double p=pixel(angle,branch);int x=(int)p,right=Math.min(x+1,WIDTH-1);double f=p-x;
        var a=column(x,branch,phi);var b=column(right,branch,phi);
        // Both neighboring paths must still exist; do not interpolate a terminated ray with a hit.
        if(a==null || b==null)return null;
        return new SchwarzschildPath.Sample(phi,a.inverseRadius()*(1-f)+b.inverseRadius()*f,
                a.slope()*(1-f)+b.slope()*f,a.delay()*(1-f)+b.delay()*f);
    }
    public double[] shellLookup(double angle) {
        if(!Double.isFinite(angle) || angle<0 || angle>Math.PI)throw new IllegalArgumentException("Invalid shell query");
        int branch=angle<shadow?0:angle<=Math.PI/2?1:2;
        double p=pixel(angle,branch);int x=(int)p,right=Math.min(x+1,WIDTH-1);double f=p-x;
        int a=((branch*STRIDE+STRIDE-1)*WIDTH+x)*4,b=((branch*STRIDE+STRIDE-1)*WIDTH+right)*4;
        var result=new double[4];for(int i=0;i<4;i++)result[i]=data[a+i]*(1-f)+data[b+i]*f;
        return result;
    }
    private SchwarzschildPath.Sample column(int x,int branch,double phi) {
        int header=(branch*STRIDE*WIDTH+x)*4;double end=data[header],impact=data[header+2];
        if(phi>end)return null;
        double p=Math.clamp(phi/end,0,1)*INTERVALS;int y=Math.min((int)p,INTERVALS-1);
        double f=p-y,h=end/INTERVALS;
        int a=((branch*STRIDE+1+y)*WIDTH+x)*4,b=a+WIDTH*4;
        double u=SchwarzschildPath.hermite(data[a],data[b],h*data[a+1],h*data[b+1],f);
        double time=SchwarzschildPath.hermite(data[a+2],data[b+2],h*SchwarzschildPath.timeDerivative(impact,data[a]),
                h*SchwarzschildPath.timeDerivative(impact,data[b]),f);
        double slope=((6*f*f-6*f)*data[a]+(-6*f*f+6*f)*data[b])/h
                +(3*f*f-4*f+1)*data[a+1]+(3*f*f-2*f)*data[b+1];
        return new SchwarzschildPath.Sample(phi,u,slope,time);
    }
    public float[] data() { return data.clone(); }
    public long gpuBytes() { return (long)data.length*Float.BYTES; }
    public double radius() { return radius; }
}
