package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchwarzschildVolumeTest {
    @Test void compactHeightProfileIsNormalizedEvenAndSmoothAtItsBoundary() {
        double integral=0,h=6.0/10000;
        for(int i=0;i<10000;i++)integral+=SchwarzschildVolume.profile(-3+(i+.5)*h)*h;
        assertEquals(1,integral,1e-10);
        assertEquals(0,SchwarzschildVolume.profile(3));assertEquals(0,SchwarzschildVolume.profile(4));
        assertTrue(SchwarzschildVolume.profile(3-1e-5)<1e-12);
        assertEquals(SchwarzschildVolume.profile(.71),SchwarzschildVolume.profile(-.71));
    }
    @Test void shellWindowsCoverDenseIndependentRadialSamplesIncludingAnInsideObserver() {
        for(double observer:new double[]{6,12,20})for(double angle:new double[]{.03,.1,.18,.4,.7,1.2,1.57,1.8,2.6}) {
            var path=new SchwarzschildPath(observer,angle,1e-12);var ranges=path.shellRanges(4.63,9.35);
            assertTrue(ranges[0]<=ranges[1] && ranges[1]<=ranges[2]+1e-10 && ranges[2]<=ranges[3]);
            for(int i=0;i<400;i++) {
                double phi=path.endAzimuth()*(i+.37)/400,r=path.sample(phi).radius();
                boolean inside=r>=4.63 && r<=9.35;
                assertEquals(inside,phi>=ranges[0] && phi<=ranges[1] || phi>=ranges[2] && phi<=ranges[3]);
            }
        }
    }
    @Test void coneIntervalsCoverExactlyTheFiniteMediumAndRetainCoplanarEmission() {
        boolean captured=false,coplanar=false;
        for(double angle:new double[]{.04,.08,.13,.2,.4,.7,1.2,1.8}) {
            var path=new SchwarzschildPath(6,angle,1e-12);
            for(double[] ab:new double[][]{{.15,-.7},{.06,-.4},{0,0},{.9,.1}}) {
                var ranges=SchwarzschildVolume.intervals(path,ab[0],ab[1],.022);
                captured|=!ranges.isEmpty() && path.fate()==SchwarzschildRay.Fate.CAPTURED;
                coplanar|=!ranges.isEmpty() && ab[0]==0 && ab[1]==0;
                for(int i=0;i<500;i++) {
                    double phi=path.endAzimuth()*(i+.41)/500,r=path.sample(phi).radius();
                    boolean inside=r>=4.63 && r<=9.35 && Math.abs(ab[0]*Math.cos(phi)+ab[1]*Math.sin(phi))<=.066;
                    assertEquals(inside,ranges.stream().anyMatch(w->phi>=w.start() && phi<=w.end()));
                }
            }
        }
        assertTrue(captured);assertTrue(coplanar);
    }
    @Test void properLengthMatchesTheSpatialMetricAndPhysicalOpacityTransformsTheMediumFrame() {
        var path=new SchwarzschildPath(20,.12,1e-12);
        for(var w:SchwarzschildVolume.intervals(path,.15,-.7,.022))for(int i=0;i<20;i++) {
            var s=path.sample(w.start()+(i+.5)*(w.end()-w.start())/20);
            double dr=-s.slope()*s.radius()*s.radius();
            double metric=Math.sqrt(dr*dr/(1-1/s.radius())+s.radius()*s.radius());
            assertEquals(metric,SchwarzschildVolume.lengthRate(path,s),metric*1e-6);
        }
        double a=SchwarzschildVolume.opticalDepth(path,.15,-.7,.698,.022,.12,true,1024);
        double b=SchwarzschildVolume.opticalDepth(path,.15,-.7,-.698,.022,.12,true,1024);
        assertTrue(a>0 && b>0 && a>b);
    }
    @Test void quadratureConvergesAndTheThinLimitRetainsTheNormalColumn() {
        double maxError=0;
        for(double observer:new double[]{6,20})for(double angle:new double[]{.05,.1,.14,.2,.4,.7}) {
            var path=new SchwarzschildPath(observer,angle,1e-12);
            double a=SchwarzschildVolume.opticalDepth(path,.15,-.7,.698,.022,.12,false,32);
            double b=SchwarzschildVolume.opticalDepth(path,.15,-.7,.698,.022,.12,false,2048);
            maxError=Math.max(maxError,Math.abs(a-b));assertEquals(b,a,.003);
        }
        var path=new SchwarzschildPath(20,.12,1e-12);
        var hits=SchwarzschildDisc.intersections(path,.15,-.7,4.63,9.35);assertFalse(hits.isEmpty());
        double thin=0;
        for(var hit:hits) {
            var s=path.sample(hit.azimuth());double mPrime=-.15*Math.sin(hit.azimuth())-.7*Math.cos(hit.azimuth());
            thin+=.12*SchwarzschildVolume.lengthRate(path,s)/(hit.radius()*Math.abs(mPrime));
        }
        assertEquals(thin,SchwarzschildVolume.opticalDepth(path,.15,-.7,.698,.00001,.12,false,2048),thin*1e-5);
        System.out.println("volume midpoint32MaxTauError="+maxError+" thinTau="+thin);
    }
    @Test void packedShellCoordinatesTrackTheIndependentReferenceAcrossCaptureAndEscape() {
        double maximum=0;
        for(double observer:new double[]{6,20}) {
            var table=new SchwarzschildDiscTable(observer);double edge=SchwarzschildRay.shadowAngle(observer);
            for(double angle:new double[]{.03,edge-.00004,edge+.00004,.2,.4,.7,1.2,1.8,2.6}) {
                var expected=new SchwarzschildPath(observer,angle,1e-12).shellRanges(4.63,9.35);
                var actual=table.shellLookup(angle);
                for(int i=0;i<4;i++) {maximum=Math.max(maximum,Math.abs(actual[i]-expected[i]));assertEquals(expected[i],actual[i],.006);}
            }
        }
        System.out.println("volume shellPhiMaxError="+maximum);
    }
    @Test void exteriorSourceWindowsFitTheShadersBoundedConeEnumeration() {
        // Extra windings near the photon orbit occur below the source annulus.
        // Its individual monotone radial portions must not silently exhaust
        // the shader's three pi-periodic height windows.
        double maxSpan=0;
        for(double observer:new double[]{1.6,3.5,6,12,20,63}) {
            double edge=SchwarzschildRay.shadowAngle(observer);
            for(int i=0;i<160;i++) {
                double angle=i<4?edge+(i-1.5)*1e-6:Math.PI*(i-3.5)/157;
                var path=new SchwarzschildPath(observer,angle,1e-12);var shell=path.shellRanges(4.63,9.35);
                for(int half=0;half<2;half++) {
                    double span=shell[half*2+1]-shell[half*2];maxSpan=Math.max(maxSpan,span);
                    assertTrue(span<Math.PI,"Source shell exceeds one cone period: "+span);
                }
            }
        }
        System.out.println("volume maximumMonotoneShellPhiSpan="+maxSpan);
    }
}
