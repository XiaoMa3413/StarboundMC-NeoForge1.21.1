package com.starboundmc.client.space;

/** Shared CPU/GPU cube convention: +X, -X, +Y, -Y, +Z, -Z; no face-edge clamping. */
public final class SkyCube {
    private SkyCube() {}
    public record Coordinate(int face, double u, double v) {}

    public static Coordinate coordinate(double x, double y, double z) {
        double ax=Math.abs(x), ay=Math.abs(y), az=Math.abs(z);
        if (!Double.isFinite(ax+ay+az) || ax+ay+az == 0)
            throw new IllegalArgumentException("A finite nonzero sky direction is required");
        if (ax >= ay && ax >= az) return x > 0 ? new Coordinate(0,-z/ax,y/ax) : new Coordinate(1,z/ax,y/ax);
        if (ay >= az) return y > 0 ? new Coordinate(2,x/ay,-z/ay) : new Coordinate(3,x/ay,z/ay);
        return z > 0 ? new Coordinate(4,x/az,y/az) : new Coordinate(5,-x/az,y/az);
    }

    public static double[] direction(int face, double u, double v) {
        double[] d=switch(face) {
            case 0 -> new double[]{1,v,-u}; case 1 -> new double[]{-1,v,u};
            case 2 -> new double[]{u,1,-v}; case 3 -> new double[]{u,-1,v};
            case 4 -> new double[]{u,v,1}; case 5 -> new double[]{-u,v,-1};
            default -> throw new IllegalArgumentException("Unknown sky face");
        };
        double length=Math.sqrt(d[0]*d[0]+d[1]*d[1]+d[2]*d[2]);
        for(int i=0;i<3;i++) d[i]/=length;
        return d;
    }

    public static int pixel(double uv, int size) {
        return Math.clamp((int)Math.floor((uv+1)*.5*size),0,size-1);
    }

    public static int cell(double x,double y,double z,int size) {
        var c=coordinate(x,y,z);
        return (c.face()*size+pixel(c.v(),size))*size+pixel(c.u(),size);
    }

    /** Exact area of a gnomonic cube pixel, in steradians. */
    public static double solidAngle(int x,int y,int size) {
        double u0=2.0*x/size-1, u1=2.0*(x+1)/size-1;
        double v0=2.0*y/size-1, v1=2.0*(y+1)/size-1;
        return primitive(u1,v1)-primitive(u0,v1)-primitive(u1,v0)+primitive(u0,v0);
    }
    private static double primitive(double u,double v) { return Math.atan2(u*v,Math.sqrt(1+u*u+v*v)); }
}
