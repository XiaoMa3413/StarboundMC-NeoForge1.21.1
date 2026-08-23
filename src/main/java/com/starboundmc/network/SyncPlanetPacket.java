package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.WarpSounds;
import com.starboundmc.world.Planet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server -> Client: tells the client which planet is currently being orbited. */
public class SyncPlanetPacket
{
    private final String planetId;

    public SyncPlanetPacket(Planet planet)
    {
        this.planetId = planet.getId();
    }

    public SyncPlanetPacket(String planetId)
    {
        this.planetId = planetId;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(planetId);
    }

    public static SyncPlanetPacket decode(FriendlyByteBuf buf)
    {
        return new SyncPlanetPacket(buf.readUtf());
    }

    public static void handle(SyncPlanetPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ClientPlanetState.setCurrent(Planet.fromId(msg.planetId));
            if (ClientPlanetState.consumeArrivalCue())
            {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> WarpSounds::onWarpFinished);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
