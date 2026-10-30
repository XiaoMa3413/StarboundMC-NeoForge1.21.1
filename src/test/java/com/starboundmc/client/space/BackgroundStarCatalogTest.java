package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;
import java.io.StringReader;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class BackgroundStarCatalogTest {
    @Test void qualityBudgetsRetainTheSameSeededStars() {
        var all = BackgroundStarCatalog.generate(32000);
        for (int count : new int[] {2000,6000,20000})
            assertArrayEquals(BackgroundStarCatalog.generate(count),Arrays.copyOf(all,count));
    }
    @Test void catalogPreservesTheStellarViewLodTwoReferenceSequence() {
        // Captured from the source LOD2 Random(seed + 1) generation sequence.
        var star = BackgroundStarCatalog.generate(3)[0];
        assertEquals(515856.87627323024,star.x(),1e-8);
        assertEquals(-234573.77974789584,star.y(),1e-8);
        assertEquals(-600886.3544776015,star.z(),1e-8);
        assertEquals(.032357182F,star.size());
        assertEquals(4.908836741028918,star.rotation(),1e-12);
        assertEquals(210,star.red()); assertEquals(225,star.green()); assertEquals(255,star.blue());
        assertEquals(8,star.brightness()); assertEquals(6000000,star.maxDistance());
        assertEquals(-271136.4522860584,BackgroundStarCatalog.generate(3)[1].x(),1e-8);
    }
    @Test void fieldIsAUniformThreeDimensionalSphereWithRareBrightTypes() {
        var stars = BackgroundStarCatalog.generate(32000);
        double sumRadius=0;
        int small=0,medium=0,bright=0;
        for (var star : stars) {
            double radius=Math.sqrt(star.x()*star.x()+star.y()*star.y()+star.z()*star.z());
            assertTrue(Double.isFinite(radius) && radius > 0 && radius <= BackgroundStarCatalog.RADIUS);
            assertTrue(star.rotation() >= 0 && star.rotation() < 2*Math.PI);
            assertTrue(star.size() >= .025F && star.size() <= .22F);
            assertTrue(star.brightness() >= 8 && star.brightness() <= 255);
            sumRadius+=radius;
            if (star.brightness() <= 32) small++;
            else if (star.brightness() < 225) medium++;
            else bright++;
        }
        assertEquals(.75,sumRadius/stars.length/BackgroundStarCatalog.RADIUS,.01);
        assertTrue(small > stars.length*.8 && small < stars.length*.9);
        assertTrue(medium > stars.length*.1 && medium < stars.length*.2);
        assertTrue(bright > stars.length*.002 && bright < stars.length*.012);
    }
    @Test void brokenProfilesAndBudgetsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->BackgroundStarCatalog.generate(0));
        assertThrows(IllegalArgumentException.class,()->BackgroundStarCatalog.generate(32001));
        assertThrows(IllegalArgumentException.class,()->BackgroundStarCatalog.readProfile(new StringReader("{\"star_types\":[]}")));
        assertThrows(IllegalArgumentException.class,()->new BackgroundStarCatalog.StarType(255,255,255,.1F,.2F,0,256,6000000,1));
        assertThrows(IllegalArgumentException.class,()->new BackgroundStarCatalog.StarType(255,255,255,Float.NaN,.2F,0,255,6000000,1));
        assertThrows(IllegalArgumentException.class,()->new BackgroundStarCatalog.StarType(255,255,255,.1F,.2F,0,255,4000000,1));
    }
}
