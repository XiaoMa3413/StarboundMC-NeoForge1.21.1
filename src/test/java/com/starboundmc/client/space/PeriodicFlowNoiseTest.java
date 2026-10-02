package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PeriodicFlowNoiseTest {
    @Test void latticeHasBoundedUnbiasedValuesAndWrapsNegativeCoordinates() {
        double sum=0,squares=0;
        for(int z=0;z<64;z++)for(int y=0;y<64;y++)for(int x=0;x<64;x++) {
            float value=PeriodicFlowNoise.value(x,y,z);assertTrue(value>=0 && value<1);
            sum+=value;squares+=value*value;
        }
        assertEquals(.5,sum/PeriodicFlowNoise.COUNT,.003);
        assertEquals(1.0/12,squares/PeriodicFlowNoise.COUNT-Math.pow(sum/PeriodicFlowNoise.COUNT,2),.002);
        assertEquals(PeriodicFlowNoise.value(63,0,63),PeriodicFlowNoise.value(-1,64,-65));
    }
    @Test void interpolatedFieldIsPeriodicAndSmoothAcrossItsWrapPlanes() {
        for(double[] p:new double[][]{{-.13,4.7,64.02},{63.9,-64.1,.7},{0,.21,.37},{64,.21,.37}}) {
            assertEquals(PeriodicFlowNoise.sample(p[0],p[1],p[2]),
                    PeriodicFlowNoise.sample(p[0]+64,p[1]-64,p[2]+128),1e-13);
        }
        for(double x:new double[]{-64,0,64,128}) {
            double e=1e-5,a=PeriodicFlowNoise.sample(x-e,.37,.63),b=PeriodicFlowNoise.sample(x,.37,.63);
            double c=PeriodicFlowNoise.sample(x+e,.37,.63);
            assertEquals(b,a,1e-9);assertEquals(b,c,1e-9);
            assertEquals((b-a)/e,(c-b)/e,5e-5);
        }
    }
}
