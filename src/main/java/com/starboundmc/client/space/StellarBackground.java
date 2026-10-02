package com.starboundmc.client.space;

import java.util.ArrayList;
import java.util.List;

/**
 * Direction-addressable version of the native catalogue. Ordinary sky drawing still uses its VBO.
 * Radiance is linear RGB per steradian; the existing pixel flux is calibrated at 720p / 70 degrees.
 * Narrow circular beams use the source index; wide beams use a flux-conserving cube hierarchy.
 * Dust remains in the shared GPU background field, rather than a second CPU noise implementation.
 */
public final class StellarBackground {
    public static final int GRID=32, FLUX_SIZE=128, DATA_WIDTH=1024;
    public static final double FOCAL_REFERENCE=720/(2*Math.tan(Math.toRadians(35)));
    public static final double MAX_BEAM_SIGMA=.008, SUPPORT_SIGMAS=4.5;
    public static final int FLUX_HEIGHT=6*(2*FLUX_SIZE-1);
    private static final double SUPPORT_RADIUS=SUPPORT_SIGMAS*Math.hypot(MAX_BEAM_SIGMA,.48/FOCAL_REFERENCE);
    private final int sourceCount;
    private final int[][] cells;
    private final float[] cellData, indices, sourceData, fluxData;
    private final int memberships, maximumCandidates;

    public StellarBackground(int count) { this(BackgroundStarCatalog.generate(count)); }

    public StellarBackground(BackgroundStarCatalog.Star[] catalog) {
        if(catalog.length == 0 || catalog.length > 32000) throw new IllegalArgumentException("Invalid star budget");
        var stars=catalog.clone();
        sourceCount=stars.length;
        List<List<Integer>> lists=new ArrayList<>(6*GRID*GRID);
        for(int i=0;i<6*GRID*GRID;i++) lists.add(new ArrayList<>());
        double[][] centers=new double[6*GRID*GRID][];
        double[] capLimit=new double[centers.length];
        for(int f=0;f<6;f++)for(int cy=0;cy<GRID;cy++)for(int cx=0;cx<GRID;cx++) {
            int cell=(f*GRID+cy)*GRID+cx;
            var center=SkyCube.direction(f,2.0*(cx+.5)/GRID-1,2.0*(cy+.5)/GRID-1);
            centers[cell]=center;double radius=0;
            for(int dx=0;dx<=1;dx++)for(int dy=0;dy<=1;dy++) {
                var corner=SkyCube.direction(f,2.0*(cx+dx)/GRID-1,2.0*(cy+dy)/GRID-1);
                radius=Math.max(radius,Math.acos(Math.clamp(center[0]*corner[0]+center[1]*corner[1]+center[2]*corner[2],-1,1)));
            }
            capLimit[cell]=Math.cos(SUPPORT_RADIUS+radius+1e-7);
        }
        sourceData=new float[DATA_WIDTH*height(stars.length*2)*4];
        double[] flux=new double[6*FLUX_SIZE*FLUX_SIZE*4];
        double sin=2*Math.sin(SUPPORT_RADIUS/2);
        for(int i=0;i<stars.length;i++) {
            var s=stars[i];
            double length=Math.sqrt(s.x()*s.x()+s.y()*s.y()+s.z()*s.z());
            if(!Double.isFinite(length) || Math.abs(length-1)>1e-5 || !(s.flux()>0)
                    || !(s.sigmaPixels()>0) || s.sigmaPixels()>.48001 || !Float.isFinite(s.flux())
                    || !Float.isFinite(s.red()+s.green()+s.blue()+s.dustFraction())
                    || s.red()<0 || s.green()<0 || s.blue()<0 || s.dustFraction()<0 || s.dustFraction()>1)
                throw new IllegalArgumentException("Invalid stellar source");
            double x=s.x()/length,y=s.y()/length,z=s.z()/length;
            double angularFlux=s.flux()/(FOCAL_REFERENCE*FOCAL_REFERENCE);
            int p=i*8;
            sourceData[p]=(float)x; sourceData[p+1]=(float)y; sourceData[p+2]=(float)z; sourceData[p+3]=s.dustFraction();
            sourceData[p+4]=(float)(s.red()*angularFlux); sourceData[p+5]=(float)(s.green()*angularFlux);
            sourceData[p+6]=(float)(s.blue()*angularFlux); sourceData[p+7]=(float)(s.sigmaPixels()/FOCAL_REFERENCE);
            int cell=SkyCube.cell(x,y,z,FLUX_SIZE)*4;
            flux[cell]+=sourceData[p+4]; flux[cell+1]+=sourceData[p+5]; flux[cell+2]+=sourceData[p+6];
            flux[cell+3]+=luminance(sourceData[p+4],sourceData[p+5],sourceData[p+6])*s.dustFraction();
            for(int face=0;face<6;face++) {
                double depth=switch(face){case 0->x;case 1->-x;case 2->y;case 3->-y;case 4->z;default->-z;};
                // Any direction on this face has depth >= 1/sqrt(3). A cap near its
                // equator cannot touch it; this also keeps the projection denominator safe.
                if(depth<=sin) continue;
                double u=switch(face){case 0->-z/depth;case 1->z/depth;case 2,3->x/depth;case 4->x/depth;default->-x/depth;};
                double v=switch(face){case 0,1,4,5->y/depth;case 2->-z/depth;default->z/depth;};
                // Component displacement is bounded by the cap chord; division is conservative.
                double du=sin*(1+Math.abs(u))/(depth-sin),dv=sin*(1+Math.abs(v))/(depth-sin);
                if(u+du < -1 || u-du > 1 || v+dv < -1 || v-dv > 1) continue;
                for(int cy=SkyCube.pixel(v-dv,GRID);cy<=SkyCube.pixel(v+dv,GRID);cy++)
                    for(int cx=SkyCube.pixel(u-du,GRID);cx<=SkyCube.pixel(u+du,GRID);cx++) {
                        int key=(face*GRID+cy)*GRID+cx;var center=centers[key];
                        if(x*center[0]+y*center[1]+z*center[2]>=capLimit[key]) lists.get(key).add(i);
                    }
            }
        }
        cells=new int[lists.size()][];
        cellData=new float[cells.length*2];
        int total=0,max=0;
        for(int i=0;i<cells.length;i++) {
            cells[i]=lists.get(i).stream().mapToInt(Integer::intValue).toArray();
            cellData[i*2]=total; cellData[i*2+1]=cells[i].length;
            total+=cells[i].length; max=Math.max(max,cells[i].length);
        }
        if(total>=1<<24) throw new IllegalArgumentException("Float address precision exhausted");
        memberships=total; maximumCandidates=max;
        indices=new float[DATA_WIDTH*height(total)];
        int offset=0; for(var c:cells) for(int id:c) indices[offset++]=id;
        fluxData=buildFluxHierarchy(flux);
    }

