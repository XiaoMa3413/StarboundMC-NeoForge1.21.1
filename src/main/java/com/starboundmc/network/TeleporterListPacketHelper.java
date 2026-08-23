package com.starboundmc.network;

import com.starboundmc.world.TeleporterManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Builds the destination list packet for the teleporter UI. */
public final class TeleporterListPacketHelper
{
    private TeleporterListPacketHelper()
    {
    }

    public static TeleporterListPacket build(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos)
    {
        List<TeleporterListPacket.Entry> entries = new ArrayList<>();
        entries.add(new TeleporterListPacket.Entry(0, "ship", ""));
        entries.add(new TeleporterListPacket.Entry(1, "planet", ""));
        for (TeleporterManager.TeleporterEntry e : TeleporterManager.validEntries(server))
        {
            String destinationKey = "n|" + TeleporterManager.key(e.dimension(), e.pos());
            if (destinationKey.length() <= PayloadSupport.MAX_ID_LENGTH)
                entries.add(new TeleporterListPacket.Entry(2, destinationKey, e.name()));
            if (entries.size() >= PayloadSupport.MAX_LIST_ENTRIES)
                break;
        }
        String currentName = TeleporterManager.getName(server, dimension, pos);
        return new TeleporterListPacket(entries, currentName == null ? "" : currentName);
    }
}
