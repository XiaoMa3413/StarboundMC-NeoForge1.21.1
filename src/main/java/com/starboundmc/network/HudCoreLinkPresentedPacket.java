// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import com.starboundmc.story.HudStateService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A receipt for this sender's cosmetic link sequence, with no client-supplied core state. */
public record HudCoreLinkPresentedPacket() implements CustomPacketPayload {
    public static final Type<HudCoreLinkPresentedPacket> TYPE = PayloadSupport.type("hud_core_link_presented");
    public static final StreamCodec<FriendlyByteBuf, HudCoreLinkPresentedPacket> STREAM_CODEC =
            StreamCodec.unit(new HudCoreLinkPresentedPacket());

    @Override public Type<HudCoreLinkPresentedPacket> type() { return TYPE; }

    public static void handle(HudCoreLinkPresentedPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player)
            HudStateService.acknowledgeCoreLink(player);
    }
}
