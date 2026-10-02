package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SchwarzschildDifferentialTest {
    @Test void angleAndTimeVariationsAgreeWithIndependentNearbyTrajectories() {
        int queries=0;double maxU=0,maxT=0;
        for(double observer:new double[]{6,12,20}) {
            double critical=SchwarzschildRay.shadowAngle(observer);
            for(double angle:new double[]{.03,.1,critical-.0002,critical+.0002,.4,.7,1.2,1.8,2.6,3.1}) {
                double epsilon=Math.min(1e-6,Math.abs(angle-critical)*.001);
                var path=new SchwarzschildPath(observer,angle,1e-13);
                var plus=new SchwarzschildPath(observer,angle+epsilon,1e-13);
                var minus=new SchwarzschildPath(observer,angle-epsilon,1e-13);
                double end=Math.min(path.endAzimuth(),Math.min(plus.endAzimuth(),minus.endAzimuth()));
                assertEquals(0,path.angularDerivative(0),0);assertEquals(0,path.angularTimeDerivative(0),0);
                for(int i=1;i<100;i++) {
                    double phi=end*i/100;var s=path.sample(phi);
                    if(s.radius()<4.63 || s.radius()>9.35)continue;
                    double q=(plus.sample(phi).inverseRadius()-minus.sample(phi).inverseRadius())/(2*epsilon);
                    double t=(plus.sample(phi).delay()-minus.sample(phi).delay())/(2*epsilon);
                    double du=Math.abs(q-path.angularDerivative(phi))/Math.max(1,Math.abs(q));
                    double dt=Math.abs(t-path.angularTimeDerivative(phi))/Math.max(1,Math.abs(t));
                    maxU=Math.max(maxU,du);maxT=Math.max(maxT,dt);
                    assertTrue(du<2e-5,"Angle variation: "+du);assertTrue(dt<2e-5,"Time variation: "+dt);
                    queries++;
                }
            }
        }
        assertTrue(queries>250);
        System.out.println("rayDifferential queries="+queries+" maxScaledUError="+maxU+" maxScaledTimeError="+maxT);
    }
}
