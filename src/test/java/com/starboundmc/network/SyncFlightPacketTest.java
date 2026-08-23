package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SyncFlightPacketTest
{
    @BeforeEach
    void resetClientConnectionState()
    {
        ClientPlanetState.resetConnectionState();
    }

    @Test
    void encodeDecodePreservesSectorLocalPositionAndVelocity()
    {
        UniversePosition expectedPosition = UniversePosition.of(
                new SectorCoordinate(-12L, 34L, 5L), 49_999.75, -123.5, 0.25);
        UniverseDelta expectedVelocity = new UniverseDelta(-87.5, 0.125, 456.75);
        SyncFlightPacket original = new SyncFlightPacket(9L, 1234L, FlightPhase.ACCELERATE,
                expectedPosition, expectedVelocity, 20.0, -4.0, 1.0, 8, 200, "test:target");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try
        {
            original.encode(buffer);
            SyncFlightPacket decoded = SyncFlightPacket.decode(buffer);

            assertEquals(expectedPosition, decoded.position());
            assertEquals(expectedVelocity, decoded.velocity());
        }
        finally
        {
            buffer.release();
        }
    }

    @Test
    void sectorZeroSnapshotKeepsExistingClientVec3Position()
    {
        Vec3 expected = new Vec3(1234.5, 102.0, -678.25);
        UniversePosition position = UniversePosition.fromLegacy(expected);

        ClientPlanetState.applyFlightSnapshot(Long.MAX_VALUE, 0L, FlightPhase.DOCKED,
                position, new UniverseDelta(1.0, 2.0, 3.0),
                0.0F, 0.0F, 0.0F, 0, 1, null);

        assertEquals(expected, ClientPlanetState.getShipPosition());
        assertEquals(position, ClientPlanetState.getShipUniversePosition());
    }

    @Test
    void newConnectionAcceptsRevisionZeroAfterPreviousServerHadHigherRevision()
    {
        UniversePosition previous = UniversePosition.of(100.0, 102.0, 200.0);
        UniversePosition stale = UniversePosition.of(300.0, 102.0, 400.0);
        UniversePosition restored = UniversePosition.of(500.0, 102.0, 600.0);

        ClientPlanetState.applyFlightSnapshot(5L, 100L, FlightPhase.DOCKED, previous, new UniverseDelta(0.0, 0.0, 0.0),
                0.0F, 0.0F, 0.0F, 0, 1, null);
        ClientPlanetState.applyFlightSnapshot(4L, 101L, FlightPhase.DOCKED, stale, new UniverseDelta(0.0, 0.0, 0.0),
                0.0F, 0.0F, 0.0F, 0, 1, null);
        assertEquals(previous, ClientPlanetState.getShipUniversePosition());

        ClientPlanetState.resetConnectionState();
        ClientPlanetState.applyFlightSnapshot(0L, 102L, FlightPhase.DOCKED, restored, new UniverseDelta(0.0, 0.0, 0.0),
                0.0F, 0.0F, 0.0F, 0, 1, null);
        assertEquals(restored, ClientPlanetState.getShipUniversePosition());
    }
}
