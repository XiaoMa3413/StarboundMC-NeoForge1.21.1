package com.starboundmc.client.space;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.ShipSpace;
import com.starboundmc.world.Planet;
import com.starboundmc.world.starmap.StarSystem;
import com.starboundmc.world.starmap.StarSystems;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class GalaxyEnvironmentBlendTest
{
    @BeforeEach
    @AfterEach
    void resetResolver()
    {
        StarSystemResolver.reset();
    }

    @Test
    void dockedSystemsProduceTheirOwnEnvironmentWithoutAnIdSwitch()
    {
        StarSystem main = StarSystems.byId(StarSystems.SYS_MAIN);
        GalaxyEnvironmentBlend warm = resolve(ShipSpace.universeDock(Planet.LUSH),
                StarSystems.SYS_MAIN, null, 0.0F);

        assertSame(main, warm.dominantSystem());
        assertEquals(1.0F, warm.influence(main), 1.0E-6F);
        assertEquals(warm.influence(main), warm.soundscapeWeight(main), 1.0E-6F);
        assertEquals(0.0F, warm.deepSpaceWeight(), 1.0E-6F);
        assertEquals(0.0F, warm.radiationLevel(), 1.0E-6F);
        assertEquals(main.getStellarVisual().getCoronaColor(), warm.skyTintColor());
        assertEquals(0.015F, warm.skyTintAmount(), 1.0E-6F);

        StarSystem cold = StarSystems.byId(StarSystems.SYS_COLD);
        GalaxyEnvironmentBlend irradiated = resolve(ShipSpace.universeDock(Planet.FROZEN),
                StarSystems.SYS_COLD, null, 20.0F);
        assertSame(cold, irradiated.dominantSystem());
        assertEquals(1.0F, irradiated.influence(cold), 1.0E-6F);
        assertEquals(0.90F, irradiated.radiationLevel(), 1.0E-6F);
        assertEquals(cold.getStellarVisual().getCoronaColor(), irradiated.skyTintColor());
        assertEquals(0.0825F, irradiated.skyTintAmount(), 1.0E-6F);
    }

    @Test
    void gapBetweenSystemsBecomesNeutralDeepSpace()
    {
        UniversePosition gap = UniversePosition.of(4_850.0, 102.0, 3_280.0);
        GalaxyEnvironmentBlend environment = resolve(gap,
                StarSystems.SYS_MAIN, StarSystems.SYS_COLD, 40.0F);

        assertNull(environment.dominantSystem());
        assertEquals(1.0F, environment.deepSpaceWeight(), 1.0E-6F);
        assertEquals(0.0F, environment.environmentPresence(), 1.0E-6F);
        assertEquals(0.0F, environment.radiationLevel(), 1.0E-6F);
        assertEquals(0xFFFFFFFF, environment.skyTintColor());
        assertEquals(0.0F, environment.skyTintAmount(), 1.0E-6F);
    }

    @Test
    void environmentStrengthFadesContinuouslyAtTheInfluenceEdge()
    {
        StarSystem cold = StarSystems.byId(StarSystems.SYS_COLD);
        UniversePosition halfwayThroughBlend = cold.getUniverseNavigationCenter().add(
                new UniverseDelta(cold.getInfluenceRadius() * 0.995, 0.0, 0.0));

        GalaxyEnvironmentBlend environment = resolve(halfwayThroughBlend,
                StarSystems.SYS_COLD, null, 30.0F);

        assertEquals(0.5F, environment.influence(cold), 1.0E-5F);
        assertEquals(0.5F, environment.environmentPresence(), 1.0E-5F);
        assertEquals(0.5F, environment.deepSpaceWeight(), 1.0E-5F);
        assertEquals(0.45F, environment.radiationLevel(), 1.0E-5F);
        assertEquals(0.04125F, environment.skyTintAmount(), 1.0E-5F);
    }

    private static GalaxyEnvironmentBlend resolve(UniversePosition position, String currentHint,
                                                   String targetHint, float animationTicks)
    {
        SpaceRenderContext context = new SpaceRenderContext(position.toLocalVec3(), position, Vec3.ZERO,
                0.0, 0.0, 0.0, FlightPhase.DOCKED, false,
                0.0F, 1, Planet.LUSH, null, currentHint, targetHint, animationTicks);
        return StarSystemResolver.resolve(context).environment();
    }
}
