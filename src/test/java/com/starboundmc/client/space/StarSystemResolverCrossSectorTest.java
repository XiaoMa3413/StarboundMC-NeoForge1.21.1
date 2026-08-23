package com.starboundmc.client.space;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarSystemResolverCrossSectorTest
{
    @AfterEach
    void resetResolver()
    {
        StarSystemResolver.reset();
    }

    @Test
    void currentAndTargetSystemsRemainCandidatesOutsideNearbySectors()
    {
        UniversePosition deepSpace = UniversePosition.of(
                new SectorCoordinate(20L, 0L, -20L), 0.0, 102.0, 0.0);
        SpaceRenderContext route = context(deepSpace, StarSystems.SYS_MAIN, StarSystems.SYS_COLD, 0.0F);

        StarSystemResolver.ResolvedStarField resolved = StarSystemResolver.resolve(route);

        assertEquals(2, resolved.count());
        assertTrue(contains(resolved, StarSystems.SYS_MAIN));
        assertTrue(contains(resolved, StarSystems.SYS_COLD));
        assertTrue(navigationTarget(resolved, StarSystems.SYS_COLD));

        StarSystemResolver.ResolvedStarField docked = StarSystemResolver.resolve(
                context(deepSpace, StarSystems.SYS_MAIN, null, 0.0F));
        assertEquals(1, docked.count());
        assertTrue(contains(docked, StarSystems.SYS_MAIN));
        assertFalse(contains(docked, StarSystems.SYS_COLD));
    }

    private static SpaceRenderContext context(UniversePosition position, String currentHint,
                                               String targetHint, float animationTicks)
    {
        return new SpaceRenderContext(position.toLocalVec3(), position, Vec3.ZERO,
                0.0, 0.0, 0.0, FlightPhase.HYPERSPACE, true,
                0.5F, 560, Planet.LUSH, Planet.FROZEN,
                currentHint, targetHint, animationTicks);
    }

    private static boolean contains(StarSystemResolver.ResolvedStarField field, String systemId)
    {
        for (int i = 0; i < field.count(); i++)
            if (field.star(i).system().getSystemId().equals(systemId))
                return true;
        return false;
    }

    private static boolean navigationTarget(StarSystemResolver.ResolvedStarField field, String systemId)
    {
        for (int i = 0; i < field.count(); i++)
            if (field.star(i).system().getSystemId().equals(systemId))
                return field.star(i).navigationTarget();
        return false;
    }
}
