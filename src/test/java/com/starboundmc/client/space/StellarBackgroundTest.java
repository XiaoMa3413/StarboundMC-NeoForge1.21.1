package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class StellarBackgroundTest {
    @Test void cubeRoundTripsIncludingCornersAndPoles() {
        for(int f=0;f<6;f++) for(double u:new double[]{-1,-.99,0,.99,1}) for(double v:new double[]{-1,-.99,0,.99,1}) {
            var d=SkyCube.direction(f,u,v);var c=SkyCube.coordinate(d[0],d[1],d[2]);
            assertArrayEquals(d,SkyCube.direction(c.face(),c.u(),c.v()),1e-14);
        }
        assertThrows(IllegalArgumentException.class,()->SkyCube.coordinate(0,0,0));
        assertThrows(IllegalArgumentException.class,()->SkyCube.coordinate(Double.NaN,1,0));
    }

    @Test void exactCubeAreasCoverTheSphere() {
        for(int n:new int[]{1,2,32,128}) {
            double area=0;for(int y=0;y<n;y++)for(int x=0;x<n;x++)area+=6*SkyCube.solidAngle(x,y,n);
            assertEquals(4*Math.PI,area,1e-12);
        }
    }

    @Test void indexMatchesExhaustiveQueriesAndHasBoundedMemory() {
        var sky=new StellarBackground(32000);var data=sky.sourceData();Random rng=new Random(531);
        for(int i=0;i<200;i++) {
            var d=SkyCube.direction(i%6,rng.nextDouble()*2-1,rng.nextDouble()*2-1);
            for(double sigma:new double[]{0,.002,StellarBackground.MAX_BEAM_SIGMA})
                assertArrayEquals(brute(data,sky.count(),d,sigma),sky.radiance(d[0],d[1],d[2],sigma),1e-10);
        }
        for(int i=0;i<sky.count();i++) {
            int p=i*8,id=i;
            assertTrue(Arrays.stream(sky.candidates(data[p],data[p+1],data[p+2])).anyMatch(n->n==id));
        }
        assertTrue(sky.maximumCandidates()<512,"max="+sky.maximumCandidates());
        assertTrue(sky.gpuBytes()<8*1024*1024,"bytes="+sky.gpuBytes());
        System.out.println("Sky query: candidates="+sky.maximumCandidates()+" memberships="+sky.memberships()+" GPU bytes="+sky.gpuBytes());
    }

    @Test void capQueriesDoNotLoseStarsAtFaceEdges() {
        for(int f=0;f<6;f++) for(double u:new double[]{-.999,-.96,.96,.999}) for(double v:new double[]{-.999,0,.999}) {
            var d=SkyCube.direction(f,u,v);
            var star=new BackgroundStarCatalog.Star((float)d[0],(float)d[1],(float)d[2],.45F,1,1,1,1,0);
            var sky=new StellarBackground(new BackgroundStarCatalog.Star[]{star});
            for(int g=0;g<6;g++)for(double a:new double[]{-1,-.98,.98,1})for(double b:new double[]{-1,-.98,0,.98,1}) {
                var q=SkyCube.direction(g,a,b);
                assertArrayEquals(brute(sky.sourceData(),1,q,.008),sky.radiance(q[0],q[1],q[2],.008),1e-10);
            }
        }
    }

    @Test void everyFluxLevelConservesTheSameIntegratedRgb() {
        var sky=new StellarBackground(6000);var sources=sky.sourceData();double[] expected=new double[3];
        for(int i=0;i<sky.count();i++)for(int k=0;k<3;k++)expected[k]+=sources[i*8+4+k];
        var data=sky.fluxData();int row=0;
        for(int n=StellarBackground.FLUX_SIZE;n>=1;n/=2) {
            double[] total=new double[3];
            for(int f=0;f<6;f++)for(int y=0;y<n;y++)for(int x=0;x<n;x++) {
                int p=((row+f*n+y)*StellarBackground.FLUX_SIZE+x)*4;
                for(int k=0;k<3;k++)total[k]+=data[p+k]*SkyCube.solidAngle(x,y,n);
                assertTrue(data[p+3]>=0 && data[p+3]<=1);
            }
            assertArrayEquals(expected,total,1e-10);row+=6*n;
        }
        assertEquals(StellarBackground.FLUX_HEIGHT,row);
    }

    @Test void beamBroadeningConservesOneSourceFlux() {
        var sky=new StellarBackground(new BackgroundStarCatalog.Star[]{new BackgroundStarCatalog.Star(0,0,1,.45F,1,1,1,2,0)});
        for(double beam:new double[]{0,.002,.008}) {
            double variance=beam*beam+Math.pow(.45/StellarBackground.FOCAL_REFERENCE,2);
            double radius=StellarBackground.SUPPORT_SIGMAS*Math.sqrt(variance),sum=0;
            int steps=2000;
            for(int i=0;i<steps;i++) {
                double theta=radius*(i+.5)/steps;
                sum+=sky.radiance(Math.sin(theta),0,Math.cos(theta),beam)[0]*2*Math.PI*Math.sin(theta)*radius/steps;
            }
            double expected=2/(StellarBackground.FOCAL_REFERENCE*StellarBackground.FOCAL_REFERENCE);
            assertEquals(expected,sum,expected*5e-5);
        }
    }

    private static double[] brute(float[] data,int count,double[] d,double sigma) {
        double[] rgb=new double[3];
        for(int i=0;i<count;i++) {
            int p=i*8;double dx=d[0]-data[p],dy=d[1]-data[p+1],dz=d[2]-data[p+2];
            double variance=sigma*sigma+data[p+7]*data[p+7],r2=dx*dx+dy*dy+dz*dz;
            if(r2>StellarBackground.SUPPORT_SIGMAS*StellarBackground.SUPPORT_SIGMAS*variance)continue;
            double kernel=Math.exp(-r2/(2*variance))/(2*Math.PI*variance);
            for(int k=0;k<3;k++)rgb[k]+=data[p+4+k]*kernel;
        }
        return rgb;
    }
}
