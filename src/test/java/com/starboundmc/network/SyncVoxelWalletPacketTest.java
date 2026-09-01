package com.starboundmc.network;

import com.starboundmc.client.ClientVoxelWalletState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncVoxelWalletPacketTest {
    @BeforeEach
    void resetClientWallet() {
        ClientVoxelWalletState.reset();
    }

    @Test
    void encodeDecodePreservesBalance() {
        SyncVoxelWalletPacket original = new SyncVoxelWalletPacket(123_456);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            SyncVoxelWalletPacket.STREAM_CODEC.encode(buffer, original);
            SyncVoxelWalletPacket decoded = SyncVoxelWalletPacket.STREAM_CODEC.decode(buffer);
            assertEquals(original.balance(), decoded.balance());
            assertEquals(original.type(), decoded.type());
        } finally {
            buffer.release();
        }
    }

    @Test
    void rejectsNegativeBalance() {
        assertThrows(IllegalArgumentException.class, () -> new SyncVoxelWalletPacket(-1));
    }

    @Test
    void clientMirrorClampsNegativeValues() {
        ClientVoxelWalletState.set(500);
        assertEquals(500, ClientVoxelWalletState.balance());
        ClientVoxelWalletState.set(-50);
        assertEquals(0, ClientVoxelWalletState.balance());
    }
}
