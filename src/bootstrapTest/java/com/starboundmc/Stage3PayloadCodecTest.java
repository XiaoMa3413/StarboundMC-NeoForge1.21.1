package com.starboundmc;

import com.starboundmc.network.AddFuelPacket;
import com.starboundmc.network.NovaBroadcastPacket;
import com.starboundmc.network.ShipAiActionPacket;
import com.starboundmc.network.ShipEnvironmentSnapshotPacket;
import com.starboundmc.network.ShipStorySnapshotPacket;
import com.starboundmc.network.StartWarpPacket;
import com.starboundmc.network.SyncFlightPacket;
import com.starboundmc.network.SyncFuelPacket;
import com.starboundmc.network.SyncPlanetPacket;
import com.starboundmc.network.SyncStarStatePacket;
import com.starboundmc.network.TeleportToShipPacket;
import com.starboundmc.network.TeleporterListPacket;
import com.starboundmc.network.TeleporterRenamePacket;
import com.starboundmc.network.TeleporterUsePacket;
import com.starboundmc.network.UpgradeMatterManipulatorPacket;
import com.starboundmc.network.WarpStartPacket;
import com.starboundmc.space.SectorCoordinate;
import com.starboundmc.space.UniverseDelta;
import com.starboundmc.space.UniversePosition;
import com.starboundmc.story.CoreState;
import com.starboundmc.story.EngineState;
import com.starboundmc.story.MineralScanState;
import com.starboundmc.story.SituationTopic;
import com.starboundmc.story.SurfaceMissionState;
import com.starboundmc.warp.FlightPhase;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class Stage3PayloadCodecTest {
    @Test
    void payloadTypesAreUniqueAndNamespaced() {
        List<CustomPacketPayload.Type<?>> types = List.of(
                UpgradeMatterManipulatorPacket.TYPE, StartWarpPacket.TYPE,
                SyncStarStatePacket.TYPE, SyncPlanetPacket.TYPE, WarpStartPacket.TYPE,
                SyncFuelPacket.TYPE, TeleporterListPacket.TYPE, TeleporterUsePacket.TYPE,
                TeleporterRenamePacket.TYPE, TeleportToShipPacket.TYPE,
                AddFuelPacket.TYPE, SyncFlightPacket.TYPE,
                ShipAiActionPacket.TYPE, ShipStorySnapshotPacket.TYPE,
                ShipEnvironmentSnapshotPacket.TYPE, NovaBroadcastPacket.TYPE);
        assertEquals(16, Set.copyOf(types).size());
        assertTrue(types.stream().allMatch(type -> type.id().getNamespace().equals("starboundmc")));
    }

    @Test
    void roundTripsUpgradeRequest() {
        assertRoundTrip(new UpgradeMatterManipulatorPacket(3),
                UpgradeMatterManipulatorPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsWarpRequest() {
        assertRoundTrip(new StartWarpPacket("system:molten"), StartWarpPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsStarState() {
        assertRoundTrip(new SyncStarStatePacket(
                        List.of("system:lush", "system:molten"), "system:molten"),
                SyncStarStatePacket.STREAM_CODEC);
    }

    @Test
    void roundTripsPlanetState() {
        assertRoundTrip(new SyncPlanetPacket("frozen"), SyncPlanetPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsWarpStart() {
        assertRoundTrip(new WarpStartPacket("barren", 420, "system:barren"),
                WarpStartPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsFuelState() {
        assertRoundTrip(new SyncFuelPacket(640, 1000), SyncFuelPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsTeleporterList() {
        assertRoundTrip(new TeleporterListPacket(List.of(
                        new TeleporterListPacket.Entry(0, "ship", ""),
                        new TeleporterListPacket.Entry(2, "n|overworld|1|64|2", "Base")),
                        "Forward Pad"),
                TeleporterListPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsTeleporterUseRequest() {
        assertRoundTrip(new TeleporterUsePacket("n|overworld|1|64|2"),
                TeleporterUsePacket.STREAM_CODEC);
    }

    @Test
    void roundTripsTeleporterRenameRequest() {
        assertRoundTrip(new TeleporterRenamePacket("Forward Pad"),
                TeleporterRenamePacket.STREAM_CODEC);
    }

    @Test
    void roundTripsShipReturnRequest() {
        assertRoundTrip(new TeleportToShipPacket(), TeleportToShipPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsFuelRequest() {
        assertRoundTrip(new AddFuelPacket(), AddFuelPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsFlightSnapshot() {
        assertRoundTrip(new SyncFlightPacket(9L, 1234L, FlightPhase.ACCELERATE,
                        UniversePosition.of(new SectorCoordinate(4L, -2L, 8L),
                                120.5, -5.25, 40_000.75),
                        new UniverseDelta(1.25, -0.5, 8.75),
                        45.0, -2.0, 3.5, 20, 400, "system:frozen"),
                SyncFlightPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsShipAiAction() {
        assertRoundTrip(ShipAiActionPacket.markSituationRead(17, 1L,
                        SituationTopic.CURRENT_LOCATION),
                ShipAiActionPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsShipStorySnapshot() {
        assertRoundTrip(new ShipStorySnapshotPacket(
                        17, 42L, 1, 9L, CoreState.ONLINE, SurfaceMissionState.ACTIVE,
                        EngineState.DAMAGED, EngineState.DAMAGED, MineralScanState.LOCKED, 0,
                        1, 12L, true, SituationTopic.REQUIRED_MASK, 1, 0),
                ShipStorySnapshotPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsShipEnvironmentSnapshot() {
        assertRoundTrip(new ShipEnvironmentSnapshotPacket(
                        17, 1, 9L, CoreState.REBOOTING,
                        EngineState.DAMAGED, EngineState.DAMAGED, 23),
                ShipEnvironmentSnapshotPacket.STREAM_CODEC);
    }

    @Test
    void roundTripsNovaBroadcast() {
        assertRoundTrip(new NovaBroadcastPacket(
                        "message.starboundmc.nova.prologue.mineral_scan_started"),
                NovaBroadcastPacket.STREAM_CODEC);
    }

    private static <T extends CustomPacketPayload> void assertRoundTrip(
            T original, StreamCodec<FriendlyByteBuf, T> codec) {
        byte[] encoded = encode(codec, original);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(encoded));
        T decoded;
        try {
            decoded = codec.decode(buffer);
            assertEquals(0, buffer.readableBytes(), "codec must consume the complete payload");
        } finally {
            buffer.release();
        }
        assertEquals(original.type(), decoded.type());
        assertArrayEquals(encoded, encode(codec, decoded));
    }

    private static <T> byte[] encode(StreamCodec<FriendlyByteBuf, T> codec, T value) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            codec.encode(buffer, value);
            byte[] result = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), result);
            return result;
        } finally {
            buffer.release();
        }
    }
}
