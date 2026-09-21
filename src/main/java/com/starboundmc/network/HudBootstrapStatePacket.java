// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import com.starboundmc.story.CoreState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.Objects;

/** Connection-wide core truth and personal HUD presentation receipts, independent of menus. */
public record HudBootstrapStatePacket(CoreState core, boolean wakePresented,
                                      boolean terminalContacted, boolean coreLinkPresented)
        implements CustomPacketPayload {
    public static final Type<HudBootstrapStatePacket> TYPE =
            PayloadSupport.type("hud_bootstrap_state");
    public static final StreamCodec<FriendlyByteBuf, HudBootstrapStatePacket> STREAM_CODEC =
            CustomPacketPayload.codec(HudBootstrapStatePacket::write, HudBootstrapStatePacket::new);

    public HudBootstrapStatePacket {
        Objects.requireNonNull(core, "core");
    }

    private HudBootstrapStatePacket(FriendlyByteBuf buffer) {
        this(CoreState.fromNetworkId(buffer.readVarInt()),
                buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean());
    }

    private void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(core.networkId());
        buffer.writeBoolean(wakePresented);
        buffer.writeBoolean(terminalContacted);
        buffer.writeBoolean(coreLinkPresented);
    }

    @Override
    public Type<HudBootstrapStatePacket> type() {
        return TYPE;
    }
}
