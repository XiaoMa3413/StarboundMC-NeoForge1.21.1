package com.starboundmc.story;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import com.starboundmc.network.HudBootstrapStatePacket;
import com.starboundmc.network.HudCoreLinkPresentedPacket;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudStateServiceTest {
    @Test
    void receiptCannotAdvanceAnOfflineOrRebootingCoreOrAnyGameplayProgress() {
        var personal = PlayerStoryState.DEFAULT;
        assertSame(personal, HudStateService.acknowledgeCoreLink(CoreState.OFFLINE, personal));
        assertSame(personal, HudStateService.acknowledgeCoreLink(CoreState.REBOOTING, personal));
        var linked = HudStateService.acknowledgeCoreLink(CoreState.ONLINE, personal);
        assertEquals(PlayerStoryFlag.HUD_CORE_LINK_PRESENTED.mask(), linked.flagsMask());
        assertFalse(linked.identityConfirmed());
        assertEquals(0, linked.tutorialMask());
        assertEquals(0, linked.readSituationMask());
        assertSame(linked, HudStateService.acknowledgeCoreLink(CoreState.ONLINE, linked));
    }

    @Test
    void receiptPersistsAndDoesNotAffectAnotherPlayer() {
        var alice = HudStateService.acknowledgeCoreLink(CoreState.ONLINE, PlayerStoryState.DEFAULT);
        var reloaded = PlayerStoryState.CODEC.parse(NbtOps.INSTANCE,
                PlayerStoryState.CODEC.encodeStart(NbtOps.INSTANCE, alice).getOrThrow()).getOrThrow();
        assertTrue(HudStateService.snapshotFor(CoreState.ONLINE, reloaded).coreLinkPresented());
        assertFalse(HudStateService.snapshotFor(CoreState.ONLINE, PlayerStoryState.DEFAULT).coreLinkPresented());
    }

    @Test
    void coreSnapshotsRoundTripEveryPhaseAndReceiptWithoutAMenuId() {
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            for (var core : CoreState.values()) {
                for (boolean seen : new boolean[]{false, true}) {
                    var packet = new HudBootstrapStatePacket(core, true, false, seen);
                    HudBootstrapStatePacket.STREAM_CODEC.encode(buffer, packet);
                    assertEquals(packet, HudBootstrapStatePacket.STREAM_CODEC.decode(buffer));
                    assertEquals(0, buffer.readableBytes());
                }
            }
            var receipt = new HudCoreLinkPresentedPacket();
            HudCoreLinkPresentedPacket.STREAM_CODEC.encode(buffer, receipt);
            assertEquals(receipt, HudCoreLinkPresentedPacket.STREAM_CODEC.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test
    void presentationReceiptDoesNotRewriteAFutureSchema() {
        var future = new PlayerStoryState(PlayerStoryState.CURRENT_SCHEMA_VERSION + 1,
                5L, false, 0, 0, 0, 0);
        assertSame(future, HudStateService.acknowledgeCoreLink(CoreState.ONLINE, future));
    }
}
