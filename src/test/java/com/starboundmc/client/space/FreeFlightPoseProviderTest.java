package com.starboundmc.client.space;

import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import com.starboundmc.world.starmap.StarSystems;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeFlightPoseProviderTest
{
    @AfterEach
    void restoreAutomaticFlightProvider()
    {
        SpaceRenderState.resetPoseProvider();
    }

    @Test
    void freeFlightProviderSuppliesNativeUniversePoseWithNeutralRouteDefaults()
    {
        UniversePosition position = UniversePosition.of(250_125.0, -75_500.0, 610_250.0);
        UniverseDelta velocity = new UniverseDelta(18.5, -2.25, 42.0);
        MutableFreeFlightProvider provider = new MutableFreeFlightProvider(
                position, velocity, 35.0, -12.0, 4.5);
        SpaceRenderState.setPoseProvider(provider);

        SpaceRenderContext context = SpaceRenderState.capture(120.5F);

        assertSame(position, context.universePosition());
        assertEquals(position.toLocalVec3(), context.shipPosition());
        assertEquals(velocity.toVec3(), context.shipVelocity());
        assertEquals(35.0, context.yaw());
        assertEquals(-12.0, context.pitch());
        assertEquals(4.5, context.roll());
        assertEquals(FlightPhase.DOCKED, context.flightPhase());
        assertFalse(context.warping());
        assertEquals(0.0F, context.warpProgress());
        assertEquals(1, context.warpDurationTicks());
        assertNull(context.currentBody());
        assertNull(context.targetBody());
        assertNull(context.currentSystemHint());
        assertNull(context.targetSystemHint());
    }

    @Test
    void floatingOriginRecentersLocalPositionWithoutJumpingRelativeStars()
    {
        MutableFreeFlightProvider provider = new MutableFreeFlightProvider(
                UniversePosition.of(49_999.75, 102.0, 0.0), UniverseDelta.ZERO,
                0.0, 0.0, 0.0);
        SpaceRenderState.setPoseProvider(provider);

        SpaceRenderContext beforeContext = SpaceRenderState.capture(10.0F);
        StarSystemResolver.ResolvedStarField beforeField = StarSystemResolver.resolve(beforeContext);
        double beforeRelativeX = relativeX(beforeField, StarSystems.SYS_MAIN);

        provider.position = provider.position.add(new UniverseDelta(1.0, 0.0, 0.0));
        SpaceRenderContext afterContext = SpaceRenderState.capture(11.0F);
        StarSystemResolver.ResolvedStarField afterField = StarSystemResolver.resolve(afterContext);
        double afterRelativeX = relativeX(afterField, StarSystems.SYS_MAIN);

        assertTrue(beforeContext.shipPosition().x > 49_999.0);
        assertTrue(afterContext.shipPosition().x < -49_999.0);
        assertEquals(1L, afterContext.universePosition().sector().x());
        assertEquals(1.0, beforeContext.universePosition().deltaXTo(afterContext.universePosition()), 1.0E-9);
        assertEquals(-1.0, afterRelativeX - beforeRelativeX, 1.0E-9);
    }

    private static double relativeX(StarSystemResolver.ResolvedStarField field, String systemId)
    {
        for (int i = 0; i < field.count(); i++)
            if (systemId.equals(field.star(i).system().getSystemId()))
                return field.star(i).relativeX();
        throw new AssertionError("Missing star system " + systemId);
    }

    private static final class MutableFreeFlightProvider implements FreeFlightPoseProvider
    {
        private UniversePosition position;
        private final UniverseDelta velocity;
        private final double yaw;
        private final double pitch;
        private final double roll;

        private MutableFreeFlightProvider(UniversePosition position, UniverseDelta velocity,
                                          double yaw, double pitch, double roll)
        {
            this.position = position;
            this.velocity = velocity;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }

        @Override
        public UniversePosition universePosition()
        {
            return position;
        }

        @Override
        public UniverseDelta universeVelocity()
        {
            return velocity;
        }

        @Override
        public double yaw()
        {
            return yaw;
        }

        @Override
        public double pitch()
        {
            return pitch;
        }

        @Override
        public double roll()
        {
            return roll;
        }
    }
}
