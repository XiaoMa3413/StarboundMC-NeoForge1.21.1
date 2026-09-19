// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.network;

import com.starboundmc.epp.EppMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenEppPacket() implements CustomPacketPayload {
    public static final Type<OpenEppPacket> TYPE = PayloadSupport.type("open_epp");
    public static final StreamCodec<FriendlyByteBuf, OpenEppPacket> STREAM_CODEC = StreamCodec.unit(new OpenEppPacket());
    @Override public Type<OpenEppPacket> type() { return TYPE; }
    public static void handle(OpenEppPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()
                && player.containerMenu == player.inventoryMenu)
            player.openMenu(new SimpleMenuProvider((id, inventory, owner) -> new EppMenu(id, inventory),
                    Component.translatable("container.starboundmc.epp")));
    }
}
