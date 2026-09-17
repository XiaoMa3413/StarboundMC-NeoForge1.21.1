package com.starboundmc.client.space;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.warp.UniverseNavigation;
import com.starboundmc.world.universe.StarSystemDefinition;
import com.starboundmc.world.universe.BuiltInUniverse;
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
        StarSystemDefinition main = com.starboundmc.client.StarmapUniverse.system(BuiltInUniverse.MAIN_SYSTEM_ID);
        GalaxyEnvironmentBlend warm = resolve(UniverseNavigation.universeDock("sys1:lush"),
                BuiltInUniverse.MAIN_SYSTEM_ID, null, 0.0F);

        assertSame(main, warm.dominantSystem());
        assertEquals(1.0F, warm.influence(main), 1.0E-6F);
        assertEquals(warm.influence(main), warm.soundscapeWeight(main), 1.0E-6F);
        assertEquals(0.0F, warm.deepSpaceWeight(), 1.0E-6F);
        assertEquals(0.0F, warm.radiationLevel(), 1.0E-6F);
        assertEquals(main.stellarVisual().getCoronaColor(), warm.skyTintColor());
        assertEquals(0.015F, warm.skyTintAmount(), 1.0E-6F);

        StarSystemDefinition cold = com.starboundmc.client.StarmapUniverse.system(BuiltInUniverse.COLD_SYSTEM_ID);
        GalaxyEnvironmentBlend irradiated = resolve(UniverseNavigation.universeDock("sys2:frozen"),
                BuiltInUniverse.COLD_SYSTEM_ID, null, 20.0F);
        assertSame(cold, irradiated.dominantSystem());
        assertEquals(1.0F, irradiated.influence(cold), 1.0E-6F);
        assertEquals(0.90F, irradiated.radiationLevel(), 1.0E-6F);
        assertEquals(cold.stellarVisual().getCoronaColor(), irradiated.skyTintColor());
        assertEquals(0.0825F, irradiated.skyTintAmount(), 1.0E-6F);
    }

    @Test
    void gapBetweenSystemsBecomesNeutralDeepSpace()
    {
        UniversePosition gap = UniversePosition.of(4_850.0, 102.0, 3_280.0);
        GalaxyEnvironmentBlend environment = resolve(gap,
                BuiltInUniverse.MAIN_SYSTEM_ID, BuiltInUniverse.COLD_SYSTEM_ID, 40.0F);

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
        StarSystemDefinition cold = com.starboundmc.client.StarmapUniverse.system(BuiltInUniverse.COLD_SYSTEM_ID);
        UniversePosition halfwayThroughBlend = cold.navigationCenter().add(
                new UniverseDelta(cold.influenceRadius() * 0.995, 0.0, 0.0));

        GalaxyEnvironmentBlend environment = resolve(halfwayThroughBlend,
                BuiltInUniverse.COLD_SYSTEM_ID, null, 30.0F);

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
                0.0F, 1, "sys1:lush", null, currentHint, targetHint, animationTicks);
        return StarSystemResolver.resolve(context).environment();
    }
}
