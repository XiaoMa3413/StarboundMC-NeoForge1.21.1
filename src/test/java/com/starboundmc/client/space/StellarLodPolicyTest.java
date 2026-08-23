package com.starboundmc.client.space;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StellarLodPolicyTest
{
    @Test
    void classifiesByScreenSizeInfluenceAndNavigationPriority()
    {
        assertEquals(StellarLod.POINT,
                StellarLodPolicy.preferred(1.20F, 0.0F, false));
        assertEquals(StellarLod.SIMPLIFIED,
                StellarLodPolicy.preferred(2.0F, 0.0F, false));
        assertEquals(StellarLod.FULL,
                StellarLodPolicy.preferred(5.0F, 0.50F, false));
        assertEquals(StellarLod.POINT,
                StellarLodPolicy.preferred(1.20F, 0.0F, true));
        assertEquals(StellarLod.FULL,
                StellarLodPolicy.preferred(2.0F, 0.10F, true));
    }

    @Test
    void enforcesFullAndSimplifiedBudgetsWithoutDroppingPoints()
    {
        FakeCandidates candidates = new FakeCandidates(30);
        Arrays.fill(candidates.radius, 5.0F);
        Arrays.fill(candidates.influence, 1.0F);
        candidates.active[28] = true;
        candidates.target[29] = true;

        StellarLodPolicy.assign(candidates);

        assertEquals(StellarLodPolicy.MAX_FULL_STARS, candidates.count(StellarLod.FULL));
        assertEquals(StellarLodPolicy.MAX_SIMPLIFIED_STARS,
                candidates.count(StellarLod.SIMPLIFIED));
        assertEquals(11, candidates.count(StellarLod.POINT));
        assertEquals(StellarLod.FULL, candidates.lod[28]);
        assertEquals(StellarLod.FULL, candidates.lod[29]);
    }

    private static final class FakeCandidates implements StellarLodPolicy.Candidates
    {
        private final float[] radius;
        private final float[] influence;
        private final boolean[] active;
        private final boolean[] target;
        private final StellarLod[] lod;

        private FakeCandidates(int count)
        {
            radius = new float[count];
            influence = new float[count];
            active = new boolean[count];
            target = new boolean[count];
            lod = new StellarLod[count];
        }

        private int count(StellarLod level)
        {
            int result = 0;
            for (StellarLod value : lod)
                if (value == level)
                    result++;
            return result;
        }

        @Override public int count() { return lod.length; }
        @Override public float projectedRadius(int index) { return radius[index]; }
        @Override public float systemInfluence(int index) { return influence[index]; }
        @Override public boolean activeSystem(int index) { return active[index]; }
        @Override public boolean navigationTarget(int index) { return target[index]; }
        @Override public StellarLod lod(int index) { return lod[index]; }
        @Override public void setLod(int index, StellarLod level) { lod[index] = level; }
    }
}
