package com.starboundmc.client.compat.stellarview;

import com.starboundmc.space.UniversePosition;
import net.povstalec.stellarview.common.util.SpaceCoords;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StellarViewPositionAdapterTest
{
    @Test
    void keepsFractionalPositionAcrossSectorBoundary()
    {
        SpaceCoords before = StellarViewPositionAdapter.toSpaceCoords(UniversePosition.of(49_999.75, 0, 0));
        SpaceCoords after = StellarViewPositionAdapter.toSpaceCoords(UniversePosition.of(50_000.25, 0, 0));

        assertEquals(49_999L, before.x().ly());
        assertEquals(50_000L, after.x().ly());
        assertEquals(0.75 * SpaceCoords.KM_PER_LY, before.x().km(), 1.0);
        assertEquals(0.25 * SpaceCoords.KM_PER_LY, after.x().km(), 1.0);
        assertEquals(0.5 * SpaceCoords.KM_PER_LY, after.x().sub(before.x()).toKm(), 1.0);
    }

    @Test
    void samplesVirtualShipPositionAndRetainsNegativeFractions()
    {
        StellarViewPositionAdapter adapter = new StellarViewPositionAdapter();
        adapter.setPosition(UniversePosition.of(-0.25, 2.5, -50_000.25));

        SpaceCoords sampled = adapter.sample(null, null, 0.0F);
        assertEquals(-1L, sampled.x().ly());
        assertEquals(0.75 * SpaceCoords.KM_PER_LY, sampled.x().km(), 1.0);
        assertEquals(2L, sampled.y().ly());
        assertEquals(-50_001L, sampled.z().ly());
        assertEquals(0.75 * SpaceCoords.KM_PER_LY, sampled.z().km(), 1.0);
    }
}