    private static float[] buildFluxHierarchy(double[] fine) {
        float[] packed=new float[FLUX_SIZE*FLUX_HEIGHT*4];
        int row=0;
        for(int n=FLUX_SIZE;n>=1;n/=2) {
            for(int face=0;face<6;face++) for(int y=0;y<n;y++) for(int x=0;x<n;x++) {
                int src=((face*n+y)*n+x)*4,dst=((row+face*n+y)*FLUX_SIZE+x)*4;
                double area=SkyCube.solidAngle(x,y,n);
                for(int k=0;k<3;k++) packed[dst+k]=(float)(fine[src+k]/area);
                double l=luminance(fine[src],fine[src+1],fine[src+2]);
                packed[dst+3]=l>0 ? (float)(fine[src+3]/l) : 0;
            }
            row+=6*n;
            if(n==1) break;
            int half=n/2; double[] coarse=new double[6*half*half*4];
            for(int f=0;f<6;f++) for(int y=0;y<n;y++) for(int x=0;x<n;x++)
                for(int k=0;k<4;k++) coarse[((f*half+y/2)*half+x/2)*4+k]+=fine[((f*n+y)*n+x)*4+k];
            fine=coarse;
        }
        return packed;
    }

    /** Reference point-source query, before dust. Gaussian tails beyond 4.5 sigma are omitted. */
    public double[] radiance(double x,double y,double z,double beamSigma) {
        if(!Double.isFinite(beamSigma) || beamSigma<0 || beamSigma>MAX_BEAM_SIGMA)
            throw new IllegalArgumentException("Indexed reference supports sigma 0.."+MAX_BEAM_SIGMA);
        SkyCube.coordinate(x,y,z);
        double length=Math.sqrt(x*x+y*y+z*z); x/=length;y/=length;z/=length;
        double[] result=new double[3];
        for(int id:cells[SkyCube.cell(x,y,z,GRID)]) {
            int p=id*8;
            double dx=x-sourceData[p],dy=y-sourceData[p+1],dz=z-sourceData[p+2];
            double variance=beamSigma*beamSigma+sourceData[p+7]*sourceData[p+7],distance2=dx*dx+dy*dy+dz*dz;
            if(distance2>SUPPORT_SIGMAS*SUPPORT_SIGMAS*variance) continue;
            double kernel=Math.exp(-distance2/(2*variance))/(2*Math.PI*variance);
            for(int k=0;k<3;k++) result[k]+=sourceData[p+4+k]*kernel;
        }
        return result;
    }

    public int[] candidates(double x,double y,double z) { return cells[SkyCube.cell(x,y,z,GRID)].clone(); }
    public int count() { return sourceCount; }
    public int memberships() { return memberships; }
    public int maximumCandidates() { return maximumCandidates; }
    public long gpuBytes() { return (long)(cellData.length+indices.length+sourceData.length+fluxData.length)*Float.BYTES; }
    public static int height(int texels) { return (texels+DATA_WIDTH-1)/DATA_WIDTH; }
    /** Upload data are copies; callers cannot mutate subsequent CPU queries. */
    public float[] cellData() { return cellData.clone(); }
    public float[] indices() { return indices.clone(); }
    public float[] sourceData() { return sourceData.clone(); }
    public float[] fluxData() { return fluxData.clone(); }
    private static double luminance(double r,double g,double b) { return .2126*r+.7152*g+.0722*b; }
}
