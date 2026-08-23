package com.starboundmc.network;

import com.starboundmc.warp.ShipWarpManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: request a warp to the given star-map entry.
 * The server resolves the entry id against the static star map, so a
 * client cannot warp to locked/unreachable bodies.
 */
public class StartWarpPacket
{
    private final String entryId;

    public StartWarpPacket(String entryId)
    {
        this.entryId = entryId;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(entryId);
    }

    public static StartWarpPacket decode(FriendlyByteBuf buf)
    {
        return new StartWarpPacket(buf.readUtf());
    }

    public static void handle(StartWarpPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
            {
                ShipWarpManager.startWarp(player, msg.entryId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
