package com.starboundmc.client.space;

/** Small periodic lattice for the volume's repeatedly sampled flow, not the sky.
 * Each atlas texel holds the four xy corners of one cell at a fixed z layer. */
public final class PeriodicFlowNoise {
    public static final int SIZE=64, COUNT=SIZE*SIZE*SIZE;
    private PeriodicFlowNoise() {}
    public static float value(int x,int y,int z) {
        int key=(x&63)+SIZE*(y&63)+SIZE*SIZE*(z&63)+3413;
        key^=key>>>16;key*=0x7feb352d;key^=key>>>15;key*=0x846ca68b;key^=key>>>16;
        return (key>>>8)*0x1.0p-24F;
    }
    public static double sample(double x,double y,double z) {
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);
        double a=x-ix,b=y-iy,c=z-iz;a=a*a*(3-2*a);b=b*b*(3-2*b);c=c*c*(3-2*c);
        double sum=0;
        for(int k=0;k<2;k++)for(int j=0;j<2;j++)for(int i=0;i<2;i++)
            sum+=value(ix+i,iy+j,iz+k)*(i==0?1-a:a)*(j==0?1-b:b)*(k==0?1-c:c);
        return sum;
    }
}
