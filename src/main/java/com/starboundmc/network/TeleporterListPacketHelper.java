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
        List<String[]> entries = new ArrayList<>();
        entries.add(new String[] { "0", "ship", "" });
        entries.add(new String[] { "1", "planet", "" });
        for (TeleporterManager.TeleporterEntry e : TeleporterManager.validEntries(server))
        {
            entries.add(new String[] { "2", "n|" + TeleporterManager.key(e.dimension(), e.pos()), e.name() });
        }
        String currentName = TeleporterManager.getName(server, dimension, pos);
        return new TeleporterListPacket(entries, currentName == null ? "" : currentName);
    }
}
