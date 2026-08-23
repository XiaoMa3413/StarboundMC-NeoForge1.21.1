package com.starboundmc.network;

import com.starboundmc.client.ClientPlanetState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Server -> Client: the ship's star-map progress — which entries have been
 * visited and the exact entry the ship is currently docked at. Sent when a
 * player enters the ship and whenever a warp completes.
 */
public class SyncStarStatePacket
{
    private final List<String> visited;
    private final String currentEntryId;

    public SyncStarStatePacket(List<String> visited, String currentEntryId)
    {
        this.visited = visited;
        this.currentEntryId = currentEntryId;
    }

    public void encode(FriendlyByteBuf buf)
    {
        buf.writeInt(visited.size());
        for (String entryId : visited)
        {
            buf.writeUtf(entryId);
        }
        buf.writeUtf(currentEntryId == null ? "" : currentEntryId);
    }

    public static SyncStarStatePacket decode(FriendlyByteBuf buf)
    {
        int count = buf.readInt();
        List<String> visited = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            visited.add(buf.readUtf());
        }
        String current = buf.readUtf();
        return new SyncStarStatePacket(visited, current.isEmpty() ? null : current);
    }

    public static void handle(SyncStarStatePacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                    ClientPlanetState.setStarState(msg.visited, msg.currentEntryId));
        });
        ctx.get().setPacketHandled(true);
    }
}
