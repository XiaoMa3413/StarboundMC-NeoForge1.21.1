package com.starboundmc.client.compat.stellarview;

import com.starboundmc.space.UniversePosition;
import com.starboundmc.client.compat.stellarview.StellarViewPositionAdapter.Coordinates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StellarViewPositionAdapterTest
{
    @Test
    void keepsFractionalPositionAcrossSectorBoundary()
    {
        Coordinates before = StellarViewPositionAdapter.toSpaceCoords(UniversePosition.of(49_999.75, 0, 0));
        Coordinates after = StellarViewPositionAdapter.toSpaceCoords(UniversePosition.of(50_000.25, 0, 0));

        assertEquals(49_999L, before.xLy());
        assertEquals(50_000L, after.xLy());
        assertEquals(0.75 * StellarViewPositionAdapter.KM_PER_LY, before.xKm(), 1.0);
        assertEquals(0.25 * StellarViewPositionAdapter.KM_PER_LY, after.xKm(), 1.0);
        assertEquals(0.5 * StellarViewPositionAdapter.KM_PER_LY, (after.xLy()-before.xLy())*StellarViewPositionAdapter.KM_PER_LY+after.xKm()-before.xKm(), 1.0);
    }

    @Test
    void samplesVirtualShipPositionAndRetainsNegativeFractions()
    {
        StellarViewPositionAdapter adapter = new StellarViewPositionAdapter();
        adapter.setPosition(UniversePosition.of(-0.25, 2.5, -50_000.25));

        Coordinates sampled = adapter.sample();
        assertEquals(-1L, sampled.xLy());
        assertEquals(0.75 * StellarViewPositionAdapter.KM_PER_LY, sampled.xKm(), 1.0);
        assertEquals(2L, sampled.yLy());
        assertEquals(-50_001L, sampled.zLy());
        assertEquals(0.75 * StellarViewPositionAdapter.KM_PER_LY, sampled.zKm(), 1.0);
    }
}
