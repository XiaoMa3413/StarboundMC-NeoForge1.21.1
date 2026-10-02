package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchwarzschildRayTest {
    @Test void shadowAgreesWithAnalyticCriticalImpact() {
        for(double radius:new double[]{2,5,20,100,1000}) {
            double angle=SchwarzschildRay.shadowAngle(radius);
            assertEquals(SchwarzschildRay.CRITICAL_IMPACT,SchwarzschildRay.impact(radius,angle),1e-12);
            assertEquals(SchwarzschildRay.Fate.CAPTURED,SchwarzschildRay.trace(radius,angle*(1-1e-5)).fate());
            assertEquals(SchwarzschildRay.Fate.ESCAPED,SchwarzschildRay.trace(radius,angle*(1+1e-5)).fate());
            assertEquals(SchwarzschildRay.Fate.UNRESOLVED,SchwarzschildRay.trace(radius,angle).fate());
        }
    }

    @Test void energyAndToleranceConvergeOnStrongDeflection() {
        double edge=SchwarzschildRay.shadowAngle(20);
        for(double angle:new double[]{edge+.00001,edge+.001,.3,1,Math.PI/2,2.8}) {
            var a=SchwarzschildRay.trace(20,angle,1e-9);var b=SchwarzschildRay.trace(20,angle,1e-12);
            assertEquals(SchwarzschildRay.Fate.ESCAPED,b.fate());
            assertEquals(a.azimuth(),b.azimuth(),1e-5);
            assertTrue(b.relativeEnergyError()<2e-8,"error="+b.relativeEnergyError());
            assertTrue(b.closestRadius()>=1.5);
        }
    }

    @Test void weakFieldLimitHasTheSchwarzschildTwoOverBDeflection() {
        double radius=1e7;
        for(double b:new double[]{200,500,1000}) {
            double angle=Math.asin(b*Math.sqrt(1-1/radius)/radius);
            var ray=SchwarzschildRay.trace(radius,angle);
            double bending=ray.azimuth()-(Math.PI-angle);
            assertEquals(2/b,bending,(2/b)*.02);
        }
    }

    @Test void strongDeflectionMatchesIndependentRadialQuadratureAndApsisRoot() {
        double radius=100;
        for(double b:new double[]{2.599,2.7,4,10,30}) {
            double lo=0,hi=2.0/3;
            for(int i=0;i<60;i++) {
                double u=(lo+hi)/2;
                if(u*u*(1-u)<1/(b*b))lo=u;else hi=u;
            }
            double apsis=(lo+hi)/2;
            double angle=Math.asin(b*Math.sqrt(1-1/radius)/radius);
            var ray=SchwarzschildRay.trace(radius,angle,1e-12);
            double quadrature=radialIntegral(apsis,Math.PI/2)*2
                    -radialIntegral(apsis,Math.asin(Math.sqrt(1/(radius*apsis))));
            assertEquals(quadrature,ray.azimuth(),2e-8);
            assertEquals(1/apsis,ray.closestRadius(),1e-8);
        }
    }

    private static double radialIntegral(double turn,double end) {
        // Independent u=turn*sin(theta)^2 quadrature, with the turning-root factor cancelled.
        double sum=0;int n=40000;
        for(int i=0;i<n;i++) {
            double theta=(i+.5)*end/n,s=Math.sin(theta),u=turn*s*s;
            double factor=turn+u-turn*turn-turn*u-u*u;
            sum+=2*Math.sqrt(turn)*s/Math.sqrt(factor);
        }
        return sum*end/n;
    }

    @Test void nearCriticalRaysWindRatherThanWrapBeforeInterpolation() {
        double edge=SchwarzschildRay.shadowAngle(20),previous=0;
        for(double offset:new double[]{.01,.001,.0001,.00001,.000001}) {
            var r=SchwarzschildRay.trace(20,edge+offset);
            assertEquals(SchwarzschildRay.Fate.ESCAPED,r.fate());
            assertTrue(r.azimuth()>previous);previous=r.azimuth();
        }
        assertTrue(previous>2*Math.PI);
        for(double delta:new double[]{1e-11,1e-10,1e-9}) {
            assertNotEquals(SchwarzschildRay.Fate.CAPTURED,SchwarzschildRay.trace(20,edge+delta).fate());
            assertNotEquals(SchwarzschildRay.Fate.ESCAPED,SchwarzschildRay.trace(20,edge-delta).fate());
        }
    }

    @Test void radialAndOutwardRaysAreHandledWithoutSingularDivision() {
        assertEquals(SchwarzschildRay.Fate.CAPTURED,SchwarzschildRay.trace(20,0).fate());
        assertEquals(0,SchwarzschildRay.trace(20,Math.PI).azimuth());
        for(double a:new double[]{Math.PI/2,2,3})assertEquals(SchwarzschildRay.Fate.ESCAPED,SchwarzschildRay.trace(2,a).fate());
        assertThrows(IllegalArgumentException.class,()->SchwarzschildRay.trace(1.5,.1));
        assertThrows(IllegalArgumentException.class,()->SchwarzschildRay.trace(20,Double.NaN));
    }

    @Test void fixedObserverTableMatchesIndependentHighPrecisionIntegrations() {
        var table=new SchwarzschildLensTable(20);
        for(int i=0;i<1000;i++) {
            double angle=table.shadow()+1e-5*Math.exp(i/999.0*Math.log((Math.PI/2-table.shadow())/1e-5));
            assertEquals(SchwarzschildRay.trace(20,angle,1e-12).azimuth(),table.lookup(angle),6e-5);
        }
        for(int i=0;i<100;i++) {
            double angle=Math.PI/2+(i+.5)/100*Math.PI/2;
            assertEquals(SchwarzschildRay.trace(20,angle).azimuth(),table.lookup(angle),2e-6);
        }
        assertTrue(Double.isNaN(table.lookup(0)));
    }
}
