package com.starboundmc.client.space;

import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import com.starboundmc.world.universe.BuiltInUniverse;
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
        SpaceRenderContext route = context(deepSpace, BuiltInUniverse.MAIN_SYSTEM_ID, BuiltInUniverse.COLD_SYSTEM_ID, 0.0F);

        StarSystemResolver.ResolvedStarField resolved = StarSystemResolver.resolve(route);

        assertEquals(2, resolved.count());
        assertTrue(contains(resolved, BuiltInUniverse.MAIN_SYSTEM_ID));
        assertTrue(contains(resolved, BuiltInUniverse.COLD_SYSTEM_ID));
        assertTrue(navigationTarget(resolved, BuiltInUniverse.COLD_SYSTEM_ID));

        StarSystemResolver.ResolvedStarField docked = StarSystemResolver.resolve(
                context(deepSpace, BuiltInUniverse.MAIN_SYSTEM_ID, null, 0.0F));
        assertEquals(1, docked.count());
        assertTrue(contains(docked, BuiltInUniverse.MAIN_SYSTEM_ID));
        assertFalse(contains(docked, BuiltInUniverse.COLD_SYSTEM_ID));
    }

    private static SpaceRenderContext context(UniversePosition position, String currentHint,
                                               String targetHint, float animationTicks)
    {
        return new SpaceRenderContext(position.toLocalVec3(), position, Vec3.ZERO,
                0.0, 0.0, 0.0, FlightPhase.HYPERSPACE, true,
                0.5F, 560, "sys1:lush", "sys2:frozen",
                currentHint, targetHint, animationTicks);
    }

    private static boolean contains(StarSystemResolver.ResolvedStarField field, String systemId)
    {
        for (int i = 0; i < field.count(); i++)
            if (field.star(i).system().systemId().equals(systemId))
                return true;
        return false;
    }

    private static boolean navigationTarget(StarSystemResolver.ResolvedStarField field, String systemId)
    {
        for (int i = 0; i < field.count(); i++)
            if (field.star(i).system().systemId().equals(systemId))
                return field.star(i).navigationTarget();
        return false;
    }
}
