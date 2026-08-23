package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: sync the ship's fuel level. */
public class SyncFuelPacket
{
    private final int fuel;
    private final int maxFuel;

    public SyncFuelPacket(int fuel, int maxFuel)
    {
        this.fuel = fuel;
        this.maxFuel = maxFuel;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeVarInt(fuel);
        buf.writeVarInt(maxFuel);
    }

    public static SyncFuelPacket decode(FriendlyByteBuf buf)
    {
        return new SyncFuelPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SyncFuelPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() -> ClientPlanetState.setFuel(msg.fuel, msg.maxFuel));
        ctx.get().setPacketHandled(true);
    }
}
