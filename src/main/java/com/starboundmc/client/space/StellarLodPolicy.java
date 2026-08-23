package com.starboundmc.client.space;

/**
 * Allocation-free stellar LOD selection. Classification is continuous-data
 * driven; fixed budgets then keep nearby-system density from multiplying full
 * corona and particle passes.
 */
public final class StellarLodPolicy
{
    public static final int MAX_FULL_STARS = 3;
    public static final int MAX_SIMPLIFIED_STARS = 16;

    private static final float FULL_RADIUS = 4.0F;
    private static final float FULL_INFLUENCE = 0.35F;
    private static final float SIMPLIFIED_RADIUS = 1.85F;
    private static final float SIMPLIFIED_INFLUENCE = 0.08F;

    private StellarLodPolicy()
    {
    }

    public static void assign(Candidates candidates)
    {
        int count = candidates.count();
        for (int i = 0; i < count; i++)
            candidates.setLod(i, StellarLod.POINT);

        selectBest(candidates, StellarLod.FULL, MAX_FULL_STARS);
        selectBest(candidates, StellarLod.SIMPLIFIED, MAX_SIMPLIFIED_STARS);
    }

    static StellarLod preferred(float projectedRadius, float influence, boolean navigationTarget)
    {
        StellarLod level;
        if (projectedRadius >= FULL_RADIUS && influence >= FULL_INFLUENCE)
            level = StellarLod.FULL;
        else if (projectedRadius >= SIMPLIFIED_RADIUS || influence >= SIMPLIFIED_INFLUENCE)
            level = StellarLod.SIMPLIFIED;
        else
            level = StellarLod.POINT;

        if (!navigationTarget)
            return level;
        return switch (level)
        {
            // The point tier already has a dedicated navigation marker. Do not
            // replace it with the dim simplified disc while still in deep space.
            case POINT -> StellarLod.POINT;
            case SIMPLIFIED -> StellarLod.FULL;
            case FULL -> StellarLod.FULL;
        };
    }

    private static void selectBest(Candidates candidates, StellarLod level, int budget)
    {
        for (int selected = 0; selected < budget; selected++)
        {
            int bestIndex = -1;
            float bestScore = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < candidates.count(); i++)
            {
                if (candidates.lod(i) != StellarLod.POINT
                        || !eligible(candidates, i, level))
                    continue;
                float score = priorityScore(candidates, i);
                if (score > bestScore)
                {
                    bestScore = score;
                    bestIndex = i;
                }
            }
            if (bestIndex < 0)
                return;
            candidates.setLod(bestIndex, level);
        }
    }

    private static boolean eligible(Candidates candidates, int index, StellarLod level)
    {
        StellarLod preferred = preferred(candidates.projectedRadius(index),
                candidates.systemInfluence(index), candidates.navigationTarget(index));
        return level == StellarLod.FULL
                ? preferred == StellarLod.FULL
                : preferred != StellarLod.POINT;
    }

    private static float priorityScore(Candidates candidates, int index)
    {
        float score = candidates.projectedRadius(index) * 8.0F
                + candidates.systemInfluence(index) * 32.0F;
        if (candidates.navigationTarget(index))
            score += 128.0F;
        if (candidates.activeSystem(index))
            score += 256.0F;
        return score;
    }

    public interface Candidates
    {
        int count();

        float projectedRadius(int index);

        float systemInfluence(int index);

        boolean activeSystem(int index);

        boolean navigationTarget(int index);

        StellarLod lod(int index);

        void setLod(int index, StellarLod lod);
    }
}
