package com.starboundmc.network;

import com.starboundmc.menu.FuelControllerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server: press the "refuel" button in the fuel controller UI.
 * Adds ALL fuel items from the slots; surplus stays when the tank is full.
 */
public class AddFuelPacket
{
    public AddFuelPacket()
    {
    }

    public void encode(FriendlyByteBuf buf)
    {
    }

    public static AddFuelPacket decode(FriendlyByteBuf buf)
    {
        return new AddFuelPacket();
    }

    public static void handle(AddFuelPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof FuelControllerMenu menu)
            {
                menu.addAllFuelItems(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
