// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.warp.FlightPhase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EvaPacketsTest {
    @Test void controlsAndDimensionBoundModeRoundTrip() {
        var b = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var input = new EvaInputPacket(1 | 4 | 16);
            EvaInputPacket.STREAM_CODEC.encode(b, input);
            assertEquals(input, EvaInputPacket.STREAM_CODEC.decode(b));
            var state = new EvaStatePacket(ResourceLocation.parse("starboundmc:ship"), 2);
            EvaStatePacket.STREAM_CODEC.encode(b, state);
            assertEquals(state, EvaStatePacket.STREAM_CODEC.decode(b));
            assertFalse(b.isReadable());
        } finally { b.release(); }
    }
    @Test void crewHoldSurvivesWireAndFreezesClientExtrapolation() {
        ClientPlanetState.resetConnectionState();
        ClientPlanetState.setCurrent("sys1:lush");
        var b = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var original = new SyncFlightPacket(10, 100, FlightPhase.TURN, UniversePosition.fromLegacy(net.minecraft.world.phys.Vec3.ZERO),
                    new UniverseDelta(0, 0, 0), 0, 0, 0, 50, 800, "sys1:rockymoon", true);
            SyncFlightPacket.STREAM_CODEC.encode(b, original);
            var decoded = SyncFlightPacket.STREAM_CODEC.decode(b);
            assertTrue(decoded.crewHold());
            ClientPayloadHandler.handle(decoded, null);
            assertEquals(50 / 800f, ClientPlanetState.warpProgress(), 1e-8);
            assertEquals(50 / 800f, ClientPlanetState.captureVisualSnapshot().warpProgress(), 1e-8);
            var resume = new SyncFlightPacket(11, 110, FlightPhase.TURN, original.position(), original.velocity(),
                    0, 0, 0, 51, 800, "sys1:rockymoon");
            ClientPayloadHandler.handle(resume, null);
            assertTrue(ClientPlanetState.warpProgress() >= 51 / 800f);
            ClientPayloadHandler.handle(decoded, null); // stale hold must not freeze newer flight
            assertTrue(ClientPlanetState.warpProgress() >= 51 / 800f);
        } finally { b.release(); ClientPlanetState.resetConnectionState(); }
    }
}
