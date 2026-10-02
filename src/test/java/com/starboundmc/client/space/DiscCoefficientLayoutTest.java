package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiscCoefficientLayoutTest {
    @Test void generationWindowsCoverBothFieldsAndPrefetchWithoutBankCollisions() {
        for(double t:new double[]{-10000,-105,-70,-35,-.0001,0,34.9999,35,70,180,400,10000}) {
            int top=DiscCoefficientLayout.newest(t);
            var occupied=new boolean[8];
            for(int g=top-7;g<=top;g++) {
                assertFalse(occupied[DiscCoefficientLayout.bank(g)]);occupied[DiscCoefficientLayout.bank(g)]=true;
            }
            for(double lag:new double[]{0,10,35,70,105}) {
                double retarded=t-lag;
                for(int g:new int[]{2*(int)Math.floor(retarded/70),2*(int)Math.floor((retarded+35)/70)+1})
                    assertTrue(g<=top && g>=top-7,"retarded field outside prepared window t="+t+" lag="+lag);
            }
        }
    }
    @Test void guardedMipChartsFitAndNeverShareBorders() {
        for(var layout:new DiscCoefficientLayout[]{new DiscCoefficientLayout(128,512,16),
                new DiscCoefficientLayout(128,256,16),new DiscCoefficientLayout(64,256,8)}) {
            assertTrue(layout.width()<=16384 && layout.rows()<=16384);
            for(int level=0;level<layout.levels();level++) {
                assertEquals(layout.offset(level)+layout.rows(level),layout.offset(level+1));
                assertTrue(layout.width(level)<=layout.width());
                assertEquals(layout.radial(level)+2,layout.width(level)/layout.height(level));
            }
            assertEquals((long)layout.width()*layout.rows()*8,layout.gpuBytes());
        }
    }
}
