package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import com.starboundmc.client.WarpSounds;
import com.starboundmc.world.Planet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server -> Client: a warp has begun. Carries the target planet, the total
 * duration and the star-map entry id of the destination (used by the star map
 * UI to animate the ship flying to it).
 */
public class WarpStartPacket
{
    private final String planetId;
    private final int durationTicks;
    private final String entryId;

    public WarpStartPacket(Planet planet, int durationTicks, String entryId)
    {
        this.planetId = planet.getId();
        this.durationTicks = durationTicks;
        this.entryId = entryId == null ? "" : entryId;
    }

    public WarpStartPacket(String planetId, int durationTicks, String entryId)
    {
        this.planetId = planetId;
        this.durationTicks = durationTicks;
        this.entryId = entryId == null ? "" : entryId;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeUtf(planetId);
        buf.writeVarInt(durationTicks);
        buf.writeUtf(entryId);
    }

    public static WarpStartPacket decode(FriendlyByteBuf buf)
    {
        return new WarpStartPacket(buf.readUtf(), buf.readVarInt(), buf.readUtf());
    }

    public static void handle(WarpStartPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ClientPlanetState.startWarp(Planet.fromId(msg.planetId), msg.durationTicks,
                    msg.entryId.isEmpty() ? null : msg.entryId);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> WarpSounds::onWarpStarted);
        });
        ctx.get().setPacketHandled(true);
    }
}
