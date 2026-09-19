package com.starboundmc.network;

import com.starboundmc.epp.EppModuleTransfer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EppModuleClickPacket(int containerId, int stateId, int slot, int module) implements CustomPacketPayload {
    public static final Type<EppModuleClickPacket> TYPE = PayloadSupport.type("epp_module_click");
    public static final StreamCodec<FriendlyByteBuf, EppModuleClickPacket> STREAM_CODEC =
            CustomPacketPayload.codec((p, b) -> {
                b.writeVarInt(p.containerId); b.writeVarInt(p.stateId); b.writeVarInt(p.slot); b.writeVarInt(p.module);
            }, b -> new EppModuleClickPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt()));
    @Override public Type<EppModuleClickPacket> type() { return TYPE; }
    public static void handle(EppModuleClickPacket packet, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player
                && !EppModuleTransfer.exchange(player, packet.containerId, packet.stateId, packet.slot, packet.module))
            player.containerMenu.sendAllDataToRemote();
    }
}
