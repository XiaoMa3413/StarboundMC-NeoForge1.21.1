package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchwarzschildDiscTest {
    @Test void finitePathsAgreeWithExistingTerminalClassificationAndConserveEnergy() {
        double edge=SchwarzschildRay.shadowAngle(20);
        for(double angle:new double[]{.0001,.06,edge-.00001,edge+.00001,.4,1.57,2.3,Math.PI-.0001}) {
            var path=new SchwarzschildPath(20,angle);
            assertEquals(SchwarzschildRay.trace(20,angle).fate(),path.fate());
            assertTrue(path.maximumEnergyError()<1e-7,"energy="+path.maximumEnergyError());
            double previous=0;
            for(int i=1;i<=40;i++) {
                var p=path.sample(path.endAzimuth()*i/40);
                assertTrue(p.delay()>previous);previous=p.delay();
                assertTrue(p.radius()>=SchwarzschildPath.NEAR_RADIUS-1e-8);
                assertTrue(p.radius()<=SchwarzschildPath.FAR_RADIUS+1e-8);
            }
        }
    }

    @Test void timeAndAngleAgreeWithIndependentRadialQuadrature() {
        var path=new SchwarzschildPath(20,.08,1e-12);var hit=path.sample(.06);
        double start=.05,end=hit.inverseRadius(),e=1/path.impact(),time=0,phi=0;
        int n=40000;
        for(int i=0;i<n;i++) {
            double u=start+(i+.5)*(end-start)/n,velocity=Math.sqrt(e*e-u*u*(1-u));
            phi+=1/velocity;time+=e/(u*u*(1-u)*velocity);
        }
        assertEquals(hit.azimuth(),phi*(end-start)/n,1e-9);
        assertEquals(hit.delay(),time*(end-start)/n,1e-7);
    }

    @Test void planeCrossingsAreOrderedAndIncludeEmissionOnCapturedRays() {
        boolean capturedHit=false,multiple=false;
        double a=.15,b=-.7;
        for(double angle=.02;angle<.6;angle+=.002) {
            var path=new SchwarzschildPath(20,angle);
            var hits=SchwarzschildDisc.intersections(path,a,b,4.63,9.35);
            double time=-1,phi=-1;
            for(var hit:hits) {
                assertEquals(0,a*Math.cos(hit.azimuth())+b*Math.sin(hit.azimuth()),1e-12);
                assertTrue(hit.azimuth()>phi && hit.delay()>time);phi=hit.azimuth();time=hit.delay();
                assertTrue(hit.radius()>=4.63 && hit.radius()<=9.35);
                if(path.fate()==SchwarzschildRay.Fate.CAPTURED)capturedHit=true;
            }
            multiple|=hits.size()>1;
        }
        assertTrue(capturedHit);assertTrue(multiple);
        assertTrue(SchwarzschildDisc.intersections(new SchwarzschildPath(20,.4),0,0,4,9).isEmpty());
    }

    @Test void circularEmitterFrequencyMatchesTransverseDilationAndReversesBeaming() {
        double r=6,v=1/Math.sqrt(2*(r-1));
        double g=SchwarzschildDisc.frequencyRatio(20,r,0);
        assertEquals(Math.sqrt((1-1/r)/(1-1/20.0))*Math.sqrt(1-v*v),g,1e-14);
        assertTrue(SchwarzschildDisc.frequencyRatio(20,r,1)>g);
        assertTrue(SchwarzschildDisc.frequencyRatio(20,r,-1)<g);
        assertEquals(1/Math.sqrt(2*r*r*r),SchwarzschildDisc.angularVelocity(r),1e-14);
    }

    @Test void spectralRadianceRetainsEnergyAndContinuousTemperature() {
        var baseline=StarPhotometry.blackbodyRadiance(4500);
        assertEquals(1,baseline.red(),1e-7);
        var cool=StarPhotometry.blackbodyRadiance(3000);var hot=StarPhotometry.blackbodyRadiance(6000);
        assertTrue(cool.red()<baseline.red() && hot.red()>baseline.red());
        assertTrue(cool.blue()/cool.red()<baseline.blue()/baseline.red());
        assertTrue(hot.blue()/hot.red()>baseline.blue()/baseline.red());
        var adjacent=StarPhotometry.blackbodyRadiance(4500.001);
        assertEquals(baseline.green(),adjacent.green(),2e-6);
    }

    @Test void packedTrajectoryTableMatchesHighPrecisionHitsOnBothBranches() {
        var table=new SchwarzschildDiscTable(20);double maxRadius=0,maxTime=0;
        double edge=SchwarzschildRay.shadowAngle(20);
        for(double angle:new double[]{.02,.08,edge-.00004,edge+.00004,.23,.4,.65,1.2,1.8,2.6}) {
            var path=new SchwarzschildPath(20,angle,1e-12);
            for(int i=1;i<150;i++) {
                double phi=path.endAzimuth()*i/150;var exact=path.sample(phi);
                if(exact.radius()<4 || exact.radius()>10)continue;
                var fast=table.lookup(angle,phi);assertNotNull(fast);
                maxRadius=Math.max(maxRadius,Math.abs(fast.radius()-exact.radius()));
                maxTime=Math.max(maxTime,Math.abs(fast.delay()-exact.delay()));
                assertEquals(exact.radius(),fast.radius(),.005);
                assertEquals(exact.delay(),fast.delay(),.015);
            }
        }
        System.out.println("discTable maxRadiusError="+maxRadius+" maxDelayError="+maxTime+" bytes="+table.gpuBytes());
    }
}
