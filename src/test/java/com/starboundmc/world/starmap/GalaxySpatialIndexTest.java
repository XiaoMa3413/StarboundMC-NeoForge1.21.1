package com.starboundmc.world.starmap;

import com.starboundmc.space.SectorCoordinate;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GalaxySpatialIndexTest
{
    @Test
    void nearbyQueryOnlyReturnsRequestedSectorShells()
    {
        List<StarSystem> systems = new ArrayList<>();
        for (int sectorX = -10; sectorX <= 10; sectorX++)
            systems.add(system("sys" + sectorX, sectorX * 100_000.0));

        GalaxySpatialIndex index = GalaxySpatialIndex.build(systems);
        StarSystem[] result = new StarSystem[64];
        int count = index.queryNearby(SectorCoordinate.ZERO, 1, result);

        assertEquals(21, index.systemCount());
        assertEquals(21, index.occupiedSectorCount());
        assertEquals(3, count);
        assertEquals("sys0", result[0].getSystemId());
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < count; i++)
            ids.add(result[i].getSystemId());
        assertEquals(Set.of("sys-1", "sys0", "sys1"), ids);
    }

    @Test
    void outputCapacityStrictlyBoundsLargeBuckets()
    {
        List<StarSystem> systems = new ArrayList<>();
        for (int i = 0; i < 1_000; i++)
            systems.add(system("dense" + i, i * 0.01));

        GalaxySpatialIndex index = GalaxySpatialIndex.build(systems);
        StarSystem[] result = new StarSystem[64];
        int count = index.queryNearby(SectorCoordinate.ZERO, 2, result);

        assertEquals(1_000, index.systemCount());
        assertEquals(1, index.occupiedSectorCount());
        assertEquals(result.length, count);
        for (StarSystem system : result)
            assertTrue(system.getSystemId().startsWith("dense"));
    }

    @Test
    void explicitLimitReservesOutputSlotsForPrioritySystems()
    {
        List<StarSystem> systems = new ArrayList<>();
        for (int i = 0; i < 20; i++)
            systems.add(system("near" + i, i * 0.01));

        GalaxySpatialIndex index = GalaxySpatialIndex.build(systems);
        StarSystem[] result = new StarSystem[8];
        int count = index.queryNearby(SectorCoordinate.ZERO, 0, result, 6);

        assertEquals(6, count);
        assertNull(result[6]);
        assertNull(result[7]);
    }

    private static StarSystem system(String id, double x)
    {
        Vec3 position = new Vec3(x, 0.0, 0.0);
        StellarVisualProfile visual = new StellarVisualProfile(position,
                0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF,
                new StellarDistanceResponse(1.0, 1.0F, 0.5F, 2.0F,
                        1.0F, 0.5F, 0.2F, 0.5F),
                1.0F, 0.0F, 0.0F, 0.0F, 1, 0);
        return new StarSystem(id, id, id, id, visual, position, 1_000.0, List.of());
    }
}
